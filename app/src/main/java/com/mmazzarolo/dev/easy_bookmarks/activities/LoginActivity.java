package com.mmazzarolo.dev.easy_bookmarks.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.mmazzarolo.dev.easy_bookmarks.R;

import java.io.IOException;

import butterknife.Bind;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Matteo on 17/07/2015.
 *
 * Login with google activity.
 * On a successfull connection it starts MainActivity with the Google Sign-in token, ready for being
 * processed by Firebase for the Firebase Login.
 */
public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int RC_GOOGLE_LOGIN = 1;

    public static final String AUTH_TOKEN_EXTRA = "AUTH_TOKEN_EXTRA";
    public static final String INTENT_SIGNOUT = "INTENT_SIGNOUT";

    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult mGoogleConnectionResult;
    private boolean mGoogleIntentInProgress;
    private boolean mSignInClicked = false;

    @Bind(R.id.button_login) SignInButton mLoginButton;
    @Bind(R.id.progressbar) ProgressBar mProgressBar;
    @Bind(R.id.textview_connecting) TextView mTextViewConnecting;

    @BindString(R.string.login_network_error) String mStringNetworkError;
    @BindString(R.string.login_auth_error) String mStringAuthError;
    @BindString(R.string.generic_error) String mStringGenericError;

    @BindDrawable(R.drawable.ic_error_grey_500_24dp) Drawable mIcError;

    @OnClick(R.id.button_login) void onClick() {
        login();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mProgressBar.setVisibility(View.INVISIBLE);
        mTextViewConnecting.setVisibility(View.INVISIBLE);

        setupGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        boolean signout = intent.getBooleanExtra(INTENT_SIGNOUT, false);
        if (signout) {
            signOut();
        } else if (!mGoogleIntentInProgress) {
            // auto sign in
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    public void login() {
        mSignInClicked = true;
        mLoginButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mTextViewConnecting.setVisibility(View.VISIBLE);
        if (!mGoogleApiClient.isConnecting()) {
            if (mGoogleConnectionResult != null) {
                resolveSignInError();
            } else if (mGoogleApiClient.isConnected()) {
                getTokenTask.execute();
            } else {
                Log.d(TAG, "Trying to connect to Google API");
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * This method fires when any startActivityForResult finishes. The requestCode maps to
     * the value passed into startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_LOGIN) {
            if (resultCode != Activity.RESULT_OK) {
                mSignInClicked = false;
            }
            mGoogleIntentInProgress = false;
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    // A helper method to resolve the current ConnectionResult error
    private void resolveSignInError() {
        if (mGoogleConnectionResult.hasResolution()) {
            try {
                mGoogleIntentInProgress = true;
                mGoogleConnectionResult.startResolutionForResult(this, RC_GOOGLE_LOGIN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    AsyncTask<Void, Void, String> getTokenTask = new AsyncTask<Void, Void, String>() {
        String errorMessage = null;

        @Override
        protected String doInBackground(Void... params) {
            String token = null;

            try {
                String scope = String.format("oauth2:%s", Scopes.PROFILE);
                token = GoogleAuthUtil.getToken(LoginActivity.this, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
            } catch (IOException transientEx) {
                    /* Network or server error */
                Log.e(TAG, "Error authenticating with Google: " + transientEx);
                errorMessage = mStringNetworkError + transientEx.getMessage();
                showAlertDialog(errorMessage);
            } catch (UserRecoverableAuthException e) {
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                if (!mGoogleIntentInProgress) {
                    mGoogleIntentInProgress = true;
                    Intent recover = e.getIntent();
                    startActivityForResult(recover, RC_GOOGLE_LOGIN);
                }
            } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                errorMessage = mStringAuthError + authEx.getMessage();
                showAlertDialog(errorMessage);
            }
            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            mSignInClicked = false;
            mLoginButton.setEnabled(true);
            mProgressBar.setVisibility(View.INVISIBLE);
            mTextViewConnecting.setVisibility(View.INVISIBLE);
            Intent intentWithToken = new Intent(LoginActivity.this, MainActivity.class);
            intentWithToken.putExtra(AUTH_TOKEN_EXTRA, token);
            intentWithToken.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentWithToken);
            finish();
        }
    };


    @Override
    public void onConnected(final Bundle bundle) {
        // Connected with Google API
        getTokenTask.execute();
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mGoogleIntentInProgress) {
            /* Store the ConnectionResult so that we can use it later when the user clicks on the Google+ login button */
            mGoogleConnectionResult = result;

            if (mSignInClicked) {
                /* The user has already clicked login so we attempt to resolve all errors until the user is signed in,
                 * or they cancel. */
                resolveSignInError();
            } else {
                Log.e(TAG, result.toString());
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // ignore
    }

    private void showAlertDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle(mStringGenericError)
                .setMessage(error)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(mIcError)
                .show();
    }

    public void signOut() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }
    }

}
