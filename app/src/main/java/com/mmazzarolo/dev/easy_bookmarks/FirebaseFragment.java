package com.mmazzarolo.dev.easy_bookmarks;

/**
 * Created by Matteo on 29/07/2015.
 */

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.ServerValue;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;
import com.mmazzarolo.dev.easy_bookmarks.models.PublicBookmark;
import com.mmazzarolo.dev.easy_bookmarks.models.User;
import com.orhanobut.logger.Logger;

public class FirebaseFragment extends Fragment implements Firebase.AuthResultHandler {

    public static String PATH_PRIVATE_BOOKMARKS = "privateBookmarks";
    public static String PATH_PRIVATE_TAGS = "privateTags";
    public static String PATH_PUBLIC_TAGS = "publicTags";
    public static String PATH_PUBLIC_TAGS_TO_BOOKMARKS = "publicTagsToBookmarks";
    public static String PATH_PUBLIC_BOOKMARKS = "publicBookmarks";
    public static String PATH_NUM_SAVED = "numSaved";
    public static String PATH_LAST_USER = "lastUser";
    public static String PATH_LAST_SAVED = "lastSaved";
    public static String PATH_DATE_SAVED = "dateSaved";

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    public interface Callbacks {
        void onAuthenticated(AuthData authData);

        void onAuthenticationError(FirebaseError firebaseError);

        void onMissingConnection();
    }

    public static final String TAG_FIREBASE_FRAGMENT = "TAG_FIREBASE_FRAGMENT";

    private Callbacks mCallbacks;
    private Context mContext;
    private Firebase mFirebase;
    private AuthData mAuthdata;
    private User mUser;
    private String mUserId;

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
        mContext = activity;
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        Firebase.setAndroidContext(getActivity().getApplicationContext());
        mFirebase = new Firebase(getString(R.string.firebase_url));
        mAuthdata = mFirebase.getAuth();

