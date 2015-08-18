package com.mmazzarolo.dev.easy_bookmarks.adapters;

/**
 * Created by Matteo on 09/07/2015.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.mmazzarolo.dev.easy_bookmarks.activities.MainActivity;
import com.mmazzarolo.dev.easy_bookmarks.activities.NewBookmarkActivity;
import com.mmazzarolo.dev.easy_bookmarks.models.PublicBookmark;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;

public class PublicAdapter extends RecyclerView.Adapter<PublicAdapter.ViewHolder>
        implements View.OnLongClickListener, View.OnClickListener {

    private static final int EXPANDED_POSITION_INVALID = -1;

    private static final String SAVED_EXPANDED_POSITION = "SAVED_EXPANDED_POSITION";

    private List<PublicBookmark> mPublicBookmarks;
    private Context mContext;
    private AppCompatActivity mActivity;
    private String mActivityName;

    private int mExpandedPosition = -1;
    private PublicBookmark mExpandedBookmark;

    @BindString(R.string.share_subject) String mStringShareSubject;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageview_favicon) ImageView imageViewFavicon;
        @Bind(R.id.textview_title) TextView textViewTitle;
        @Bind(R.id.textview_domain) TextView textViewDomain;
        @Bind(R.id.linearlayout_detail) LinearLayout linearLayoutDetail;
        @Bind(R.id.textview_num_saved) TextView textViewNumSaved;
        @Bind(R.id.textview_full_url) TextView textViewFullUrl;
        @Bind(R.id.imageview_share) ImageView imageViewShare;
        @Bind(R.id.imageview_save) ImageView imageViewSave;
        @Bind(R.id.textview_score) TextView textViewScore;
        @Bind(R.id.cardview_score) CardView cardViewScore;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public PublicAdapter(List<PublicBookmark> publicBookmarks, Context context, String activityName) {
        mPublicBookmarks = publicBookmarks;
        mContext = context;
        mActivity = (AppCompatActivity) mContext;
        mActivityName = activityName;
    }

    @Override
    public PublicAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.card_bookmark_public, viewGroup, false);

        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.itemView.setOnClickListener(this);
        viewHolder.itemView.setOnLongClickListener(this);
        viewHolder.itemView.setTag(viewHolder);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        PublicBookmark bookmark = mPublicBookmarks.get(position);

        holder.textViewTitle.setText(bookmark.getTitle());

        try {
            String url = URLDecoder.decode(bookmark.getKey(), "UTF-8");
            holder.textViewDomain.setText(new URL(url).getHost());
            holder.textViewFullUrl.setText(url);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }

        String lastDateSavedText = Utilities.epochToString(bookmark.getLastSaved());
        String numSavedText;
        if (bookmark.getNumSaved() == 1) {
            numSavedText = mContext.getResources()
                    .getString(R.string.cardview_bookmark_num_saved_single, lastDateSavedText);
        } else {
            numSavedText =
                    mContext.getResources()
                            .getString(R.string.cardview_bookmark_num_saved_multi,
                                    bookmark.getNumSaved(), lastDateSavedText);
        }
        holder.textViewNumSaved.setText(numSavedText);

        Picasso.with(mContext)
                .load(Utilities.getFaviconUrl(bookmark.getKey()))
                .placeholder(R.drawable.ic_bookmark_grey_500_18dp)
                .error(R.drawable.ic_bookmark_grey_500_18dp)
                .into(holder.imageViewFavicon);

        if (mActivityName.equals(MainActivity.TAG)) {
            if (Utilities.getListOrder(mContext).equals("numSaved")) {
                holder.textViewScore.setVisibility(View.VISIBLE);
                holder.textViewScore.setText(String.valueOf(bookmark.getNumSaved()));
                holder.cardViewScore.setVisibility(View.VISIBLE);
            } else {
                holder.textViewScore.setVisibility(View.GONE);
                holder.cardViewScore.setVisibility(View.GONE);
            }
        } else {
            holder.textViewScore.setVisibility(View.GONE);
            holder.cardViewScore.setVisibility(View.GONE);
        }

        if (position == mExpandedPosition) {
            holder.linearLayoutDetail.setVisibility(View.VISIBLE);
            mExpandedBookmark = bookmark;
            holder.imageViewShare.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startShareIntent();
                }
            });
            holder.imageViewSave.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startNewBookmarkActivity();
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

    private void startNewBookmarkActivity() {
        Intent intent = new Intent(mContext, NewBookmarkActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, mExpandedBookmark.getUrl());
        mActivity.startActivity(intent);
        mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

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
            mExpandedBookmark = mPublicBookmarks.get(mExpandedPosition);
        }
    }

    public void refreshExpandedCard() {
        int newPosition = mPublicBookmarks.indexOf(mExpandedBookmark);
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
        String url = mPublicBookmarks.get(position).getUrl();
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
        return (mPublicBookmarks != null) ? mPublicBookmarks.size() : 0;
    }
}