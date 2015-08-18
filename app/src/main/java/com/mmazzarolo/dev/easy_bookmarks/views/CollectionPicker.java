package com.mmazzarolo.dev.easy_bookmarks.views;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Collection item picker custom layout from: https://github.com/anton46/Foursquare-CollectionPicker
 * I heavily customized it for this specific app.
 */

public class CollectionPicker extends LinearLayout {

    public static final int LAYOUT_WIDTH_OFFSET = 3;

    private ViewTreeObserver mViewTreeObserver;
    private LayoutInflater mInflater;

    private List<CollectionItem> mCollectionItems = new ArrayList<>();
    private LinearLayout mRow;
    private HashMap<String, Object> mCheckedItems = new HashMap<>();
    private OnCollectionItemClickListener mClickListener;
    private int mWidth;
    private int mItemMargin = 10;
    private int textPaddingLeft = 8;
    private int textPaddingRight = 8;
    private int textPaddingTop = 5;
    private int texPaddingBottom = 5;
    private int mAddIcon = android.R.drawable.ic_menu_add;
    private int mCancelIcon = android.R.drawable.ic_menu_close_clear_cancel;
    private int mLayoutBackgroundColorNormal = R.color.primary;
    private int mLayoutBackgroundColorPressed = R.color.accent;
    private int mTextColor = android.R.color.white;
    private int mRadius = 3;
    private boolean mHasLimit;
    private boolean mInitialized;

    private boolean simplifiedTags;

    private int mNumSelectedItem;

    private int mTotalPadding;
    private int mIndexFrontView;
    private LayoutParams mItemParams;


    public CollectionPicker(Context context) {
        this(context, null);
    }

