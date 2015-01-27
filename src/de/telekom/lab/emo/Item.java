package de.telekom.lab.emo;

import android.graphics.Bitmap;

public class Item {

	private Bitmap image;
	private String text;
	private boolean checkBoxSetting;
	
	public Item() {
		// TODO Auto-generated constructor stub
	}

	
	public Item(Bitmap image, String text, boolean checkBoxSetting) {
		super();
		this.image = image;
		this.text = text;
		this.checkBoxSetting = checkBoxSetting;
	}


	public Bitmap getImage() {
		return image;
	}

	public void setImage(Bitmap image) {
		this.image = image;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isCheckBoxSetting() {
		return checkBoxSetting;
	}

	public void setCheckBoxSetting(boolean checkBoxSetting) {
		this.checkBoxSetting = checkBoxSetting;
	}

}
