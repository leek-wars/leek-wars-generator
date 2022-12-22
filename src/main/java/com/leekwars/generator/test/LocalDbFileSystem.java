package com.leekwars.generator.test;

import leekscript.compiler.AIFile;
import leekscript.compiler.Folder;
import leekscript.compiler.resolver.FileSystem;

public class LocalDbFileSystem extends FileSystem {

	@Override
	public Folder getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder getRoot(int owner, int farmer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder getRoot(int owner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder findFolder(String name, Folder folder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AIFile findFile(String name, Folder folder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AIFile getFileById(int id, int farmer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder getFolderById(int id, int farmer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAITimestamp(AIFile ai) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void loadDependencies(AIFile ai) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getFolderTimestamp(Folder folder) {
		// TODO Auto-generated method stub
		return 0;
	}

}
