package com.leekwars.generator.polyglot;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.graalvm.polyglot.io.FileSystem;

/**
 * FileSystem polyglot en MEMOIRE, en lecture seule, servant uniquement les fichiers d'IA du
 * joueur (et rien de l'hote). C'est ce qui permet le multi-fichiers : les systemes de modules
 * natifs (import/export JS, import Python) resolvent leurs dependances a travers lui, donc
 * sans aucun acces disque.
 *
 * Les fichiers sont montes sous un prefixe virtuel ({@link #MOUNT}). Un chemin virtuel
 * {@code /ai/strategie.js} est traduit en chemin LeekScript {@code strategie.js}. La liste des
 * chemins est connue d'avance (pour le listing de dossier, requis par l'import Python) ; le
 * contenu est lu paresseusement via {@code read}.
 *
 * En option, un {@code passthroughRoot} (sous-arbre hote, ex: le python-home de GraalPy) est
 * delegue au FS hote en LECTURE SEULE. C'est necessaire pour Python : GraalPy lit sa stdlib .py
 * depuis ce dossier hote, qu'un FS purement en memoire ne servirait pas (la stdlib casserait).
 * Tout chemin hors {@link #MOUNT} et hors {@code passthroughRoot} reste inaccessible.
 */
public class PolyglotFileSystem implements FileSystem {

	/** Point de montage virtuel des fichiers du joueur. */
	public static final String MOUNT = "/ai";

	/**
	 * Extensions probees pour un import JS/TS SANS extension ({@code import './lib'}) : habitude
	 * Node CJS / TypeScript tres repandue, qui sinon echoue en "Cannot find module" au combat alors
	 * que l'editeur n'a rien signale. A ne PAS activer pour Python : importlib fait ses propres
	 * probes de chemins sans extension (paquets = dossiers) et un probing .js les fausserait.
	 */
	public static final List<String> JS_PROBE_EXTENSIONS = List.of(".js", ".mjs");

	private final Set<String> files;   // chemins LeekScript des fichiers
	private final Set<String> dirs;    // dossiers (derives des chemins de fichiers), "" = racine
	private final Function<String, String> read; // chemin LeekScript -> contenu
	private final List<String> probeExtensions; // probing des imports sans extension (vide = off)
	private final String entryDir;     // dossier du fichier d'entree ("" = racine), repli des imports bare

	private final Path passthroughRoot;     // sous-arbre hote delegue en lecture seule (stdlib), ou null
	private final Path passthroughRootReal; // sa version resolue (symlinks suivis), pour le confinement
	private final FileSystem hostDelegate;  // FS hote, uniquement pour le passthrough

	public PolyglotFileSystem(Set<String> filePaths, Function<String, String> read) {
		this(filePaths, read, null);
	}

	public PolyglotFileSystem(Set<String> filePaths, Function<String, String> read, Path passthroughRoot) {
		this(filePaths, read, passthroughRoot, List.of(), null);
	}

	/**
	 * @param filePaths       tous les chemins de fichiers du joueur (ex: "main.js", "lib/util.js")
	 * @param read            lecture paresseuse d'un fichier (chemin LeekScript -> contenu, ou null)
	 * @param passthroughRoot sous-arbre hote delegue en lecture seule (stdlib GraalPy), ou null
	 * @param probeExtensions extensions essayees pour un chemin demande sans extension et absent
	 *                        (cf {@link #JS_PROBE_EXTENSIONS}) ; liste vide = probing desactive
	 * @param entryPath       chemin du fichier d'ENTREE de l'IA (son dossier sert de repli aux
	 *                        imports bare ancres racine par le loader), ou null
	 */
	public PolyglotFileSystem(Set<String> filePaths, Function<String, String> read, Path passthroughRoot, List<String> probeExtensions, String entryPath) {
		this.files = new HashSet<>(filePaths);
		this.read = read;
		this.probeExtensions = probeExtensions;
		int entrySlash = entryPath == null ? -1 : entryPath.lastIndexOf('/');
		this.entryDir = entrySlash < 0 ? "" : entryPath.substring(0, entrySlash);
		this.dirs = new HashSet<>();
		this.dirs.add(""); // racine
		for (String f : filePaths) {
			int slash = f.lastIndexOf('/');
			while (slash >= 0) {
				dirs.add(f.substring(0, slash));
				slash = f.lastIndexOf('/', slash - 1);
			}
		}
		this.passthroughRoot = passthroughRoot == null ? null : passthroughRoot.toAbsolutePath().normalize();
		this.hostDelegate = passthroughRoot == null ? null : FileSystem.newDefaultFileSystem();
		Path real = this.passthroughRoot;
		if (this.passthroughRoot != null) {
			try {
				real = hostDelegate.toRealPath(this.passthroughRoot);
			} catch (IOException e) {
				real = this.passthroughRoot; // la racine devrait exister ; sinon on garde le lexical
			}
		}
		this.passthroughRootReal = real;
	}

