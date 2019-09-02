package com.leekwars.generator;

import leekscript.compiler.resolver.ResolverContext;

public class DbContext extends ResolverContext {

	private int folder;

	public DbContext(int folder) {
		this.folder = folder;
	}
	
	public int getFolder() {
		return this.folder;
	}

	@Override
	public String toString() {
		return String.valueOf(folder);
	}
}