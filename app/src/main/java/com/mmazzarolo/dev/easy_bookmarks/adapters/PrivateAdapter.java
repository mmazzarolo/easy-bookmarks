package com.mmazzarolo.dev.easy_bookmarks.adapters;

/**
 * Created by Matteo on 09/07/2015.
 */

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.activities.BaseActivity;
import com.mmazzarolo.dev.easy_bookmarks.activities.EditBookmarkActivity;
import com.mmazzarolo.dev.easy_bookmarks.activities.PrivateTagDetailActivity;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateBookmark;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import org.apmem.tools.layouts.FlowLayout;
import org.parceler.Parcels;

import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;

public class PrivateAdapter extends RecyclerView.Adapter<PrivateAdapter.ViewHolder>
        implements View.OnLongClickListener, View.OnClickListener {

    private static final int EXPANDED_POSITION_INVALID = -1;

    private static final String SAVED_EXPANDED_POSITION = "SAVED_EXPANDED_POSITION";

    private List<PrivateBookmark> mPrivateBookmarks;
    private Context mContext;
    private BaseActivity mActivity;
    private String mActivityName;

    private int mExpandedPosition = -1;
    private PrivateBookmark mExpandedBookmark;

    @BindString(R.string.share_subject) String mStringShareSubject;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.card_view1) CardView cardView;
        @Bind(R.id.imageview_favicon) ImageView imageViewFavicon;
        @Bind(R.id.textview_title) TextView textViewTitle;
        @Bind(R.id.textview_domain) TextView textViewDomain;
        @Bind(R.id.linearlayout_detail) LinearLayout linearLayoutDetail;
        @Bind(R.id.flowlayout_tags) FlowLayout flowLayout;
        @Bind(R.id.textview_date_saved) TextView textViewDateSaved;
        @Bind(R.id.textview_full_url) TextView textViewFullUrl;
        @Bind(R.id.imageview_delete) ImageView imageViewDelete;
        @Bind(R.id.imageview_share) ImageView imageViewShare;
        @Bind(R.id.imageview_edit) ImageView imageViewEdit;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public PrivateAdapter(List<PrivateBookmark> privateBookmarks, Context context, String activityName) {
        mPrivateBookmarks = privateBookmarks;
        mContext = context;
        mActivity = (BaseActivity) mContext;
        mActivityName = activityName;
    }

    @Override
    public PrivateAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.card_bookmark_private, viewGroup, false);

        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.itemView.setOnClickListener(this);
        viewHolder.itemView.setOnLongClickListener(this);
        viewHolder.itemView.setTag(viewHolder);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        PrivateBookmark bookmark = mPrivateBookmarks.get(position);

        if (!Utilities.isNullOrEmpty(bookmark.getTitleCustom())) {
            holder.textViewTitle.setText(bookmark.getTitleCustom());
        } else {
            holder.textViewTitle.setText(bookmark.getTitle());
        }

        try {
            String url = URLDecoder.decode(bookmark.getKey(), "UTF-8");
            holder.textViewDomain.setText(new URL(url).getHost());
            holder.textViewFullUrl.setText(url);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }


        String lastDateSavedText = Utilities.epochToString(bookmark.getDateSaved());
        holder.textViewDateSaved.setText(mContext.getResources()
                .getString(R.string.cardview_bookmark_saved_date, lastDateSavedText));

        Picasso.with(mContext)
                .load(Utilities.getFaviconUrl(bookmark.getKey()))
                .placeholder(R.drawable.ic_bookmark_grey_500_18dp)
                .error(R.drawable.ic_bookmark_grey_500_18dp)
                .into(holder.imageViewFavicon);

        holder.flowLayout.removeAllViews();

        for (String tag : bookmark.getTags()) {
            addCardTag(tag, holder.flowLayout);
        }

        if (position == mExpandedPosition) {
            holder.linearLayoutDetail.setVisibility(View.VISIBLE);
            mExpandedBookmark = bookmark;
            holder.imageViewEdit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startEditBookmarkActivity(holder.cardView);
                }
            });
            holder.imageViewShare.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startShareIntent();
                }
            });
            holder.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    deleteBookmark();
                }
            });
        } else {
            holder.linearLayoutDetail.setVisibility(View.GONE);
        }
    }

    private void startShareIntent() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, mExpandedBookmark.getTitle());
        i.putExtra(Intent.EXTRA_TEXT, mExpandedBookmark.getUrl());
        mActivity.startActivity(Intent.createChooser(i, mStringShareSubject));
    }

    private void startEditBookmarkActivity(View view) {
        Intent intent = new Intent(mContext, EditBookmarkActivity.class);
        intent.putExtra(EditBookmarkActivity.INTENT_BOOKMARK, Parcels.wrap(mExpandedBookmark));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String transitionName = mContext.getString(R.string.trans_cardview);
            ActivityOptions transitionActivityOptions =
                    ActivityOptions.makeSceneTransitionAnimation(mActivity, view, transitionName);
            mActivity.startActivity(intent, transitionActivityOptions.toBundle());
        } else {
            mActivity.startActivity(intent);
            mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void addCardTag(String tag, FlowLayout flowLayout) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.card_tag, null);
        v.setOnClickListener(onTagClick);

        TextView textView = ButterKnife.findById(v, R.id.name);
        textView.setText(tag);

        flowLayout.addView(v);
    }

    private void deleteBookmark() {
        String stringDeleteTitle = mContext.getString(R.string.delete_confirm_title);
        String stringDeleteMessage = mContext.getString(R.string.delete_confirm_message);
        Drawable icWarning = ContextCompat.getDrawable(mContext, R.drawable.ic_warning_grey_500_24dp);
        new AlertDialog.Builder(mContext)
                .setTitle(stringDeleteTitle)
                .setMessage(stringDeleteMessage)
                .setIcon(icWarning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mActivity.getFirebaseFragment().removeBookmark(mExpandedBookmark);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private View.OnClickListener onTagClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intentWithToken = new Intent(mContext, PrivateTagDetailActivity.class);
            TextView textView = ButterKnife.findById(v, R.id.name);
            intentWithToken.putExtra(PrivateTagDetailActivity.INTENT_TAG_NAME, textView.getText().toString());
            mActivity.startActivity(intentWithToken);
            if (mActivityName.equals(PrivateTagDetailActivity.TAG)) {
                mActivity.finish();
            }
        }
    };

    @Override
    public void onClick(View view) {
        if (Utilities.onClickExpand(mContext)) {
            expandCard(view);
        } else {
            openUrl(view);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (Utilities.onClickExpand(mContext)) {
            openUrl(view);
        } else {
            expandCard(view);
        }
        return true;
    }

    public void updateExpandedBookmark() {
        if (mExpandedPosition != EXPANDED_POSITION_INVALID) {
            mExpandedBookmark = mPrivateBookmarks.get(mExpandedPosition);
        }
    }

    public void refreshExpandedCard() {
        int newPosition = mPrivateBookmarks.indexOf(mExpandedBookmark);

        if (newPosition != mExpandedPosition && mExpandedPosition != EXPANDED_POSITION_INVALID) {
            expandCard(newPosition);
        }
    }

    private void expandCard(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        expandCard(holder.getPosition());
    }

    private void expandCard(int position) {
        // Check for an expanded view, collapse if you find one
        if (mExpandedPosition != EXPANDED_POSITION_INVALID) {
            int prev = mExpandedPosition;
            notifyItemChanged(prev);
        }

        // If the position clicked is the same of the expanded view then collapse it
        if (mExpandedPosition == position) {
            mExpandedPosition = EXPANDED_POSITION_INVALID;
        } else {
            mExpandedPosition = position;
        }
        notifyItemChanged(mExpandedPosition);
    }

    private void openUrl(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        int position = holder.getPosition();
        String url = mPrivateBookmarks.get(position).getUrl();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        mContext.startActivity(i);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_EXPANDED_POSITION, mExpandedPosition);
    }

    public void onRestoreSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_EXPANDED_POSITION)) {
            mExpandedPosition =
                    savedInstanceState.getInt(SAVED_EXPANDED_POSITION, EXPANDED_POSITION_INVALID);
        }
    }

    @Override
    public int getItemCount() {
        return (mPrivateBookmarks != null) ? mPrivateBookmarks.size() : 0;
    }
}