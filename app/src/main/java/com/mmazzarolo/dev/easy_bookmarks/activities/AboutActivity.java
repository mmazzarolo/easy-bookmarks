package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;

import com.mmazzarolo.dev.easy_bookmarks.R;
import com.orhanobut.logger.Logger;

import java.io.InputStream;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;

/**
 * Created by Matteo on 18/08/2015.
 */
public class AboutActivity extends AppCompatActivity {

    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.webview) WebView mWebview;
    @Bind(R.id.collapsing_toolbar) CollapsingToolbarLayout mCollapsingToolbarLayout;

    @BindString(R.string.generic_about) String mStringAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        setupToolbar();
        mCollapsingToolbarLayout.setTitle(mStringAbout);
        mCollapsingToolbarLayout.setCollapsedTitleTextColor(getResources().getColor(R.color.white));
        mCollapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.white));
        readText();
    }

    private void readText() {
        InputStream is = getResources().openRawResource(R.raw.about);
        try {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String str = new String(buffer);
            mWebview.loadDataWithBaseURL(null, str, "text/html", "utf-8", null);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }

    }

    private void setupToolbar() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