    public CollectionPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CollectionPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TypedArray typeArray = context
                .obtainStyledAttributes(attrs, R.styleable.CollectionPicker);
        this.mItemMargin = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_itemMargin,
                        Utilities.dpToPx(this.getContext(), mItemMargin));
        this.textPaddingLeft = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_textPaddingLeft,
                        Utilities.dpToPx(this.getContext(), textPaddingLeft));
        this.textPaddingRight = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_textPaddingRight,
                        Utilities.dpToPx(this.getContext(),
                                textPaddingRight));
        this.textPaddingTop = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_textPaddingTop,
                        Utilities.dpToPx(this.getContext(), textPaddingTop));
        this.texPaddingBottom = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_textPaddingBottom,
                        Utilities.dpToPx(this.getContext(),
                                texPaddingBottom));
        this.mAddIcon = typeArray.getResourceId(R.styleable.CollectionPicker_cp_addIcon, mAddIcon);
        this.mCancelIcon = typeArray.getResourceId(R.styleable.CollectionPicker_cp_cancelIcon,
                mCancelIcon);
        this.mLayoutBackgroundColorNormal = typeArray.getColor(
                R.styleable.CollectionPicker_cp_itemBackgroundNormal,
                mLayoutBackgroundColorNormal);
        this.mLayoutBackgroundColorPressed = typeArray.getColor(
                R.styleable.CollectionPicker_cp_itemBackgroundPressed,
                mLayoutBackgroundColorPressed);
        this.mRadius = (int) typeArray
                .getDimension(R.styleable.CollectionPicker_cp_itemRadius, mRadius);
        this.mTextColor = typeArray
                .getColor(R.styleable.CollectionPicker_cp_itemTextColor, mTextColor);
        this.simplifiedTags = typeArray
                .getBoolean(R.styleable.CollectionPicker_cp_simplified, false);
        this.mHasLimit = typeArray
                .getBoolean(R.styleable.CollectionPicker_cp_hasLimit, false);
        typeArray.recycle();

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        mViewTreeObserver = getViewTreeObserver();
        mViewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mInitialized) {
                    mInitialized = true;
                    drawItemView();
                }
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
    }

    /**
     * Selected flags
     */
    public void setCheckedItems(HashMap<String, Object> checkedItems) {
        mCheckedItems = checkedItems;
    }

    public void addCheckedItem(CollectionItem collectionItem) {
        mCheckedItems.put(collectionItem.id, collectionItem.text);
    }

    public HashMap<String, Object> getCheckedItems() {
        return mCheckedItems;
    }

    public void drawItemView() {
        if (!mInitialized) {
            return;
        }

        clearUi();

        mTotalPadding = getPaddingLeft() + getPaddingRight();
        mIndexFrontView = 0;
        mItemParams = getItemLayoutParams();

        for (int i = 0; i < mCollectionItems.size(); i++) {
            final CollectionItem collectionItem = mCollectionItems.get(i);
            addItemToCollection(collectionItem, i);
        }

    }

    public void addItemToCollection(final CollectionItem collectionItem, final int position) {

        if (mCheckedItems != null && mCheckedItems.containsKey(collectionItem.id)) {
            collectionItem.isSelected = true;
            mNumSelectedItem++;
        }

        final View itemLayout = createItemView(collectionItem);

        itemLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                animateView(v);

                if (!simplifiedTags) {
                    if (!collectionItem.isSelected && mHasLimit && mNumSelectedItem >= 3) {
                        Logger.v("Can't add collectionItem, limit reached:" + collectionItem.id + " - " + collectionItem.text);
                    } else {
                        collectionItem.isSelected = !collectionItem.isSelected;
                        if (collectionItem.isSelected) {
                            mCheckedItems.put(collectionItem.id, collectionItem);
                            mNumSelectedItem++;
                        } else {
                            mCheckedItems.remove(collectionItem.id);
                            mNumSelectedItem--;
                        }

                        if (isJellyBeanAndAbove()) {
                            itemLayout.setBackground(getSelector(collectionItem));
                        } else {
                            itemLayout.setBackgroundDrawable(getSelector(collectionItem));
                        }
                        ImageView iconView = (ImageView) itemLayout.findViewById(R.id.item_icon);
                        iconView.setBackgroundResource(getItemIcon(collectionItem.isSelected));
                    }
                }
                if (mClickListener != null) {
                    mClickListener.onClick(collectionItem, position);
                }
            }
        });

        TextView itemTextView = (TextView) itemLayout.findViewById(R.id.item_name);
        itemTextView.setText(collectionItem.text);
        itemTextView.setPadding(textPaddingLeft, textPaddingTop, textPaddingRight,
                texPaddingBottom);
        itemTextView.setTextColor(getResources().getColor(mTextColor));

        float itemWidth = itemTextView.getPaint().measureText(collectionItem.text) + textPaddingLeft
                + textPaddingRight;

        // if (!simplifiedTags) {
        ImageView indicatorView = (ImageView) itemLayout.findViewById(R.id.item_icon);
        indicatorView.setBackgroundResource(getItemIcon(collectionItem.isSelected));
        indicatorView.setPadding(0, textPaddingTop, textPaddingRight, texPaddingBottom);

        if (simplifiedTags) {
            indicatorView.setVisibility(View.GONE);
        }

        itemWidth += Utilities.dpToPx(getContext(), 30) + textPaddingLeft
                + textPaddingRight;

        if (mWidth <= mTotalPadding + itemWidth + Utilities
                .dpToPx(this.getContext(), LAYOUT_WIDTH_OFFSET)) {
            mTotalPadding = getPaddingLeft() + getPaddingRight();
            mIndexFrontView = position;
            addItemView(itemLayout, mItemParams, true, position);
        } else {
            if (position != mIndexFrontView) {
                mItemParams.leftMargin = mItemMargin;
                mTotalPadding += mItemMargin;
            }
            addItemView(itemLayout, mItemParams, false, position);
        }
        mTotalPadding += itemWidth;
    }

    private View createItemView(CollectionItem collectionItem) {
        View view = mInflater.inflate(R.layout.tag_chooser_item, this, false);
//        if (isJellyBeanAndAbove()) {
//            view.setBackground(getSelector(collectionItem));
//        } else {
//            view.setBackgroundDrawable(getSelector(collectionItem));
//        }

        return view;
    }

    private LayoutParams getItemLayoutParams() {
        LayoutParams itemParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        itemParams.bottomMargin = mItemMargin / 2;
        itemParams.topMargin = mItemMargin / 2;

        return itemParams;
    }

    private int getItemIcon(Boolean isSelected) {
        return isSelected ? mCancelIcon : mAddIcon;
    }

    private void clearUi() {
        removeAllViews();
        mRow = null;
    }

    private void addItemView(View itemView, ViewGroup.LayoutParams chipParams, boolean newLine,
                             int position) {
        if (mRow == null || newLine) {
            mRow = new LinearLayout(getContext());
            mRow.setGravity(Gravity.CENTER);
            mRow.setOrientation(HORIZONTAL);

            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            mRow.setLayoutParams(params);

            addView(mRow);
        }

        mRow.addView(itemView, chipParams);
        animateItemView(itemView, position);
    }

    private StateListDrawable getSelector(CollectionItem collectionItem) {
        return collectionItem.isSelected ? getSelectorSelected() : getSelectorNormal();
    }

    private StateListDrawable getSelectorNormal() {
        StateListDrawable states = new StateListDrawable();

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(mLayoutBackgroundColorPressed);
//        gradientDrawable.setCornerRadius(mRadius);
//        gradientDrawable.setStroke(1, this.getContext().getResources().getColor(R.color.primary_dark));

        states.addState(new int[]{android.R.attr.state_pressed}, gradientDrawable);

        gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(mLayoutBackgroundColorNormal);
//        gradientDrawable.setCornerRadius(mRadius);
//        gradientDrawable.setStroke(1, this.getContext().getResources().getColor(R.color.primary_dark));

        states.addState(new int[]{}, gradientDrawable);

        return states;
    }

    private StateListDrawable getSelectorSelected() {
        StateListDrawable states = new StateListDrawable();

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(mLayoutBackgroundColorNormal);
//        gradientDrawable.setCornerRadius(mRadius);
//        gradientDrawable.setStroke(1, this.getContext().getResources().getColor(R.color.primary_dark));

        states.addState(new int[]{android.R.attr.state_pressed}, gradientDrawable);

        gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(mLayoutBackgroundColorPressed);
//        gradientDrawable.setCornerRadius(mRadius);
//        gradientDrawable.setStroke(1, this.getContext().getResources().getColor(R.color.primary_dark));

        states.addState(new int[]{}, gradientDrawable);

        return states;
    }

    public List<CollectionItem> getItems() {
        return mCollectionItems;
    }

    public void setItems(List<CollectionItem> collectionItems) {
        mCollectionItems = collectionItems;
    }

    public void clearItems() {
        mCollectionItems.clear();
    }

    public void setOnItemClickListener(OnCollectionItemClickListener clickListener) {
        mClickListener = clickListener;
    }

    private boolean isJellyBeanAndAbove() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
    }

    private void animateView(final View view) {
        view.setScaleY(1f);
        view.setScaleX(1f);

        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .setStartDelay(0)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reverseAnimation(view);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

    private void reverseAnimation(View view) {
        view.setScaleY(1.2f);
        view.setScaleX(1.2f);

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .setListener(null)
                .start();
    }

    private void animateItemView(View view, int position) {
        long animationDelay = 600;

        animationDelay += position * 30;

        view.setScaleY(0);
        view.setScaleX(0);
        view.animate()
                .scaleY(1)
                .scaleX(1)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .setStartDelay(animationDelay)
                .start();
    }
}