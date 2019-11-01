package com.leekwars.generator;

import leekscript.compiler.AIFile;
import leekscript.compiler.resolver.Resolver;
import leekscript.compiler.resolver.ResolverContext;

public class DbResolver implements Resolver<DbContext> {

	private static final String TAG = DbResolver.class.getSimpleName();

	private String command;

	public DbResolver(String command) {
		this.command = command;
	}

	@Override
	public AIFile<DbContext> resolve(String path, ResolverContext basecontext) {

		DbContext context = (DbContext) basecontext;
		if (context == null) {
			Log.w(TAG, "No context, missing farmer and folder!");
			return null;
		}

		String result = resolve_internal(context.getFarmer(), context.getFolder(), path);
		
		if (result.equals("0")) {
			Log.w(TAG, "AI " + path + " not found!");
			return null;
		} else {
			Log.i(TAG, "Resolved ai: " + result);
			String[] parts = result.split(" ", 2);
			int folderID = Integer.parseInt(parts[0]);
			int aiID = Integer.parseInt(parts[1]);
			long timestamp = Long.parseLong(parts[2]);

			String code = Util.readFile("../ai/" + aiID + ".leek");

			DbContext newContext = new DbContext(context.getFarmer(), folderID);
			return new AIFile<DbContext>(path, code, timestamp, newContext, aiID);
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

	@Override
	public ResolverContext createContext(int farmer, int owner) {
		return new DbContext(farmer, 0);
	}
}