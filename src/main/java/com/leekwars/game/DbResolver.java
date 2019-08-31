package com.leekwars.game;

import leekscript.compiler.AIFile;
import leekscript.compiler.resolver.Resolver;
import leekscript.compiler.resolver.ResolverContext;

public class DbResolver implements Resolver<DbContext> {

	private static final String TAG = DbResolver.class.getSimpleName();

	private String command;
	private int farmer;

	public DbResolver(String command) {
		this.command = command;
	}

	@Override
	public AIFile<DbContext> resolve(String path, ResolverContext basecontext) {

		DbContext context = (DbContext) basecontext;
		if (context == null) {
			context = new DbContext(0);
		}

		String result = resolve_internal(farmer, context.getFolder(), path);
		
		if (result.equals("0")) {
			Log.w(TAG, "AI " + path + " not found!");
			return null;
		} else {
			Log.i(TAG, "Resolved ai: " + result);
			String[] parts = result.split(" ", 2);
			int folderID = Integer.parseInt(parts[0]);
			int aiID = Integer.parseInt(parts[1]);

			String code = Util.readFile("ai/" + aiID + ".leek");

			DbContext newContext = new DbContext(folderID);
			return new AIFile<DbContext>(path, code, newContext);
		}
	}

	private String resolve_internal(int owner, int folder, String path) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command, String.valueOf(owner), String.valueOf(folder), path);
			Log.i(TAG, "Start resolve process " + command + " " + owner + " " + folder + " " + path);
			// pb.inheritIO();
			Process p = pb.start();
			p.waitFor();
			Log.i(TAG, "Resolve process finished");
			return Util.inputStreamToString(p.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			return "0";
		}
	}

	public int getFarmer() {
		return farmer;
	}
	public void setFarmer(int farmer) {
		this.farmer = farmer;
	}
}