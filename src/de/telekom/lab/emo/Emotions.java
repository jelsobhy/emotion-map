package de.telekom.lab.emo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Emotions {
	private static Emotions INSTANCE = new Emotions();

	public static final int EMOTIO_UNKNOWN = 0;
	public static final int EMOTIO_LOL = 1;
	public static final int EMOTIO_HAPPY_PLUS = 2;
	public static final int EMOTIO_HAPPY = 3;
	public static final int EMOTIO_LOVE = 4;
	public static final int EMOTIO_NORMAL = 5;
	public static final int EMOTIO_OPS = 6;
	public static final int EMOTIO_ANGRY = 7;
	public static final int EMOTIO_CRY = 8;
	public static final int EMOTIO_SAD = 9;

	public int[] icons = {
			R.drawable.emotion_lol, R.drawable.emotion_happy_plus,
			R.drawable.emotion_happy, R.drawable.emotion_love,
			R.drawable.emotion_hoom, R.drawable.emotion_normal,
			R.drawable.emotion_angry, R.drawable.emotion_cry,
			R.drawable.emotion_sad, };

	public String[] buttonNames = { "LOL", "Happy+", "Happy", "Love it",
			"Hoom?", "Normal", "Angry", "Cry", "Sad" };

	private final int[] emotionPos2IDMap;
	private final int[] emotionID2IconMap;
	int maxEmoID = 0;
	private final Bitmap[] bitmapArray;

	public static Emotions getInstance() {
		return INSTANCE;
	}

	public Emotions() {
		emotionPos2IDMap = new int[9];
		maxEmoID = EMOTIO_UNKNOWN;
		emotionPos2IDMap[0] = EMOTIO_LOL;
		emotionPos2IDMap[1] = EMOTIO_HAPPY_PLUS;
		emotionPos2IDMap[2] = EMOTIO_HAPPY;
		emotionPos2IDMap[3] = EMOTIO_LOVE;
		emotionPos2IDMap[4] = EMOTIO_OPS;
		emotionPos2IDMap[5] = EMOTIO_NORMAL;
		emotionPos2IDMap[6] = EMOTIO_ANGRY;
		emotionPos2IDMap[7] = EMOTIO_CRY;
		emotionPos2IDMap[8] = EMOTIO_SAD;

		for (int i = 0; i < emotionPos2IDMap.length; i++)
			if (emotionPos2IDMap[i] > maxEmoID)
				maxEmoID = emotionPos2IDMap[i];

		emotionID2IconMap = new int[maxEmoID + 1];
		for (int i = 0; i < emotionPos2IDMap.length; i++)
			emotionID2IconMap[emotionPos2IDMap[i]] = i;
		bitmapArray = new Bitmap[icons.length];
	}

	public int getEmotionIcon(int position) {
		if (position >= 0 && position < icons.length)
			return icons[position];
		return icons[0];
	}

	public int getEmotionIconByType(int type) {
		if (type >= 0 && type < emotionID2IconMap.length
				&& emotionID2IconMap[type] >= 0
				&& emotionID2IconMap[type] < icons.length)
			return icons[emotionID2IconMap[type]];
		return icons[0];
	}

	public String getEmotionTextByType(int type) {
		if (type >= 0 && type < emotionID2IconMap.length
				&& emotionID2IconMap[type] >= 0
				&& emotionID2IconMap[type] < icons.length)
			return buttonNames[emotionID2IconMap[type]];
		return buttonNames[0];
	}

	public String getEmotionText(int position) {
		if (position >= 0 && position < buttonNames.length)
			return buttonNames[position];
		return buttonNames[0];

	}

	public int getEmotionID(int position) {
		if (position >= 0 && position < emotionPos2IDMap.length)
			return emotionPos2IDMap[position];
		return -1;
	}

	public int getMenuCount() {
		return buttonNames.length;
	}

	public Bitmap getBitmap(int type, Context context) {
		int id = 0;
		BitmapFactory.Options bfOptions = new BitmapFactory.Options();

		bfOptions.inPurgeable = true; // Tell to gc that whether it needs free
										// memory, the Bitmap can be cleared
		bfOptions.inInputShareable = true; // Which kind of reference will be
											// used to recover the Bitmap data
											// after being clear, when it will
											// be used in the future
		if (type >= 0 && type < emotionID2IconMap.length
				&& emotionID2IconMap[type] >= 0
				&& emotionID2IconMap[type] < icons.length)
			id = emotionID2IconMap[type];

		if (bitmapArray[id] == null) {
			bitmapArray[id] = BitmapFactory.decodeResource(
					context.getResources(), icons[id], bfOptions);
		}
		return bitmapArray[id];
	}

	public int getMaximumEmotionType() {
		return maxEmoID;
	}

}
