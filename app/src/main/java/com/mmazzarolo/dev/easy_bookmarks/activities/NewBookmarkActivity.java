package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.OnClick;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * Created by Matteo on 29/07/2015.
 * <p/>
 * Activity for adding a new bookmark.
 */
public class NewBookmarkActivity extends BaseActivity {

    private static final String TAG = NewBookmarkActivity.class.getSimpleName();

    private static final String SHOWCASE_ID_2 = "SHOWCASE_ID_2";

    private FirebaseFragment mFirebaseFragment;
    private Firebase mFirebase;

    private String mUserId;
    private String mTitle;
    private String mUrl;
    private String mUrlEncoded;
    private boolean mUrlIsValid;

    private String mIntentUrl;

    private PrivateBookmark mPrivateBookmark;

    private ArrayAdapter<PrivateTag> mPrivateTagsAdapter;
    private List<PrivateTag> mTagsList = new ArrayList<>();

    private boolean doneShowcase;

    @Bind(R.id.imageview_favicon) ImageView mImageViewFavicon;
    @Bind(R.id.textview_title) TextView mTextViewTitle;
    @Bind(R.id.edittext_title_custom) EditText mEditTextTitleCustom;
    @Bind(R.id.edittext_add) EditText mEditTextAdd;
    @Bind(R.id.completitionview) TagsCompletitionView mCompletitionView;
    @Bind(R.id.textview_done) TextView mTextViewDone;
    @Bind(R.id.textview_cancel) TextView mTextViewCancel;
    @Bind(R.id.linearlayout_detail) LinearLayout mLinearLayoutDetail;
    @Bind(R.id.linearlayout_detail_separator) LinearLayout mLinearLayoutSeparator;
    @Bind(R.id.linearlayout_title) LinearLayout mLinearLayoutTitle;
    @Bind(R.id.progressbar) ProgressBar mProgressBar;
    @Bind(R.id.coordinatorlayout) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.imageview_paste) ImageView mPaste;
    @Bind(R.id.imageview_add_custom_title) ImageView mIconCustomTitle;
    @Bind(R.id.imageview_add_tags) ImageView mIconTags;

    @BindString(R.string.sb_enter_valid_url) String mStringEnterValidUrl;
    @BindString(R.string.sb_url_already_exists) String mStringUrlAlreadyExists;
    @BindString(R.string.sb_connection_error) String mStringConnectionError;
    @BindString(R.string.showcase_custom_title) String mStringShowCaseTitle;
    @BindString(R.string.showcase_tags) String mStringShowCaseTags;
    @BindString(R.string.showcase_got_it) String mStringShowCaseGotIt;

    @OnClick(R.id.textview_done) void onDoneClick() {
        if (Utilities.isConnected(this)) {
            if (mUrlIsValid) {
                mFirebase.child(FirebaseFragment.PATH_PRIVATE_BOOKMARKS)
                        .child(mUserId).child(mUrlEncoded)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    showSnackbar(mStringUrlAlreadyExists);
                                } else {
                                    saveBookmark();
                                }
                            }

                            @Override public void onCancelled(FirebaseError firebaseError) {

                            }
                        });
            } else {
                showSnackbar(mStringEnterValidUrl);
            }
        } else {
            showSnackbarForNoConnection();
        }
    }

    private void saveBookmark() {
        String titleCustom = mEditTextTitleCustom.getText().toString();
        mPrivateBookmark = new PrivateBookmark(mUrlEncoded, mUrl, mTitle, titleCustom);
        mPrivateBookmark.setTagsFromPrivateTagList(mCompletitionView.getObjects());
        mFirebaseFragment.addBookmark(mPrivateBookmark);
        finish();
    }

    @OnClick(R.id.textview_cancel) void onCancelClick() {
        finish();
    }

    @OnClick(R.id.imageview_paste) void onPasteClick() {
        @SuppressWarnings("deprecation")
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if ((clipboard.hasPrimaryClip()
                && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            mEditTextAdd.setText(item.getText().toString());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseFragment = getFirebaseFragment();
        mFirebase = getFirebase();
        mUserId = getUser().id;

        handleIntent();

        setupCardView();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            mIntentUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
    }

    private void setupCardView() {
        showDetails(false);
        setupTagEditor();
        setupTagsList();

        mEditTextAdd.addTextChangedListener(urlTextWatcher);

        if (!Utilities.isNullOrEmpty(mIntentUrl)) {
            mEditTextAdd.setText(mIntentUrl);
            mEditTextAdd.setHint("");
            mEditTextAdd.setEnabled(false);
            mEditTextAdd.setInputType(InputType.TYPE_NULL);
        }
    }

    private void showDetails(Boolean show) {
        if (show) {
            mLinearLayoutTitle.setVisibility(View.VISIBLE);
            mLinearLayoutSeparator.setVisibility(View.VISIBLE);
            mLinearLayoutDetail.setVisibility(View.VISIBLE);
        } else {
            mLinearLayoutTitle.setVisibility(View.GONE);
            mLinearLayoutSeparator.setVisibility(View.GONE);
            mLinearLayoutDetail.setVisibility(View.GONE);
        }
    }

    TextWatcher urlTextWatcher = (new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() != 0) {
                checkText(s.toString());
            }
        }
    });

    private void checkText(String text) {
        showDetails(false);
        mUrlIsValid = false;
        mUrl = text;
        mUrlEncoded = Utilities.encodeUrlForFirebase(mUrl);
        if (Utilities.isUrlValid(mUrl) && Utilities.isConnected(NewBookmarkActivity.this)) {
            new CheckUrlAsyncTask().execute(mUrl);
        }
    }

    private void setupCardViewDetail() {
        showDetails(true);

        mTextViewTitle.setText(mTitle);

        Picasso.with(this)
                .load(Utilities.getFaviconUrl(mUrlEncoded))
                .placeholder(R.drawable.ic_public_grey_500_24dp)
                .error(R.drawable.ic_public_grey_500_24dp)
                .into(mImageViewFavicon);

        if (!doneShowcase) {
            startShowCase();
        }

    }

    private void startShowCase() {
        doneShowcase = true;

        ShowcaseConfig config = new ShowcaseConfig();
        config.setContentTextColor(getResources().getColor(R.color.md_grey_400));
        config.setDismissTextColor(getResources().getColor(R.color.white));

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID_2);

        sequence.setConfig(config);

        sequence.addSequenceItem(mIconCustomTitle, mStringShowCaseTitle, mStringShowCaseGotIt);
        sequence.addSequenceItem(mIconTags, mStringShowCaseTags, mStringShowCaseGotIt);

        sequence.start();
    }

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

    public class CheckUrlAsyncTask extends AsyncTask<String, Void, String> {

        @Override protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            try {
                Document doc = Jsoup.connect(url).get();
                mTitle = doc.title();
                return "OK";
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return "KO";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBar.setVisibility(View.GONE);
            if (s.equals("OK")) {
                setupCardViewDetail();
                mUrlIsValid = true;
            } else {
                showDetails(false);
                showSnackbar(mStringConnectionError);
                Logger.e("KO");
            }
        }
    }

    /**
     * BaseActivity abstract methods
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_new_bookmark;
    }

    @Override protected View getCoordinatorLayoutView() {
        return mCoordinatorLayout;
    }

}
