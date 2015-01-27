package de.telekom.lab.emo.gui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import de.telekom.lab.emo.Emotion_BroadcastingActivity;
import de.telekom.lab.emo.Emotions;
import de.telekom.lab.emo.R;
import de.telekom.lab.emo.db.DBManager;

public class EmoMeWidgetProvider extends AppWidgetProvider {
	// Debugging
    private static final String TAG = "EmoMeWidgetProvider";
    private static final boolean D = true;
    
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int N = appWidgetIds.length;
		int smileType= Emotions.EMOTIO_HAPPY;
		Toast.makeText(context, "onUpdate", Toast.LENGTH_SHORT);
		if (dbManager!=null){
        	if (!dbManager.isDatabaseOpen())
        		dbManager.open();
        	if (dbManager.isDatabaseOpen()){
        		smileType=dbManager.getLatestEmotionType();
        	}
        }
		 // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, Emotion_BroadcastingActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_emo_me);
            views.setOnClickPendingIntent(R.id.imageButtonReferesh, pendingIntent);
            views.setOnClickPendingIntent(R.id.imageButtonMe, pendingIntent);
            views.setImageViewResource(R.id.imageButtonMe, Emotions.getInstance().getEmotionIconByType(smileType));
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        if (D) Log.d(TAG,"onUpdate");
        
	}

	DBManager dbManager;
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		dbManager=new DBManager(context);
		dbManager.open();
        if (D) Log.d(TAG,"onEnabled");
	}
	

}
