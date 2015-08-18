package com.mmazzarolo.dev.easy_bookmarks.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.mmazzarolo.dev.easy_bookmarks.R;

/**
 * Created by Matteo on 12/08/2015.
 */
public class MyWidgetProvider extends AppWidgetProvider {

    public static String ACTION_CLICK_URL = "ACTION_CLICK_URL";
    public static String EXTRA_ITEM_URL = "EXTRA_ITEM_URL";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void update(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds =
                appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        update(context, appWidgetManager, appWidgetIds);
    }

    public void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_provider_layout);

            // Setting the ListView intent and adatper
            Intent intent = new Intent(context, MyWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setEmptyView(R.id.listview, R.id.empty_view);
            remoteViews.setRemoteAdapter(R.id.listview, intent);

            // Setting the refresh button intent
            Intent refreshIntent = new Intent(context, MyWidgetProvider.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent);

            // Setting the click intent on the list items
            Intent clickIntent = new Intent(context, MyWidgetProvider.class);
            clickIntent.setAction(ACTION_CLICK_URL);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.listview, clickPendingIntent);

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.listview);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_CLICK_URL)) {
            String url = intent.getStringExtra(EXTRA_ITEM_URL);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            update(context);
        }
        super.onReceive(context, intent);
    }

}