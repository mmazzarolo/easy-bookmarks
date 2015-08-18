package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.adapters.PrivateAdapter;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;

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
public class PrivateTagDetailActivity extends BaseActivity {

    public static final String TAG = PrivateTagDetailActivity.class.getSimpleName();

    public static final String INTENT_TAG_NAME = "INTENT_TAG_NAME";

    public static final String SAVED_BOOKMARKS_LIST = "SAVED_BOOKMARKS_LIST";
    public static final String SAVED_BOOKMARKS_MAP = "SAVED_BOOKMARKS_MAP";

    private Firebase mFirebase;
    private Firebase mFirebaseListenerRef;
    private PrivateAdapter mAdapter;
    private String mUserId;

    private String mTagOfBookmark;

    private List<PrivateBookmark> mBookmarksList;
    private Map<String, PrivateBookmark> mBookmarksMap;

    @Bind(R.id.coordinatorlayout_private_tag_detail) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.recyclerview_private_tag_detail) RecyclerView mRecyclerView;
    @Bind(R.id.toolbar) Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebase = getFirebase();
        mUserId = getUser().id;

        handleIntent();

        handleSavedInstanceState(savedInstanceState);

        setupToolbar();

        setupRecyclerView();

        mFirebaseListenerRef = mFirebase.child(FirebaseFragment.PATH_PRIVATE_TAGS)
                .child(mUserId).child(mTagOfBookmark);
        mFirebaseListenerRef.addChildEventListener(privateBookmarksListener);
    }

    private void handleSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_LIST)
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_MAP)) {
            mBookmarksList = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_LIST));
            mBookmarksMap = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_MAP));
            mAdapter = new PrivateAdapter(mBookmarksList, this, PrivateTagDetailActivity.TAG);
            mAdapter.onRestoreSavedInstance(savedInstanceState);
        } else {
            mBookmarksList = new ArrayList<PrivateBookmark>();
            mBookmarksMap = new HashMap<String, PrivateBookmark>();
            mAdapter = new PrivateAdapter(mBookmarksList, this, PrivateTagDetailActivity.TAG);
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

    ChildEventListener privateBookmarksListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
            String key = dataSnapshot.getKey();
            Object value = dataSnapshot.getValue();

            if (!mBookmarksMap.containsKey(key)) {
                PrivateBookmark privateBookmark = new PrivateBookmark(dataSnapshot);
                mBookmarksMap.put(key, privateBookmark);
                mBookmarksList.add(0, privateBookmark);
                mAdapter.notifyItemInserted(0);
                mAdapter.refreshExpandedCard();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            String key = dataSnapshot.getKey();
            Object value = dataSnapshot.getValue();

            if (mBookmarksMap.containsKey(key)) {
                PrivateBookmark oldPrivateBookmark = mBookmarksMap.get(key);
                PrivateBookmark newPrivateBookmark = new PrivateBookmark(dataSnapshot);
                int position = mBookmarksList.indexOf(oldPrivateBookmark);
                mBookmarksMap.put(key, newPrivateBookmark);
                mBookmarksList.set(position, newPrivateBookmark);
                mAdapter.notifyItemChanged(position);
                mAdapter.updateExpandedBookmark();
                mAdapter.refreshExpandedCard();
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String key = dataSnapshot.getKey();
            Object value = dataSnapshot.getValue();

            if (mBookmarksMap.containsKey(key)) {
                PrivateBookmark privateBookmark = mBookmarksMap.get(key);
                int position = mBookmarksList.indexOf(privateBookmark);
                mBookmarksMap.remove(key);
                mBookmarksList.remove(privateBookmark);
                mAdapter.notifyItemRemoved(position);
                mAdapter.refreshExpandedCard();
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFirebaseListenerRef.removeEventListener(privateBookmarksListener);
    }

    /**
     * BaseActivity abstract methods
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_private_tag_detail;
    }

    @Override protected View getCoordinatorLayoutView() {
        return mCoordinatorLayout;
    }

}
