package com.leekwars.game.items;

public class ItemTemplate {

	private int mId;
	private String mName;
	private int mType;
	private int mPrice;
	private int mLevel;
	private String mParams;

	public ItemTemplate(int id, String name, int type, int price, int level, String params) {
		mId = id;
		mName = name;
		mType = type;
		mPrice = price;
		mLevel = level;
		mParams = params;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		mType = type;
	}

	public int getPrice() {
		return mPrice;
	}

	public void setPrice(int price) {
		mPrice = price;
	}

	public int getLevel() {
		return mLevel;
	}

	public void setLevel(int level) {
		this.mLevel = level;
	}

	public String getParams() {
		return mParams;
	}

	public void setParams(String params) {
		mParams = params;
	}

}
