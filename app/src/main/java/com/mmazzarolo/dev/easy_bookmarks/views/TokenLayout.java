package com.mmazzarolo.dev.easy_bookmarks.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mmazzarolo.dev.easy_bookmarks.R;


/**
 * Created by Matteo on 01/08/2015.
 */
public class TokenLayout extends LinearLayout {

    public TokenLayout(Context context) {
        super(context);
    }

    public TokenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        TextView v = (TextView) findViewById(R.id.name);
        if (selected) {
            v.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_close_white_18dp, 0);
        } else {
            v.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_local_offer_white_18dp, 0);
        }
    }
}