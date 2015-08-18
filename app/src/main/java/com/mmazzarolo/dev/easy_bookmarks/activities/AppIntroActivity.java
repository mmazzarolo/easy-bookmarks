package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.views.CirclePageIndicator;
import com.mmazzarolo.dev.easy_bookmarks.views.ColorShades;
import com.wnafee.vector.compat.ResourcesCompat;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Matteo on 06/08/2015.
 *
 * App introduction, thanks to https://github.com/TakeoffAndroid
 * It uses ResourcesCompat.getDrawable to extract the vector images on pre-lollipop.
 * (https://github.com/wnafee/vector-compat)
 */
public class AppIntroActivity extends AppCompatActivity {

    private static final String SAVING_STATE_SLIDER_ANIMATION = "SliderAnimationSavingState";

    private boolean isSliderAnimation = false;

    @Bind(R.id.viewpager) ViewPager mViewPager;
    @Bind(R.id.circlepageindicator) CirclePageIndicator mCirclePageIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_app_intro);
        ButterKnife.bind(this);

        setupViewPager();
    }


    private void setupViewPager() {
        mViewPager.setAdapter(new ViewPagerAdapter(this));

        mCirclePageIndicator.setViewPager(mViewPager);

        mViewPager.setPageTransformer(true, new CustomPageTransformer());

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {

                View landingBGView = findViewById(R.id.landing_backgrond);

                int colorBg[] = getResources().getIntArray(R.array.landing_bg);

                ColorShades shades = new ColorShades();
                shades.setFromColor(colorBg[position % colorBg.length])
                        .setToColor(colorBg[(position + 1) % colorBg.length])
                        .setShade(positionOffset);

                landingBGView.setBackgroundColor(shades.generate());

            }

            public void onPageSelected(int position) {
                if (position == 5) {
                    processGoogleLogin();
                }
            }

            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void processGoogleLogin() {
        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    public class ViewPagerAdapter extends PagerAdapter {

        private final int NUM_PAGES = 6;

        private Context context;

        public ViewPagerAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            Drawable icon;
            String title;
            String hint;

            if (position == 0) {
                icon = ResourcesCompat.getDrawable(context, R.drawable.vector_bookmark);
                title = getResources().getString(R.string.intro_welcome);
                hint = "";
            } else if (position == 1) {
                icon = ResourcesCompat.getDrawable(context, R.drawable.vector_book);
                title = getResources().getString(R.string.intro_save);
                hint = getResources().getString(R.string.intro_save_hint);
            } else if (position == 2) {
                icon = ResourcesCompat.getDrawable(context, R.drawable.vector_tag);
                title = getResources().getString(R.string.intro_organize);
                hint = getResources().getString(R.string.intro_organize_hint);
            } else if (position == 3) {
                icon = ResourcesCompat.getDrawable(context, R.drawable.vector_synch);
                title = getResources().getString(R.string.intro_sync);
                hint = getResources().getString(R.string.intro_sync_hint);
            } else {
                icon = ResourcesCompat.getDrawable(context, R.drawable.vector_earth);
                title = getResources().getString(R.string.intro_public);
                hint = getResources().getString(R.string.intro_public_hint);
            }

            View itemView = getLayoutInflater().inflate(R.layout.item_app_intro, container, false);

            if (position < 5) {
                ImageView iconView = (ImageView) itemView.findViewById(R.id.imageview_landing_slide);
                TextView titleView = (TextView) itemView.findViewById(R.id.textview_title);
                TextView hintView = (TextView) itemView.findViewById(R.id.textview_hint);

                iconView.setImageDrawable(icon);
                titleView.setText(title);
                hintView.setText(hint);
            }

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((RelativeLayout) object);
        }
    }

    public class CustomPageTransformer implements ViewPager.PageTransformer {


        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            View imageView = view.findViewById(R.id.imageview_landing_slide);
            View contentView = view.findViewById(R.id.textview_title);
            View txt_title = view.findViewById(R.id.textview_hint);

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left
            } else if (position <= 0) { // [-1,0]
                // This page is moving out to the left

                // Counteract the default swipe
                setTranslationX(view, pageWidth * -position);
                if (contentView != null) {
                    // But swipe the contentView
                    setTranslationX(contentView, pageWidth * position);
                    setTranslationX(txt_title, pageWidth * position);

                    setAlpha(contentView, 1 + position);
                    setAlpha(txt_title, 1 + position);
                }

                if (imageView != null) {
                    // Fade the image in
                    setAlpha(imageView, 1 + position);
                }

            } else if (position <= 1) { // (0,1]
                // This page is moving in from the right

                // Counteract the default swipe
                setTranslationX(view, pageWidth * -position);
                if (contentView != null) {
                    // But swipe the contentView
                    setTranslationX(contentView, pageWidth * position);
                    setTranslationX(txt_title, pageWidth * position);

                    setAlpha(contentView, 1 - position);
                    setAlpha(txt_title, 1 - position);

                }
                if (imageView != null) {
                    // Fade the image out
                    setAlpha(imageView, 1 - position);
                }

            }
        }

    }

    /**
     * Sets the alpha for the view. The alpha will be applied only if the running android device OS is greater than honeycomb.
     *
     * @param view  - view to which alpha to be applied.
     * @param alpha - alpha value.
     */
    private void setAlpha(View view, float alpha) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !isSliderAnimation) {
            view.setAlpha(alpha);
        }
    }

    /**
     * Sets the translationX for the view. The translation value will be applied only if the running android device OS is greater than honeycomb.
     *
     * @param view         - view to which alpha to be applied.
     * @param translationX - translationX value.
     */
    private void setTranslationX(View view, float translationX) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !isSliderAnimation) {
            view.setTranslationX(translationX);
        }
    }

    public void onSaveInstanceState(Bundle outstate) {

        if (outstate != null) {
            outstate.putBoolean(SAVING_STATE_SLIDER_ANIMATION, isSliderAnimation);
        }

        super.onSaveInstanceState(outstate);
    }

    public void onRestoreInstanceState(Bundle inState) {

        if (inState != null) {
            isSliderAnimation = inState.getBoolean(SAVING_STATE_SLIDER_ANIMATION, false);
        }
        super.onRestoreInstanceState(inState);

    }
}
