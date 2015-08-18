package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.adapters.PublicAdapter;
import com.mmazzarolo.dev.easy_bookmarks.models.PublicBookmark;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;

/**
 * Created by Matteo on 29/07/2015.
 */
public class PublicTagDetailActivity extends BaseActivity {

    public static final String TAG = PublicTagDetailActivity.class.getSimpleName();

    public static final String INTENT_TAG_NAME = "INTENT_TAG_NAME";

    public static final String SAVED_BOOKMARKS_LIST = "SAVED_BOOKMARKS_LIST";
    public static final String SAVED_BOOKMARKS_MAP = "SAVED_BOOKMARKS_MAP";

    private Firebase mFirebase;
    private PublicAdapter mAdapter;

    private String mTagOfBookmark;

    private List<PublicBookmark> mBookmarksList;
    private Map<String, PublicBookmark> mBookmarksMap;

    @Bind(R.id.coordinatorlayout_public_tag_detail) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.recyclerview_public_tag_detail) RecyclerView mRecyclerView;
    @Bind(R.id.toolbar) Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebase = getFirebase();

        handleIntent();

        handleSavedInstanceState(savedInstanceState);

        setupToolbar();

        setupRecyclerView();

        mFirebase.child(FirebaseFragment.PATH_PUBLIC_TAGS_TO_BOOKMARKS).child(mTagOfBookmark)
                .addListenerForSingleValueEvent(tagsListener);
    }

    private void handleSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_LIST)
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_MAP)) {
            mBookmarksList = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_LIST));
            mBookmarksMap = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_MAP));
            mAdapter = new PublicAdapter(mBookmarksList, this, PublicTagDetailActivity.TAG);
            mAdapter.onRestoreSavedInstance(savedInstanceState);
        } else {
            mBookmarksList = new ArrayList<PublicBookmark>();
            mBookmarksMap = new HashMap<String, PublicBookmark>();
            mAdapter = new PublicAdapter(mBookmarksList, this, PublicTagDetailActivity.TAG);
        }
    }

    private void handleIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(INTENT_TAG_NAME)) {
            mTagOfBookmark = bundle.getString(INTENT_TAG_NAME);
        }
    }

    private void setupToolbar() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Browsing tag: " + mTagOfBookmark);
        }
    }

    private void setupRecyclerView() {
        if (!Utilities.isTablet(this)) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            mRecyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        }
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new FadeInDownAnimator());
    }

    ValueEventListener tagsListener = (new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            for (DataSnapshot child : snapshot.getChildren()) {
                if (!mBookmarksMap.containsKey(child.getKey())) {
                    mFirebase.child(FirebaseFragment.PATH_PUBLIC_BOOKMARKS).child(child.getKey())
                            .addListenerForSingleValueEvent(bookmarksListener);
                }
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
        }
    });

    ValueEventListener bookmarksListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            Object value = snapshot.getValue();

            PublicBookmark publicBookmark= new PublicBookmark(snapshot);

            mBookmarksMap.put(key, publicBookmark);
            mBookmarksList.add(0, publicBookmark);
            mAdapter.notifyItemInserted(0);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_BOOKMARKS_LIST, Parcels.wrap(mBookmarksList));
        outState.putParcelable(SAVED_BOOKMARKS_MAP, Parcels.wrap(mBookmarksMap));
        mAdapter.onSaveInstanceState(outState);
    }

    /**
     * BaseActivity abstract methods
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_public_tag_detail;
    }

    @Override protected View getCoordinatorLayoutView() {
        return mCoordinatorLayout;
    }

}
