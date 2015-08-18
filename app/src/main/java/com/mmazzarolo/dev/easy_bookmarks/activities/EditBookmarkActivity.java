package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateTag;
import com.mmazzarolo.dev.easy_bookmarks.views.TagsCompletitionView;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;
import com.tokenautocomplete.FilteredArrayAdapter;
import com.tokenautocomplete.TokenCompleteTextView;

import org.apmem.tools.layouts.FlowLayout;
import org.parceler.Parcels;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Matteo on 29/07/2015.
 *
 * Activity for the bookmark edit (the bookmark is in the intent).
 * Two custom views are used in setupTagEditor:
 * - Completition View for the tags edit section (https://github.com/splitwise/TokenAutoComplete)
 * - FlowLayout for for the active tags setcion (https://github.com/ApmeM/android-flowlayout)
 */
public class EditBookmarkActivity extends BaseActivity {

    private static final String TAG = EditBookmarkActivity.class.getSimpleName();

    public static final String INTENT_BOOKMARK = "INTENT_BOOKMARK";

    private FirebaseFragment mFirebaseFragment;
    private Firebase mFirebase;
    private String mUserId;

    private PrivateBookmark mPrivateBookmarksOld;
    private PrivateBookmark mPrivateBookmarksNew;

    private ArrayAdapter<PrivateTag> mPrivateTagsAdapter;
    private List<PrivateTag> mTagsList = new ArrayList<>();

    @Bind(R.id.card_view1) CardView mCardView;
    @Bind(R.id.imageview_favicon) ImageView mImageViewFavicon;
    @Bind(R.id.textview_title) TextView mTextViewTitle;
    @Bind(R.id.textview_domain) TextView mTextViewDomain;
    @Bind(R.id.textview_full_url) TextView mTextViewFullUrl;
    @Bind(R.id.textview_date_saved) TextView mTextViewDateSaved;
    @Bind(R.id.edittext_title_custom) EditText mEditTextTitleCustom;
    @Bind(R.id.completitionview) TagsCompletitionView mCompletitionView;
    @Bind(R.id.textview_done) TextView mTextViewDone;
    @Bind(R.id.textview_cancel) TextView mTextViewCancel;
    @Bind(R.id.textview_delete) TextView mTextViewDelete;
    @Bind(R.id.coordinatorlayout) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.flowlayout_tags) FlowLayout mFlowLayout;

    @BindDrawable(R.drawable.ic_warning_grey_500_24dp) Drawable mIcWarning;

    @BindString(R.string.delete_confirm_title) String mStringDeleteTitle;
    @BindString(R.string.delete_confirm_message) String mStringDeleteMessage;

    @OnClick(R.id.textview_done) void onDoneClick() {
        if (Utilities.isConnected(this)) {
            mPrivateBookmarksNew.setTagsFromPrivateTagList(mCompletitionView.getObjects());
            mPrivateBookmarksNew.setTitleCustom(mEditTextTitleCustom.getText().toString());
            mFirebaseFragment.updateBookmark(mPrivateBookmarksOld, mPrivateBookmarksNew);
            finish();
        } else {
            showSnackbarForNoConnection();
        }
    }

    @OnClick(R.id.textview_cancel) void onCancelClick() {
        finish();
    }

    @OnClick(R.id.textview_delete) void onDeleteClick() {
        new AlertDialog.Builder(this)
                .setTitle(mStringDeleteTitle)
                .setMessage(mStringDeleteMessage)
                .setIcon(mIcWarning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mFirebaseFragment.removeBookmark(mPrivateBookmarksOld);
                        finish();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebase = getFirebase();
        mFirebaseFragment = getFirebaseFragment();
        mUserId = getUser().id;

        handleIntent();

        setupCardView();

        setupTagsList();
    }

    private void handleIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(INTENT_BOOKMARK)) {
            mPrivateBookmarksOld = Parcels.unwrap(bundle.getParcelable(INTENT_BOOKMARK));
            mPrivateBookmarksNew = new PrivateBookmark(mPrivateBookmarksOld);
        }
    }

    private void setupCardView() {

        mTextViewTitle.setText(mPrivateBookmarksOld.getTitle());

        if (!Utilities.isNullOrEmpty(mPrivateBookmarksOld.getTitleCustom())) {
            mTextViewTitle.setText(mPrivateBookmarksOld.getTitleCustom());
        } else {
            mTextViewTitle.setText(mPrivateBookmarksOld.getTitle());
        }

        try {
            String url = URLDecoder.decode(mPrivateBookmarksOld.getKey(), "UTF-8");
            mTextViewDomain.setText(new URL(url).getHost());
            mTextViewFullUrl.setText(url);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }


        String lastDateSavedText = Utilities.epochToString(mPrivateBookmarksOld.getDateSaved());
        mTextViewDateSaved.setText(getResources()
                .getString(R.string.cardview_bookmark_saved_date, lastDateSavedText));

        if (mPrivateBookmarksOld.getTitleCustom() != null
                && !mPrivateBookmarksOld.getTitleCustom().equals("")) {
            mEditTextTitleCustom.setText(mPrivateBookmarksOld.getTitleCustom());
        }

        mFlowLayout.removeAllViews();

        for (String tag : mPrivateBookmarksOld.getTags()) {
            addCardTag(tag, mFlowLayout);
        }

        Picasso.with(this)
                .load(Utilities.getFaviconUrl(mPrivateBookmarksNew.getKey()))
                .placeholder(R.drawable.ic_public_grey_500_24dp)
                .error(R.drawable.ic_public_grey_500_24dp)
                .into(mImageViewFavicon);

        setupTagEditor();
    }

    private void addCardTag(String tag, FlowLayout flowLayout) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.card_tag, null);

        TextView textView = ButterKnife.findById(v, R.id.name);
        textView.setText(tag);

        flowLayout.addView(v);
    }

    // More info here: https://github.com/splitwise/TokenAutoComplete
    private void setupTagEditor() {
        mPrivateTagsAdapter = new FilteredArrayAdapter<PrivateTag>(this, R.layout.tag_layout, mTagsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                    convertView = l.inflate(R.layout.tag_layout, parent, false);
                }
                PrivateTag p = getItem(position);
                ((TextView) convertView.findViewById(R.id.name)).setText(p.getName());
                return convertView;
            }

            @Override
            protected boolean keepObject(PrivateTag tag, String mask) {
                return tag.getName().toLowerCase().startsWith(mask.toLowerCase());
            }
        };

        mCompletitionView = (TagsCompletitionView) findViewById(R.id.completitionview);
        mCompletitionView.setAdapter(mPrivateTagsAdapter);
        mCompletitionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
        mCompletitionView.setThreshold(1);
        mCompletitionView.setTokenLimit(3);
        mCompletitionView.performBestGuess(false);
        mCompletitionView.allowCollapse(false);
        mCompletitionView.allowDuplicates(false);
        char[] splitChar = {',', ';', ' ', '.'};
        mCompletitionView.setSplitChar(splitChar);

        for (String tagName : mPrivateBookmarksOld.getTags()) {
            mCompletitionView.addObject(new PrivateTag(tagName));
        }
    }

    private void setupTagsList() {
        mFirebase.child(FirebaseFragment.PATH_PRIVATE_TAGS).child(mUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mTagsList.add(new PrivateTag(child.getKey()));
                }
                mPrivateTagsAdapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * BaseActivity abstract methods
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_edit_bookmark;
    }

    @Override protected View getCoordinatorLayoutView() {
        return mCoordinatorLayout;
    }

}
