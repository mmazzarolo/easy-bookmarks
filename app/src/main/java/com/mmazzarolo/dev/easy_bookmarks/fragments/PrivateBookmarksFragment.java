package com.mmazzarolo.dev.easy_bookmarks.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.activities.MainActivity;
import com.mmazzarolo.dev.easy_bookmarks.adapters.PrivateAdapter;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;
import com.mmazzarolo.dev.easy_bookmarks.models.User;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;

/**
 * Created by Matteo on 13/07/2015.
 */
public class PrivateBookmarksFragment extends Fragment {

    public static final String EXTRA_TAG_OF_BOOKMARKS = "EXTRA_TAG_OF_BOOKMARKS";

    public static final String SAVED_BOOKMARKS_LIST = "SAVED_BOOKMARKS_LIST";
    public static final String SAVED_BOOKMARKS_MAP = "SAVED_BOOKMARKS_MAP";

    private Firebase mFirebase;
    private Firebase mFirebaseListenerRef;
    private User mUser;
    private PrivateAdapter mAdapter;

    private Context mContext;
    private AppCompatActivity mActivity;

    private String mTagOfBookmark;

    private List<PrivateBookmark> mBookmarksList;
    private Map<String, PrivateBookmark> mBookmarksMap;

    @Bind(R.id.recyclerview_private_bookmarks) RecyclerView mRecyclerView;
    @Bind(R.id.linearlayout_no_bookmarks) LinearLayout mLinearLayoutNoBookmarks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mActivity = (AppCompatActivity) mContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFirebase();
        handleSavedInstanceState(savedInstanceState);
        handleArguments();
    }

    private void setupFirebase() {
        FirebaseFragment firebaseFragment;
        firebaseFragment = Utilities.setupFirebase(mActivity.getSupportFragmentManager());
        mFirebase = firebaseFragment.getFirebase();
        mUser = firebaseFragment.getUser();
    }

    private void handleSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_LIST)
                && savedInstanceState.containsKey(SAVED_BOOKMARKS_MAP)) {
            mBookmarksList = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_LIST));
            mBookmarksMap = Parcels.unwrap(savedInstanceState.getParcelable(SAVED_BOOKMARKS_MAP));
            mAdapter = new PrivateAdapter(mBookmarksList, mContext, MainActivity.TAG);
            mAdapter.onRestoreSavedInstance(savedInstanceState);
        } else {
            mBookmarksList = new ArrayList<PrivateBookmark>();
            mBookmarksMap = new HashMap<String, PrivateBookmark>();
            mAdapter = new PrivateAdapter(mBookmarksList, mContext, MainActivity.TAG);
        }
    }

    private void handleArguments() {
        Bundle bundle = this.getArguments();
        if (bundle != null && bundle.containsKey(EXTRA_TAG_OF_BOOKMARKS)) {
            mTagOfBookmark = bundle.getString(EXTRA_TAG_OF_BOOKMARKS);
            mFirebaseListenerRef = mFirebase
                    .child(FirebaseFragment.PATH_PRIVATE_TAGS)
                    .child(mUser.id)
                    .child(mTagOfBookmark);
        } else {
            mFirebaseListenerRef = mFirebase.child(FirebaseFragment.PATH_PRIVATE_BOOKMARKS)
                    .child(mUser.id);
        }
        mFirebaseListenerRef.orderByChild(FirebaseFragment.PATH_DATE_SAVED).
                addChildEventListener(privateBookmarksListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_bookmarks, container, false);
        ButterKnife.bind(this, view);

        checkIfThereAreBookmarks();

        if (!Utilities.isTablet(mContext)) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        } else {
            mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        }
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new FadeInDownAnimator());

        return view;
    }

    private void checkIfThereAreBookmarks() {
        mFirebase.child(FirebaseFragment.PATH_PRIVATE_BOOKMARKS).child(mUser.id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mLinearLayoutNoBookmarks.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });
    }

    ChildEventListener privateBookmarksListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
            mLinearLayoutNoBookmarks.setVisibility(View.GONE);

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

            if (mBookmarksList.isEmpty()) {
                mLinearLayoutNoBookmarks.setVisibility(View.VISIBLE);
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
}