package com.leekwars.generator.test;

import com.alibaba.fastjson.JSONObject;

import leekscript.compiler.resolver.ResolverContext;

public class DbContext extends ResolverContext {

	private final int farmer;
	private final int folder;

	public DbContext(int farmer, int folder) {
		this.farmer = farmer;
		this.folder = folder;
	}

	public int getFarmer() {
		return farmer;
	}
	public int getFolder() {
		return this.folder;
	}

	@Override
	public String toString() {
		return String.valueOf(folder);
	}

	@Override
	public void toJson(JSONObject json) {

	}
}