        if (isAuthenticated()) {
            saveUserInFirebase(mAuthdata);
        }
    }

    public void authenticate(String provider, String token) {
        mFirebase.authWithOAuthToken(provider, token, this);
    }

    private void saveUserInFirebase(AuthData authData) {
        String id = authData.getUid();
        String displayName = authData.getProviderData().get("displayName").toString();
        String provider = authData.getProvider();
        String providerId = authData.getProviderData().get("id").toString();

        mUser = new User(id, displayName, provider, providerId);
        mUserId = mUser.id;

        mFirebase.child("users").child(id).child("provider").setValue(provider);
        mFirebase.child("users").child(id).child("providerId").setValue(providerId);
        mFirebase.child("users").child(id).child("displayName").setValue(displayName);

        new Firebase(mFirebase + "/privateBookmarks/" + getUser().id).keepSynced(true);
        new Firebase(mFirebase + "/privateTags/" + getUser().id).keepSynced(true);
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        mAuthdata = authData;
        saveUserInFirebase(authData);
        mCallbacks.onAuthenticated(authData);
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        mCallbacks.onAuthenticationError(firebaseError);
    }

    /***********************************************************************************************
     * Firebase bookmarks management.
     * Add / Delete / Update of bookmarks and related tags.
     **********************************************************************************************/
    /**
     * Adding a bookmark to Firebase
     */
    public void addBookmark(final PrivateBookmark bookmark) {
        if (!Utilities.isConnected(mContext)) {
            mCallbacks.onMissingConnection();
            return;
        }
        Firebase privateBookmarkPath =
                mFirebase.child(PATH_PRIVATE_BOOKMARKS).child(mUserId).child(bookmark.getKey());

        privateBookmarkPath.setValue(bookmark);
        privateBookmarkPath.child(PATH_DATE_SAVED).setValue(ServerValue.TIMESTAMP);
        privateBookmarkPath.child(PATH_DATE_SAVED).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                bookmark.setDateSaved((long) dataSnapshot.getValue());
                mFirebase.child(FirebaseFragment.PATH_PUBLIC_BOOKMARKS).child(bookmark.getKey())
                                .runTransaction(new NewBookmarkTransaction(bookmark));

                for (String tag : bookmark.getTags()) {
                    addTag(tag, bookmark);
                }
            }

            @Override public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    /**
     * Updating a bookmark
     */
    public void updateBookmark(PrivateBookmark bookmarkOld, PrivateBookmark bookmarkNew) {
        if (!Utilities.isConnected(mContext)) {
            mCallbacks.onMissingConnection();
            return;
        }
        mFirebase.child(PATH_PRIVATE_BOOKMARKS).child(mUserId).child(bookmarkNew.getKey()).setValue(bookmarkNew);

        for (String tag : bookmarkOld.getTags()) {
            if (!bookmarkNew.isTaggedWith(tag)) {
                removeTag(tag, bookmarkNew);
            }
        }

        for (String tag : bookmarkNew.getTags()) {
            if (!bookmarkOld.isTaggedWith(tag)) {
                addTag(tag, bookmarkNew);
            }
        }
    }


    /**
     * Removing a bookmark from Firebase
     */
    public void removeBookmark(PrivateBookmark bookmark) {
        if (!Utilities.isConnected(mContext)) {
            mCallbacks.onMissingConnection();
            return;
        }
        mFirebase.child(PATH_PRIVATE_BOOKMARKS).child(mUserId).child(bookmark.getKey()).removeValue();

        for (String tag : bookmark.getTags()) {
            removeTag(tag, bookmark);
        }

        mFirebase.child(PATH_PUBLIC_BOOKMARKS).child(bookmark.getKey())
                .runTransaction(removePublicBookmarkTransaction);
    }

    public void addTag(String tag, PrivateBookmark bookmark) {
        if (!Utilities.isConnected(mContext)) {
            mCallbacks.onMissingConnection();
            return;
        }
        mFirebase.child(PATH_PRIVATE_TAGS).child(mUserId).child(tag).child(bookmark.getKey()).setValue(bookmark);
        mFirebase.child(FirebaseFragment.PATH_PUBLIC_TAGS_TO_BOOKMARKS).child(tag).child(bookmark.getKey()).runTransaction(addPublicTagsToBookmarksTransaction);
        mFirebase.child(FirebaseFragment.PATH_PUBLIC_TAGS).child(tag).runTransaction(addPublicTagsTransaction);
    }

    public void removeTag(String tag, PrivateBookmark bookmark) {
        if (!Utilities.isConnected(mContext)) {
            mCallbacks.onMissingConnection();
            return;
        }
        mFirebase.child(PATH_PRIVATE_TAGS).child(mUserId).child(tag).child(bookmark.getKey()).removeValue();
        mFirebase.child(FirebaseFragment.PATH_PUBLIC_TAGS_TO_BOOKMARKS).child(tag).child(bookmark.getKey()).runTransaction(removePublicTagsToBookmarksTransaction);
        mFirebase.child(FirebaseFragment.PATH_PUBLIC_TAGS).child(tag).runTransaction(removePublicTagsTransaction);
    }

    /***********************************************************************************************
     * Firebase transactions.
     * Management of PUBLIC bookmarks/tags with transaction (for incrementing/decrementing counters)
     **********************************************************************************************/
    /**
     * Add a new public bookmark
     */
    public class NewBookmarkTransaction implements Transaction.Handler {

        PrivateBookmark privateBookmark;

        public NewBookmarkTransaction(PrivateBookmark privateBookmark) {
            this.privateBookmark = privateBookmark;
        }

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() == null) {
                // Insert the bookmark
                PublicBookmark publicBookmark = new PublicBookmark(privateBookmark, mUser.getDisplayName());
                currentData.setValue(publicBookmark);
                currentData.child(FirebaseFragment.PATH_LAST_SAVED).setValue(ServerValue.TIMESTAMP);
            } else {
                // Update the bookmark
                Long newNumSaved = (Long) currentData.child(FirebaseFragment.PATH_NUM_SAVED).getValue() + 1;
                currentData.child(FirebaseFragment.PATH_NUM_SAVED).setValue(newNumSaved);
                currentData.child(FirebaseFragment.PATH_LAST_USER).setValue(mUser.getDisplayName());
                currentData.child(FirebaseFragment.PATH_LAST_SAVED).setValue(ServerValue.TIMESTAMP);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                Logger.v("Insert completed succesfully!");
            } else {
                if (firebaseError != null) {
                    Logger.v("Error: " + firebaseError.getMessage());
                } else {
                    Logger.v("Transaction not commited");
                }
            }
        }
    }
    /**
     * Remove an existing public bookmark
     */
    private Transaction.Handler removePublicBookmarkTransaction = new Transaction.Handler() {

        private boolean itemRemoved;

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            long lastSaved = (long) currentData.child(PATH_NUM_SAVED).getValue();
            if (currentData.getValue() == null || (lastSaved <= 1)) {
                currentData.child(PATH_NUM_SAVED).setValue(0);
                itemRemoved = true;
            } else {
                currentData.child(PATH_NUM_SAVED).setValue(lastSaved - 1);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                if (itemRemoved) {
                    currentData.getRef().removeValue();
                }
                Logger.v("Removed tag " + currentData.getKey());
            } else {
                Logger.e("Error in removePublicBookmarkTransaction: " + currentData.getKey());
                Logger.e(firebaseError.getMessage());
            }
        }
    };

    /**
     * Add a new public tag
     */
    private Transaction.Handler addPublicTagsToBookmarksTransaction = new Transaction.Handler() {

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() == null) {
                currentData.setValue(1);
            } else {
                currentData.setValue((Long) currentData.getValue() + 1);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                Logger.v("Added public tag " + currentData.getKey());
            } else {
                Logger.e("Error in addPublicTagsToBookmarksTransaction: " + currentData.getKey());
                Logger.e(firebaseError.getMessage());
            }
        }
    };

    private Transaction.Handler addPublicTagsTransaction = new Transaction.Handler() {

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() == null) {
                currentData.setValue(1);
            } else {
                currentData.setValue((Long) currentData.getValue() + 1);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                Logger.v("Added public tag " + currentData.getKey());
            } else {
                Logger.e("Error in addPublicTagsTransaction: " + currentData.getKey());
                Logger.e(firebaseError.getMessage());
            }
        }
    };

    /**
     * Remove an existing public tag
     */
    private Transaction.Handler removePublicTagsToBookmarksTransaction = new Transaction.Handler() {

        private boolean itemRemoved;

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() == null || ((Long) currentData.getValue()) <= 1) {
                currentData.setValue(0);
                itemRemoved = true;
            } else {
                currentData.setValue((Long) currentData.getValue() - 1);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                if (itemRemoved) {
                    currentData.getRef().removeValue();
                }
                Logger.v("Removed tag " + currentData.getKey());
            } else {
                Logger.e("Error in removePublicTagsToBookmarksTransaction: " + currentData.getKey());
                Logger.e(firebaseError.getMessage());
            }
        }
    };

    private Transaction.Handler removePublicTagsTransaction = new Transaction.Handler() {

        private boolean itemRemoved;

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() == null || ((Long) currentData.getValue()) <= 1) {
                currentData.setValue(0);
                itemRemoved = true;
            } else {
                currentData.setValue((Long) currentData.getValue() - 1);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
            if (firebaseError == null && committed) {
                if (itemRemoved) {
                    currentData.getRef().removeValue();
                }
                Logger.v("Removed tag " + currentData.getKey());
            } else {
                Logger.e("Error in removePublicTagsTransaction: " + currentData.getKey());
                Logger.e(firebaseError.getMessage());
            }
        }
    };

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public Firebase getFirebase() {
        return mFirebase;
    }

    public AuthData getAuthdata() {
        return mAuthdata;
    }

    public boolean isAuthenticated() {
        return mAuthdata != null;
    }

    public User getUser() {
        return mUser;
    }

}