	/** Chemin absolu normalise : les chemins relatifs sont ancres au point de montage (pas le CWD). */
	private static Path abs(Path p) {
		return (p.isAbsolute() ? p : Path.of(MOUNT).resolve(p)).normalize();
	}

	/**
	 * Confinement du passthrough : on suit les symlinks (toRealPath) et on REVERIFIE que la cible
	 * reelle reste sous la racine reelle, sinon un symlink dans le python-home pourrait pointer hors
	 * du sous-arbre. Pour un chemin inexistant (probing d'import), pas de symlink a suivre -&gt; lexical.
	 */
	private void requireContained(Path p) throws IOException {
		Path real;
		try {
			real = hostDelegate.toRealPath(p);
		} catch (IOException e) {
			real = abs(p);
		}
		if (!real.startsWith(passthroughRootReal)) {
			throw new java.nio.file.AccessDeniedException(String.valueOf(p));
		}
	}

	/** Chemin virtuel absolu d'un fichier LeekScript (ex: "strategie.js" -> "/ai/strategie.js"). */
	public static String mountPath(String leekPath) {
		return MOUNT + "/" + leekPath;
	}

	/** Chemin LeekScript a partir d'un chemin virtuel, ou null s'il est hors du point de montage. */
	private String toLeekPath(Path p) {
		String s = abs(p).toString();
		if (s.equals(MOUNT)) return "";
		if (!s.startsWith(MOUNT + "/")) return null; // hors du dossier du joueur
		return probe(s.substring(MOUNT.length() + 1));
	}

	/**
	 * Resolution tolerante d'un chemin demande absent (JS/TS uniquement, cf constructeur) :
	 * <ol>
	 * <li>import sans extension : {@code './lib'} -> {@code lib.js} / {@code lib.mjs}, meme si un
	 *     DOSSIER {@code lib/} existe (resolution TypeScript : le fichier gagne sur le dossier —
	 *     un dossier n'est de toute facon jamais importable en ESM, pas de magie index.js) ;</li>
	 * <li>repli RACINE : {@code dossier/lib.js} absent -> {@code lib.js} racine s'il existe
	 *     (specificateur bare depuis un sous-dossier alors que la lib vit a la racine — la
	 *     resolution relative au module importeur a deja ete tentee par graaljs).</li>
	 * </ol>
	 * Un FICHIER existant n'est jamais reecrit ; un dossier existant sans fichier candidat non plus.
	 */
	private String probe(String leek) {
		if (probeExtensions.isEmpty() || files.contains(leek)) {
			return leek;
		}
		// Le fichier probe ({@code lib.js}) gagne sur un dossier {@code lib/} homonyme : sinon un
		// simple dossier 'test/' dans le compte rend 'import "./test"' (test.ts) irresoluble.
		String withExt = probeWithExtensions(leek);
		if (withExt != null) {
			return withExt;
		}
		if (dirs.contains(leek)) {
			return leek;
		}
		// Repli dossier de l'ENTREE : un import bare ('include.js' sans ./) est ancre a la racine
		// par le loader, mais le joueur vise generalement le voisin de son fichier (ia-ts/test.js
		// -> ia-ts/include.js). Le module importeur n'est pas connu ici ; le dossier de l'entree
		// est la meilleure approximation (projet mono-dossier = cas dominant).
		if (!entryDir.isEmpty() && !leek.startsWith(entryDir + "/")) {
			String cand = entryDir + "/" + leek;
			if (files.contains(cand)) {
				return cand;
			}
			String candExt = probeWithExtensions(cand);
			if (candExt != null) {
				return candExt;
			}
		}
		// Dernier recours : le meme nom a la racine (module d'un sous-dossier visant une lib racine).
		int slash = leek.lastIndexOf('/');
		if (slash >= 0) {
			String base = leek.substring(slash + 1);
			if (files.contains(base)) {
				return base;
			}
			String baseExt = probeWithExtensions(base);
			if (baseExt != null) {
				return baseExt;
			}
		}
		return leek;
	}

	/** Le chemin complete par une extension probee, ou null (extension deja explicite = pas de probing). */
	private String probeWithExtensions(String leek) {
		String base = leek.substring(leek.lastIndexOf('/') + 1);
		if (base.indexOf('.') >= 0) {
			return null;
		}
		for (String ext : probeExtensions) {
			if (files.contains(leek + ext)) {
				return leek + ext;
			}
		}
		return null;
	}

	/** Le chemin est-il dans le sous-arbre hote delegue (stdlib) ? (decision lexicale) */
	private boolean inPassthrough(Path p) {
		return passthroughRoot != null && abs(p).startsWith(passthroughRoot);
	}

