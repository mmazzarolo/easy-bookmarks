package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.adapters.ViewPagerAdapter;
import com.mmazzarolo.dev.easy_bookmarks.adapters.ZoomOutPageTransformer;
import com.mmazzarolo.dev.easy_bookmarks.fragments.TagsFragment;

import butterknife.Bind;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.OnClick;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * Main activity.
 * A ViewPager + Tabs for browsing both personal and public bookmarks.
 * I tried to style the custom floatin action button (https://github.com/Clans/FloatingActionButton)
 * like the Google Inbox app.
 */

public class MainActivity extends BaseActivity implements FirebaseFragment.Callbacks {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String SHOWCASE_ID_1 = "SHOWCASE_ID_1";
    private static final int TAB_POSITION_PRIVATE = 0;
    private static final int TAB_POSITION_PUBLIC = 1;
    private static final String TAG_TAGS_FRAGMENT = "TAG_TAGS_FRAGMENT";

    private int mTabPosition;

    private ViewPagerAdapter mViewPagerAdapter;

    @Bind(R.id.coordinatorlayout) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.floating_action_menu) FloatingActionMenu mFab;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.tablayout) TabLayout mTablayout;
    @Bind(R.id.viewpager) ViewPager mViewpager;
    @Bind(R.id.floating_action_button_add) FloatingActionButton mFabAdd;
    @Bind(R.id.floating_action_button_tags) FloatingActionButton mFabTags;
    @Bind(R.id.floating_action_button_order) FloatingActionButton mFabOrder;
    @Bind(R.id.relativelayout_fake_fab) RelativeLayout mFakeFab;
    @Bind(R.id.relativelayout_fake) RelativeLayout mFake;

    @BindString(R.string.tab_title_private_bookmarks) String mStringTabTitlePrivateBookmarks;
    @BindString(R.string.tab_title_public_bookmarks) String mStringTabTitlePublicBookmarks;
    @BindString(R.string.fab_order_last) String mStringFabOrderLast;
    @BindString(R.string.fab_order_most) String mStringFabOrderMost;
    @BindString(R.string.fab_private_tags) String mStringFabPrivateTags;
    @BindString(R.string.fab_public_tags) String mStringFabPublicTags;
    @BindString(R.string.sb_must_be_connected) String mStringMustBeConnected;
    @BindString(R.string.sb_list_last) String mStringListLast;
    @BindString(R.string.sb_list_most) String mStringListMost;
    @BindString(R.string.showcase_intro) String mStringShowCaseIntro;
    @BindString(R.string.showcase_saved_bookmarks) String mStringShowCaseSavedBookmarks;
    @BindString(R.string.showcase_actions) String mStringShowCaseActions;
    @BindString(R.string.showcase_public) String mStringShowCasePublic;
    @BindString(R.string.showcase_got_it) String mStringShowCaseGotIt;

    @BindDrawable(R.drawable.ic_person_white_24dp) Drawable mIcPersonWhite;
    @BindDrawable(R.drawable.ic_person_teal_900_24dp) Drawable mIcPersonDark;
    @BindDrawable(R.drawable.ic_public_white_24dp) Drawable mIcPublicWhite;
    @BindDrawable(R.drawable.ic_public_teal_900_24dp) Drawable mIcPublicDark;

    @OnClick(R.id.floating_action_button_add) void onAddButtonClick() {
        mFab.toggle(true);
        Intent intent = new Intent(this, NewBookmarkActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @OnClick(R.id.floating_action_button_tags) void onTagsButtonClick() {
        mFab.toggle(true);
        if (isViewPublic() && !Utilities.isConnected(this)) {
            showSnackbar(mStringMustBeConnected);
            return;
        }
        DialogFragment tagsFragment = new TagsFragment();
        if (isViewPublic()) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(TagsFragment.EXTRA_FROM_PUBLIC, true);
            tagsFragment.setArguments(bundle);
        }
        tagsFragment.show(getSupportFragmentManager(), TAG_TAGS_FRAGMENT);
    }

    @OnClick(R.id.floating_action_button_order) void onOrderButtonClick() {
        Utilities.switchOrder(this);
        mViewPagerAdapter.getPublicBookmarksFragment().hideRecyclerView();
        mViewPagerAdapter.getPublicBookmarksFragment().showRecyclerView();
        if (Utilities.getListOrder(this).equals("numSaved")) {
            showSnackbar(mStringListMost);
        } else {
            showSnackbar(mStringListLast);
        }
        setFabOrderLabelText();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFab.setClosedOnTouchOutside(true);

        setupToolbar();

        setFabOrderLabelText();

        if (isAuthenticated()) {
            setupViewPager();
        }
    }

    private void setupToolbar() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setTitle("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupViewPager() {
        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewpager.setAdapter(mViewPagerAdapter);
        mTablayout.setupWithViewPager(mViewpager);
        mViewpager.setOffscreenPageLimit(2);
        mViewpager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTablayout) {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mTabPosition = position;
                onPageChanged();
            }
        });
        mViewpager.setPageTransformer(true, new ZoomOutPageTransformer());
        mTabPosition = TAB_POSITION_PRIVATE;
        onPageChanged();

        startShowCase();
    }

    private void startShowCase() {
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        config.setContentTextColor(getResources().getColor(R.color.md_grey_400));
        config.setDismissTextColor(getResources().getColor(R.color.white));

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID_1);

        sequence.setConfig(config);

        sequence.addSequenceItem(mFake, mStringShowCaseIntro, mStringShowCaseGotIt);

        sequence.addSequenceItem(getImageViewFromTabLayout(0), mStringShowCaseSavedBookmarks, mStringShowCaseGotIt);

        sequence.addSequenceItem(mFakeFab, mStringShowCaseActions, mStringShowCaseGotIt);

        sequence.addSequenceItem(getImageViewFromTabLayout(1), mStringShowCasePublic, mStringShowCaseGotIt);

        sequence.start();
    }

    private View getImageViewFromTabLayout(int position) {
        ViewGroup tabs = (ViewGroup) mTablayout.getChildAt(0);
        ViewGroup selectedTab = (ViewGroup) tabs.getChildAt(position);
        return selectedTab.getChildAt(0);
    }

    private void onPageChanged() {
        if (!isViewPublic()) {
            mTablayout.getTabAt(TAB_POSITION_PRIVATE).setIcon(mIcPersonWhite).setText("");
            mTablayout.getTabAt(TAB_POSITION_PUBLIC).setIcon(mIcPublicDark).setText("");
            getSupportActionBar().setTitle(mStringTabTitlePrivateBookmarks);
            mFabOrder.setVisibility(View.GONE);
            mFabTags.setLabelText(mStringFabPrivateTags);
        } else {
            mTablayout.getTabAt(TAB_POSITION_PRIVATE).setIcon(mIcPersonDark).setText("");
            mTablayout.getTabAt(TAB_POSITION_PUBLIC).setIcon(mIcPublicWhite).setText("");
            getSupportActionBar().setTitle(mStringTabTitlePublicBookmarks);
            mFabOrder.setVisibility(View.VISIBLE);
            mFabTags.setLabelText(mStringFabPublicTags);
        }
        mFab.close(true);
    }

    private void setFabOrderLabelText() {
        if (Utilities.getListOrder(this).equals("lastSaved")) {
            mFabOrder.setLabelText(mStringFabOrderMost);
        } else {
            mFabOrder.setLabelText(mStringFabOrderLast);
        }
    }

    private boolean isViewPublic() {
        return mTabPosition == TAB_POSITION_PUBLIC;
    }

    /**
     * FirebaseFragment.Callbacks implementation
     */
    @Override
    public void onAuthenticated(AuthData authData) {
        super.onAuthenticated(authData);
        setupViewPager();
    }

    @Override
    public void onAuthenticationError(FirebaseError error) {
        super.onAuthenticationError(error);
    }

    /**
     * BaseActivity abstract methods
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override protected View getCoordinatorLayoutView() {
        return mCoordinatorLayout;
    }
}
