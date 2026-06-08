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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.graalvm.polyglot.io.FileSystem;

/**
 * FileSystem polyglot en MEMOIRE, en lecture seule, servant uniquement les fichiers d'IA du
 * joueur (et rien de l'hote). C'est ce qui permet le multi-fichiers : les systemes de modules
 * natifs (import/export JS, import Python) resolvent leurs dependances a travers ce FS, donc
 * sans aucun acces disque.
 *
 * Les fichiers sont montes sous un prefixe virtuel ({@link #MOUNT}). Un chemin virtuel
 * {@code /ai/strategie.js} est traduit en chemin LeekScript {@code strategie.js}. La liste des
 * chemins est connue d'avance (pour le listing de dossier, requis par l'import Python) ; le
 * contenu est lu paresseusement via {@code read}.
 */
public class PolyglotFileSystem implements FileSystem {

	/** Point de montage virtuel des fichiers du joueur. */
	public static final String MOUNT = "/ai";

	private final Set<String> files;   // chemins LeekScript des fichiers
	private final Set<String> dirs;    // dossiers (derives des chemins de fichiers), "" = racine
	private final Function<String, String> read; // chemin LeekScript -> contenu

	/**
	 * @param filePaths tous les chemins de fichiers du joueur (ex: "main.js", "lib/util.js")
	 * @param read      lecture paresseuse d'un fichier (chemin LeekScript -> contenu, ou null)
	 */
	public PolyglotFileSystem(Set<String> filePaths, Function<String, String> read) {
		this.files = new HashSet<>(filePaths);
		this.read = read;
		this.dirs = new HashSet<>();
		this.dirs.add(""); // racine
		for (String f : filePaths) {
			int slash = f.lastIndexOf('/');
			while (slash >= 0) {
				dirs.add(f.substring(0, slash));
				slash = f.lastIndexOf('/', slash - 1);
			}
		}
	}

	/** Chemin virtuel absolu d'un fichier LeekScript (ex: "strategie.js" -> "/ai/strategie.js"). */
	public static String mountPath(String leekPath) {
		return MOUNT + "/" + leekPath;
	}

	/** Chemin LeekScript a partir d'un chemin virtuel, ou null s'il est hors du point de montage. */
	private String toLeekPath(Path p) {
		String s = p.toAbsolutePath().normalize().toString();
		if (s.equals(MOUNT)) return "";
		if (!s.startsWith(MOUNT + "/")) return null; // hors du dossier du joueur
		return s.substring(MOUNT.length() + 1);
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
		// Les chemins relatifs sont ancres au point de montage (et non au CWD du process).
		return path.isAbsolute() ? path : Path.of(MOUNT).resolve(path);
	}

	@Override
	public Path toRealPath(Path path, LinkOption... options) {
		return toAbsolutePath(path).normalize();
	}

	@Override
	public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... options) throws IOException {
		if (modes.contains(AccessMode.WRITE)) {
			throw new IOException("read-only");
		}
		String leek = toLeekPath(path);
		if (leek == null || (!files.contains(leek) && !dirs.contains(leek))) {
			throw new NoSuchFileException(String.valueOf(path));
		}
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		String leek = toLeekPath(path);
		String content = leek != null && files.contains(leek) ? read.apply(leek) : null;
		if (content == null) {
			throw new NoSuchFileException(String.valueOf(path));
		}
		return new ReadOnlyByteChannel(content.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
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
		String leek = toLeekPath(path);
		boolean file = leek != null && files.contains(leek);
		boolean dir = leek != null && dirs.contains(leek);
		if (!file && !dir) {
			throw new NoSuchFileException(String.valueOf(path));
		}
		FileTime epoch = FileTime.fromMillis(0);
		Map<String, Object> r = new HashMap<>();
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