	private static boolean isWrite(Set<? extends OpenOption> options) {
		return options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)
			|| options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW)
			|| options.contains(StandardOpenOption.DELETE_ON_CLOSE);
	}

	@Override
	public Path parsePath(URI uri) {
		return Path.of(uri.getPath());
	}

	@Override
	public Path parsePath(String path) {
		return Path.of(path);
	}

	@Override
	public Path toAbsolutePath(Path path) {
		if (inPassthrough(path)) {
			return hostDelegate.toAbsolutePath(path);
		}
		// Les chemins relatifs sont ancres au point de montage (et non au CWD du process).
		return abs(path);
	}

	@Override
	public Path toRealPath(Path path, LinkOption... options) throws IOException {
		if (inPassthrough(path)) {
			requireContained(path);
			return hostDelegate.toRealPath(path, options);
		}
		// Canonicalise un import sans extension vers le fichier probe : '/ai/lib' et '/ai/lib.js'
		// designent alors le MEME module (sinon il serait charge deux fois, avec deux etats).
		String leek = toLeekPath(path);
		if (leek != null && !leek.isEmpty()) {
			return Path.of(mountPath(leek));
		}
		return abs(path);
	}

	@Override
	public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... options) throws IOException {
		if (modes.contains(AccessMode.WRITE)) {
			throw new IOException("read-only");
		}
		if (inPassthrough(path)) {
			requireContained(path);
			hostDelegate.checkAccess(path, modes, options);
			return;
		}
		String leek = toLeekPath(path);
		if (leek == null || (!files.contains(leek) && !dirs.contains(leek))) {
			throw new NoSuchFileException(String.valueOf(path));
		}
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		if (isWrite(options)) {
			throw new IOException("read-only");
		}
		if (inPassthrough(path)) {
			requireContained(path);
			return hostDelegate.newByteChannel(path, options, attrs);
		}
		String leek = toLeekPath(path);
		String content = leek != null && files.contains(leek) ? read.apply(leek) : null;
		if (content == null) {
			throw new NoSuchFileException(String.valueOf(path));
		}
		return new ReadOnlyByteChannel(content.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
		if (inPassthrough(dir)) {
			requireContained(dir);
			return hostDelegate.newDirectoryStream(dir, filter);
		}
		String leekDir = toLeekPath(dir);
		if (leekDir == null || !dirs.contains(leekDir)) {
			throw new NoSuchFileException(String.valueOf(dir));
		}
		String prefix = leekDir.isEmpty() ? "" : leekDir + "/";
		List<Path> entries = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String f : files) {
			if (f.startsWith(prefix)) {
				String rest = f.substring(prefix.length());
				int slash = rest.indexOf('/');
				String name = slash < 0 ? rest : rest.substring(0, slash); // fichier direct ou sous-dossier
				if (seen.add(name)) {
					entries.add(Path.of(mountPath(prefix + name)));
				}
			}
		}
		return new DirectoryStream<Path>() {
			@Override public java.util.Iterator<Path> iterator() { return entries.iterator(); }
			@Override public void close() {}
		};
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		if (inPassthrough(path)) {
			requireContained(path);
			return hostDelegate.readAttributes(path, attributes, options);
		}
		String leek = toLeekPath(path);
		boolean file = leek != null && files.contains(leek);
		boolean dir = leek != null && dirs.contains(leek);
		if (!file && !dir) {
			throw new NoSuchFileException(String.valueOf(path));
		}
		FileTime epoch = FileTime.fromMillis(0);
		Map<String, Object> r = new java.util.HashMap<>();
		r.put("isRegularFile", file);
		r.put("isDirectory", dir);
		r.put("isSymbolicLink", false);
		r.put("isOther", false);
		long size = 0;
		if (file) {
			String content = read.apply(leek);
			size = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
		}
		r.put("size", size);
		r.put("lastModifiedTime", epoch);
		r.put("lastAccessTime", epoch);
		r.put("creationTime", epoch);
		r.put("ctime", epoch);
		r.put("fileKey", null);
		// Attributs unix demandes par le stat() de GraalPy.
		r.put("mode", dir ? 0040555 : 0100444);
		r.put("nlink", 1);
		r.put("uid", 0);
		r.put("gid", 0);
		r.put("dev", 0L);
		r.put("ino", (long) Math.abs(String.valueOf(path).hashCode()));
		r.put("rdev", 0L);
		r.put("permissions", Set.<PosixFilePermission>of());
		return r;
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		throw new IOException("read-only");
	}

	@Override
	public void delete(Path path) throws IOException {
		throw new IOException("read-only");
	}

	/** Canal en lecture seule sur un tableau d'octets. */
	private static final class ReadOnlyByteChannel implements SeekableByteChannel {
		private final byte[] bytes;
		private long position = 0;
		private boolean open = true;

		ReadOnlyByteChannel(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public int read(ByteBuffer dst) {
			if (position >= bytes.length) return -1;
			int n = Math.min(dst.remaining(), (int) (bytes.length - position));
			dst.put(bytes, (int) position, n);
			position += n;
			return n;
		}

		@Override public int write(ByteBuffer src) { throw new UnsupportedOperationException("read-only"); }
		@Override public long position() { return position; }
		@Override public SeekableByteChannel position(long newPosition) { position = newPosition; return this; }
		@Override public long size() { return bytes.length; }
		@Override public SeekableByteChannel truncate(long size) { throw new UnsupportedOperationException("read-only"); }
		@Override public boolean isOpen() { return open; }
		@Override public void close() { open = false; }
	}
}
