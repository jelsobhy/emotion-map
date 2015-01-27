package de.telekom.lab.emo.gui;

import java.util.Calendar;

public interface RangeChangeListener {
	public abstract void onRangeChanged(Calendar from, Calendar until);
}
