package com.leekwars.game.items;

public class Item {

	// Item types
	public final static int TYPE_WEAPON = 1;
	public final static int TYPE_CHIP = 2;
	public final static int TYPE_POTION = 3;

	private int mId;
	private int mFarmer;
	private int mTemplate;
	private int mLeek;
	private int mDuration;

	public Item(int id, int farmer, int template, int leek, int duration) {
		mId = id;
		mFarmer = farmer;
		mTemplate = template;
		mLeek = leek;
		mDuration = duration;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public int getFarmer() {
		return mFarmer;
	}

	public void setFarmer(int farmer) {
		mFarmer = farmer;
	}

	public int getTemplate() {
		return mTemplate;
	}

	public void setTemplate(int template) {
		mTemplate = template;
	}

	public int getLeek() {
		return mLeek;
	}

	public void setLeek(int leek) {
		mLeek = leek;
	}

	public int getDuration() {
		return mDuration;
	}

	public void setDuration(int duration) {
		mDuration = duration;
	}
}
