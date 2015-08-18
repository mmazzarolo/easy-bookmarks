package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.mmazzarolo.dev.easy_bookmarks.FirebaseFragment;
import com.mmazzarolo.dev.easy_bookmarks.R;
import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.mmazzarolo.dev.easy_bookmarks.models.User;

import butterknife.BindString;
import butterknife.ButterKnife;

/**
 * Created by Matteo on 03/08/2015.
 *
 * Base activity extended by every activity that needs to access Firebase.
 */
public abstract class BaseActivity extends AppCompatActivity implements FirebaseFragment.Callbacks {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String SETTINGS_FIRST_RUN = "SETTINGS_FIRST_RUN";

    private FirebaseFragment mFirebaseFragment;
    private Firebase mFirebase;
    private User mUser;

    private ProgressDialog mAuthProgressDialog;
    private String mAuthToken;

    @BindString(R.string.fb_login_error) String mStringError;
    @BindString(R.string.fb_login_progess_dialog_title) String mStringDialogTitle;
    @BindString(R.string.fb_login_progess_dialog_text) String mStringDialogText;
    @BindString(R.string.sb_no_connection_action) String mStringMissingConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(getLayoutResourceId());
        ButterKnife.bind(this);

        setupFirebase();

        // If this is the first time the app runs then start the introduction activity
        if (isFirstRun()) {
            Utilities.saveSharedPreference(this, SETTINGS_FIRST_RUN, "false");
            startAppIntroActivity();
            return;
        }

        // If the app has just been authenticated with Google then authenticate with Firebase
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(LoginActivity.AUTH_TOKEN_EXTRA)) {
            mAuthToken = extras.getString(LoginActivity.AUTH_TOKEN_EXTRA);
            processFirebaseLogin();
        } else {
            //   If the app is not authenticated with Firebase then authenticate with Google (by
            //   starting a new login activity)
            if (!mFirebaseFragment.isAuthenticated() || isTokenExpired()) {
                mFirebase.unauth();
                processGoogleLogin();
                return;
            }
        }
    }

    private void setupFirebase() {
        mFirebaseFragment = Utilities.setupFirebase(getSupportFragmentManager());
        mFirebase = mFirebaseFragment.getFirebase();
        mUser = mFirebaseFragment.getUser();
    }

    private boolean isTokenExpired() {
        return (mFirebaseFragment.getAuthdata() == null
                || Utilities.isExpired(mFirebaseFragment.getAuthdata().getExpires()));
    }

    private void processGoogleLogin() {
        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    private void processFirebaseLogin() {
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle(mStringDialogTitle);
        mAuthProgressDialog.setMessage(mStringDialogText);
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();
        if (!mFirebaseFragment.isAuthenticated()) {
            mFirebaseFragment.authenticate("google", mAuthToken);
        } else {
            mAuthProgressDialog.hide();
        }
    }

    private boolean isFirstRun() {
        return Boolean.valueOf(Utilities.readSharedPreference(this, SETTINGS_FIRST_RUN, "true"));
    }

    private void startAppIntroActivity() {
        Intent intent = new Intent(getApplicationContext(), AppIntroActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mFirebaseFragment.isAuthenticated()) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_about:
                startAboutActivity();
                return true;
            case R.id.action_settings:
                startSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void logout() {
        if (mFirebaseFragment.isAuthenticated()) {
            mFirebase.unauth();
            processGoogleLogout();
        }
    }

    private void startSettingsActivity() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    private void startAboutActivity() {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
    }

    private void processGoogleLogout() {
        Intent logoutIntent = new Intent(getApplicationContext(), LoginActivity.class);
        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        logoutIntent.putExtra(LoginActivity.INTENT_SIGNOUT, true);
        startActivity(logoutIntent);
        finish();
    }

    public void showSnackbar(String message) {
        Snackbar.make(getCoordinatorLayoutView(), message, Snackbar.LENGTH_LONG).show();
    }

    public void showSnackbarForNoConnection() {
        showSnackbar(mStringMissingConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAuthProgressDialog != null) {
            mAuthProgressDialog.dismiss();
        }
    }

    /**
     * FirebaseFragment.Callbacks implementation
     */
    @Override
    public void onAuthenticated(AuthData authData) {
        invalidateOptionsMenu();
        mAuthProgressDialog.hide();
    }

    @Override
    public void onAuthenticationError(FirebaseError error) {
        invalidateOptionsMenu();
        mAuthProgressDialog.hide();
        showErrorDialog(error.getMessage());
    }

    @Override
    public void onMissingConnection() {
        showSnackbarForNoConnection();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(mStringError)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Getters and setters
     */
    protected boolean isAuthenticated() {
        return mFirebaseFragment.isAuthenticated();
    }

    public Firebase getFirebase() {
        return mFirebase;
    }

    public User getUser() {
        return mUser;
    }

    public FirebaseFragment getFirebaseFragment() {
        return mFirebaseFragment;
    }

    /**
     * Abstract methods that MUST be implemented by the extending class
     */
    protected abstract int getLayoutResourceId();

    protected abstract View getCoordinatorLayoutView();

}
