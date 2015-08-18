package com.mmazzarolo.dev.easy_bookmarks.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.models.PrivateTag;
import com.tokenautocomplete.TokenCompleteTextView;

/**
 * Created by Matteo on 01/08/2015.
 */
public class TagsCompletitionView extends TokenCompleteTextView<PrivateTag> {

        public TagsCompletitionView(Context context) {
            super(context);
        }

        public TagsCompletitionView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TagsCompletitionView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected View getViewForObject(PrivateTag tag) {

            LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            LinearLayout view = (LinearLayout)l.inflate(R.layout.tag_token, (ViewGroup) TagsCompletitionView.this.getParent(), false);
            ((TextView)view.findViewById(R.id.name)).setText(tag.getName());

            return view;
        }

        @Override
        protected PrivateTag defaultObject(String completionText) {
            return new PrivateTag(completionText);
        }
    }
