/*
 * Copyright (c) 2016-2017, Michael A. Updike All rights reserved.
 * Licensed under Apache 2.0
 * https://opensource.org/licenses/Apache-2.0
 * https://github.com/Pushy-Clipboard/pushy-android/blob/master/LICENSE.md
 */

package com.weebly.opus1269.clipman.ui.signin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.Log;
import com.weebly.opus1269.clipman.model.Devices;
import com.weebly.opus1269.clipman.model.Prefs;
import com.weebly.opus1269.clipman.model.User;
import com.weebly.opus1269.clipman.msg.MessagingClient;
import com.weebly.opus1269.clipman.msg.RegistrationClient;
import com.weebly.opus1269.clipman.ui.base.BaseActivity;

/**
 * This Activity handles account selection, sign-in, registration and
 * authorization
 */
public class SignInActivity extends BaseActivity implements
  GoogleApiClient.OnConnectionFailedListener,
  OnCompleteListener<AuthResult>,
  View.OnClickListener {

  /**
   * Result of Google signIn attempt - {@value}
   */
  private static final int RC_SIGN_IN = 9001;

  /**
   * Google authorization
   */
  private GoogleApiClient mGoogleApiClient = null;

  /**
   * Google account
   */
  private GoogleSignInAccount mAccount = null;

  /**
   * Firebase authorization
   */
  private FirebaseAuth mAuth = null;

  /**
   * To be notified of device removal
   */
  private BroadcastReceiver mDevicesReceiver = null;

  /**
   * Flag to indicate if sign-out includes revocation of app access
   */
  private boolean mIsRevoke = false;

  // saved state
  /**
   * Error message related to SignIn or SignOut
   */
  private String mErrorMessage = null;
  /**
   * Saved state: {@value}
   */
  private static final String STATE_ERROR = "error";

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    mLayoutID = R.layout.activity_sign_in;

    super.onCreate(savedInstanceState);

    // Check whether we're recreating a previously destroyed instance
    if (savedInstanceState != null) {
      // Restore value of members from saved state
      restoreInstanceState(savedInstanceState);
    }

    setupButtons();

    setupDevicesBroadcastReceiver();

    mAuth = FirebaseAuth.getInstance();

    setupGoogleSignIn();

  }

  @Override
  protected void onStart() {
    super.onStart();

    LocalBroadcastManager
      .getInstance(this)
      .registerReceiver(mDevicesReceiver,
        new IntentFilter(Devices.INTENT_FILTER));
    updateView();

    if (User.INSTANCE.isLoggedIn()) {
      attemptSilentSignIn();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    LocalBroadcastManager
      .getInstance(this)
      .unregisterReceiver(mDevicesReceiver);
    dismissProgress();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Save the current state
    outState.putString(STATE_ERROR, mErrorMessage);
  }

  @Override
  protected void restoreInstanceState(Bundle savedInstanceState) {
    super.restoreInstanceState(savedInstanceState);

    mErrorMessage = savedInstanceState.getString(STATE_ERROR);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mOptionsMenuID = R.menu.menu_signin;

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean processed = true;

    final int id = item.getItemId();
    switch (id) {
      case R.id.action_help:
        showHelpDialog();
        break;
      default:
        processed = false;
        break;
    }

    return processed || super.onOptionsItemSelected(item);
  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Result returned from launching the Intent from
    // GoogleSignInApi.getSignInIntent(...);
    if (requestCode == RC_SIGN_IN) {
      final GoogleSignInResult result =
        Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      handleSignInResult(result);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implement Firebase OnCompleteListener
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void onComplete(@NonNull Task<AuthResult> task) {
    // Called after firebase sign in
    if (!task.isSuccessful()) {
      // If sign in fails, display a message to the user.
      final Exception ex = task.getException();
      Log.logEx(TAG, "firebase signin error", ex);
      assert ex != null;
      signInFailed(ex.getLocalizedMessage());
    } else {
      // success
      final FirebaseUser user = mAuth.getCurrentUser();
      if (user != null) {
        // User is signed in
        if (mAccount != null) {
          // set User
          User.INSTANCE.set(mAccount);
          updateView();

          if (!Prefs.isDeviceRegistered()) {
            // register with server
            setProgressMessage(getString(R.string.registering));
            new RegistrationClient
              .RegisterAsyncTask(this, mAccount.getIdToken())
              .executeMe();
          }
        } else {
          // something went wrong, shouldn't be here
          signInFailed(getString(R.string.sign_in_err));
        }
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implement View.OnClickListener
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.sign_in_button:
        onSignInClicked();
        break;
      case R.id.sign_out_button:
        onSignOutClicked();
        break;
      case R.id.revoke_access_button:
        onRevokeAccessClicked();
        break;
      default:
        break;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implement GoogleApiClient.OnConnectionFailedListener
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    // An unresolvable error has occurred and Google APIs
    // (including Sign-In) will not be available.
    Log.logE(TAG,
      "onConnectionFailed: " + connectionResult.getErrorMessage());
    mErrorMessage = getString(R.string.error_connection);
    dismissProgress();
  }

  ///////////////////////////////////////////////////////////////////////////
  // public methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * SignOut of Google and Firebase
   */
  public void doSignOut() {
    if (mGoogleApiClient.isConnected()) {
      showProgress(getString(R.string.signing_out));
      Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
        new ResultCallback<Status>() {
          @Override
          public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
              FirebaseAuth.getInstance().signOut();
              clearUser();
              dismissProgress();
            } else {
              signOutFailed(getString(R.string.sign_out_err_fmt,
                status.getStatusMessage()));
            }
          }
        });
    } else {
      signOutFailed(getString(R.string.sign_out_err_fmt, ""));
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // private methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Event: SignIn button clicked
   */
  private void onSignInClicked() {
    final Intent signInIntent =
      Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }

  /**
   * Event: SignOut button clicked
   */
  private void onSignOutClicked() {
    handleSigningOut(false);
  }

  /**
   * Event: Revoke access button clicked
   */
  private void onRevokeAccessClicked() {
    handleSigningOut(true);
  }

  /**
   * Try to signin with cached credentials or cross-device single sign-on
   */
  private void attemptSilentSignIn() {
    final OptionalPendingResult<GoogleSignInResult> opr =
      Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

    if (opr.isDone()) {
      // If the user's cached credentials are valid,
      // the OptionalPendingResult will be "done"
      // and the GoogleSignInResult will be available instantly.
      handleSignInResult(opr.get());
    } else {
      // If the user has not previously signed in on this device
      // or the sign-in has expired,
      // this asynchronous branch will attempt to sign in the user
      // silently.  Cross-device single sign-on will occur in this branch.
      showProgress(getString(R.string.signing_in));
      opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
        @Override
        public void onResult(@NonNull GoogleSignInResult r) {
          dismissProgress();
          handleSignInResult(r);
        }
      });
    }
  }

  /**
   * All SignIn attempts will come through here
   * @param result The {@link GoogleSignInResult} of any SignIn attempt
   */
  private void handleSignInResult(GoogleSignInResult result) {
    mErrorMessage = "";
    if (result.isSuccess()) {
      mAccount = result.getSignInAccount();
      if (!User.INSTANCE.isLoggedIn()) {
        // Authenticate with Firebase, also completes sign-in activities
        firebaseAuthWithGoogle();
      } else {
        updateView();
      }
    } else {
      // Google signIn failed
      final String error =
        getString(R.string.sign_in_err_fmt,
          result.getStatus().toString());
      signInFailed(error);
    }
  }

  /**
   * Authorize with Firebase
   */
  private void firebaseAuthWithGoogle() {
    showProgress(getString(R.string.signing_in));
    AuthCredential credential =
      GoogleAuthProvider.getCredential(mAccount.getIdToken(), null);
    mAuth.signInWithCredential(credential)
      .addOnCompleteListener(this, this);
  }

  /**
   * Handle everything related to unregistering, signing out, and revoking
   * access
   * @param revoke - if true revoke access to app
   */
  private void handleSigningOut(Boolean revoke) {
    mIsRevoke = revoke;
    if (Prefs.isDeviceRegistered()) {
      if (Prefs.isPushClipboard()) {
        // also handles unregister and sign-out
        showProgress(getString(R.string.signing_out));
        MessagingClient.sendDeviceRemoved();
      } else {
        // handles unregister and sign-out
        doUnregister();
      }
    } else {
      if (revoke) {
        doRevoke();
      } else {
        doSignOut();
      }
    }
  }

  /**
   * Show a dialog with rationale for signin
   */
  private void showHelpDialog() {
    DialogFragment dialogFragment = new HelpDialogFragment();
    dialogFragment.show(getSupportFragmentManager(), "HelpDialogFragment");
  }

  /**
   * Initialize  Buttons
   */
  private void setupButtons() {
    findViewById(R.id.sign_in_button).setOnClickListener(this);
    findViewById(R.id.sign_out_button).setOnClickListener(this);
    findViewById(R.id.revoke_access_button).setOnClickListener(this);

    final SignInButton signInButton = findViewById(R.id.sign_in_button);
    if (signInButton != null) {
      signInButton.setStyle(
        SignInButton.SIZE_WIDE,
        SignInButton.COLOR_AUTO);
    }
  }

  /**
   * Initialize  {@link Devices} {@link BroadcastReceiver}
   */
  private void setupDevicesBroadcastReceiver() {
    // handler for received Intents for the "devices" event
    // we want to know when our device is removed and unregistered
    // so we can finish logout
    mDevicesReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getBundleExtra(Devices.BUNDLE);
        final String action = bundle.getString(Devices.ACTION);

        if (action != null) {
          switch (action) {
            case Devices.ACTION_MY_DEVICE_REMOVED:
              // device remove message sent, now unregister
              setProgressMessage(getString(R.string.unregistering));
              doUnregister();
              break;
            case Devices.ACTION_MY_DEVICE_REGISTERED:
              // registered
              dismissProgress();
              break;
            case Devices.ACTION_MY_DEVICE_UNREGISTERED:
              // unregistered, now signout or revoke
              if (mIsRevoke) {
                doRevoke();
              } else {
                doSignOut();
              }
              break;
          }
        }
      }
    };
  }

  /**
   * Initialize Google SignIn
   */
  private void setupGoogleSignIn() {
    // Configure sign-in to request the user's ID, email address, and basic
    // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
    final GoogleSignInOptions gso =
      new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestIdToken(getString(R.string.default_web_client_id))
        .build();

    // Build a GoogleApiClient with access to the Google Sign-In API and the
    // options specified by gso.
    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .enableAutoManage(this, this)
      .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
      .build();
  }

  /**
   * Revoke access to app for this {@link User}
   */
  private void doRevoke() {
    if (mGoogleApiClient.isConnected()) {
      showProgress(getString(R.string.signing_out));
      Auth.GoogleSignInApi
        .revokeAccess(mGoogleApiClient)
        .setResultCallback(
          new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
              if (status.isSuccess()) {
                FirebaseAuth.getInstance().signOut();
                clearUser();
                dismissProgress();
              } else {
                signOutFailed(getString(R.string.revoke_err_fmt,
                  status.getStatusMessage()));
              }
            }
          });
    } else {
      signOutFailed(getString(R.string.revoke_err_fmt, ""));
    }
  }

  /**
   * Unregister with fcm. This will also perform the sign-out or revoke
   */
  private void doUnregister() {
    new RegistrationClient.UnregisterAsyncTask(
      SignInActivity.this).executeMe();
  }

  /**
   * Remove all {@link User} info.
   */
  private void clearUser() {
    User.INSTANCE.clear();
    updateView();
  }

  /**
   * Set the state of all the UI elements
   */
  private void updateView() {
    final View signInView = findViewById(R.id.sign_in);
    final View signOutView = findViewById(R.id.sign_out_and_disconnect);
    final TextView userNameView = findViewById(R.id.user_name);
    final TextView emailView = findViewById(R.id.email);
    final TextView errorView = findViewById(R.id.error_message);
    if (signInView == null || signOutView == null) {
      // hmm
      return;
    }

    if (User.INSTANCE.isLoggedIn()) {
      signInView.setVisibility(View.GONE);
      signOutView.setVisibility(View.VISIBLE);
      userNameView.setText(User.INSTANCE.getName());
      emailView.setVisibility(View.VISIBLE);
      emailView.setText(User.INSTANCE.getEmail());
    } else {
      signInView.setVisibility(View.VISIBLE);
      signOutView.setVisibility(View.GONE);
      userNameView.setText(getString(R.string.signed_out));
      emailView.setVisibility(View.GONE);
      emailView.setText("");
    }
    errorView.setText(mErrorMessage);
  }

  /**
   * SignIn failed for some reason
   * @param error info. on failure
   */
  private void signInFailed(String error) {
    mErrorMessage = error;
    Log.logE(TAG, mErrorMessage);
    clearUser();
    dismissProgress();
  }

  /**
   * SignOut failed for some reason
   * @param error info. on failure
   */
  private void signOutFailed(String error) {
    mErrorMessage = error;
    Log.logE(TAG, mErrorMessage);
    dismissProgress();
  }

  /**
   * Display progress
   * @param message - message to display
   */
  private void showProgress(String message) {
    final View userView = findViewById(R.id.user);
    final View progressView = findViewById(R.id.progress);

    userView.setVisibility(View.GONE);
    progressView.setVisibility(View.VISIBLE);
    setProgressMessage(message);
  }

  /**
   * Remove progress
   */
  private void dismissProgress() {
    final View userView = findViewById(R.id.user);
    final View progressView = findViewById(R.id.progress);

    userView.setVisibility(View.VISIBLE);
    progressView.setVisibility(View.GONE);
    setProgressMessage("");
  }

  /**
   * Set progress message
   * @param message - message to display
   */
  private void setProgressMessage(String message) {
    final TextView progressMessageView = findViewById(R.id.progress_message);
    progressMessageView.setText(message);
  }
}
