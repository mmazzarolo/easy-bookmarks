package com.mmazzarolo.dev.easy_bookmarks.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matteo on 12/08/2015.
 */
public class MyViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private int appWidgetId;
    private List<PrivateBookmark> mBookmarks = new ArrayList<PrivateBookmark>();
    private String mUserid;
    private Firebase mFirebase;

    public MyViewsFactory(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        Firebase.setAndroidContext(context);
        mFirebase = new Firebase(context.getString(R.string.firebase_url));

        if (mFirebase.getAuth() != null) {
            mUserid = mFirebase.getAuth().getUid();
            getList();
        }
    }

    private void getList() {
        mFirebase.child(FirebaseFragment.PATH_PRIVATE_BOOKMARKS).child(mUserid)
                .orderByChild(FirebaseFragment.PATH_DATE_SAVED)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mBookmarks.add(new PrivateBookmark(child));
                }
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int appWidgetIds[] = appWidgetManager
                        .getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listview);
            }

            @Override public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return mBookmarks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_item);

        PrivateBookmark bookmark = mBookmarks.get(position);

        if (!Utilities.isNullOrEmpty(bookmark.getTitleCustom())) {
            remoteViews.setTextViewText(R.id.textview_title, bookmark.getTitleCustom());
        } else {
            remoteViews.setTextViewText(R.id.textview_title, bookmark.getTitle());
        }

        try {
            String url = URLDecoder.decode(bookmark.getKey(), "UTF-8");
            remoteViews.setTextViewText(R.id.textview_domain, new URL(url).getHost());
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }

//            http://stackoverflow.com/questions/24771297/picasso-load-images-to-widget-listview
        try {
            Bitmap b = Picasso.with(context).load(Utilities.getFaviconUrl(bookmark.getKey())).get();
            remoteViews.setImageViewBitmap(R.id.imageview_favicon, b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bundle extras = new Bundle();
        extras.putString(MyWidgetProvider.EXTRA_ITEM_URL, bookmark.getUrl());
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
    }
}
