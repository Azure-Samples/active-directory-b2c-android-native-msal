package com.azuresample.msalandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.microsoft.identity.client.*;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    /* Azure AD variables */
    private PublicClientApplication sampleApp;
    private AuthenticationResult authResult;
    private String[] scopes;

    /* UI & Debugging Variables */
    private static final String TAG = MainActivity.class.getSimpleName();
    Button callApiButton;
    Button learnMoreButton;

    /* Global App State */
    AppSubClass state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        callApiButton = (Button) findViewById(R.id.callApi);
        learnMoreButton = (Button) findViewById(R.id.learnMore);

        callApiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onCallApiClicked(scopes);
            }
        });

        learnMoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                learnMore();
            }
        });

        state = (AppSubClass) getApplicationContext();
        scopes = Constants.SCOPES.split("\\s+");

        /* Initializes the app context using MSAL */
        sampleApp = state.getPublicClient();

        /* Initialize the MSAL App context */
        if (sampleApp == null) {
            sampleApp = new PublicClientApplication(
                    this.getApplicationContext(),
                    Constants.CLIENT_ID,
                    String.format(Constants.AUTHORITY, Constants.TENANT, Constants.SISU_POLICY));
            state.setPublicClient(sampleApp);

            Logger.getInstance().setExternalLogger(new ILoggerCallback() {
                @Override
                public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                    Log.d(tag, "MSAL_LOG: " + message);
                }
            });
        }
    }

    //
    // Core Identity methods used by MSAL
    // ==================================
    // onActivityResult() - handles redirect from System browser
    // onCallApiClicked() - attempts to get tokens for our api, if it succeeds calls api & updates UI
    //

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /* Use MSAL to acquireToken for the end-user
    *  Call API
    *  Pass UserInfo response data to AuthenticatedActivity
    */
    private void onCallApiClicked(String[] scopes) {

        /* Attempt to get a user and acquireTokenSilently
         * If this fails we will do an interactive request
         */
        Log.d(TAG, "Call API Clicked");

        try {
            User currentUser = Helpers.getUserByPolicy(sampleApp.getUsers(), Constants.SISU_POLICY);

            if (currentUser != null) {
                /* We have 1 user */

                sampleApp.acquireTokenSilentAsync(
                        scopes,
                        currentUser,
                        String.format(Constants.AUTHORITY, Constants.TENANT, Constants.SISU_POLICY),
                        false,
                        getAuthSilentCallback());
            } else {
                /* We have no user */
                sampleApp.acquireToken(getActivity(), scopes, getAuthInteractiveCallback());
            }
        } catch (MsalClientException e) {
            /* No token in cache, proceed with normal unauthenticated app experience */
            Log.d(TAG, "MSAL Exception Generated while getting users: " + e.toString());

        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "User at this position does not exist: " + e.toString());
        }
    }

    //
    // App callbacks for MSAL
    // ======================
    // getActivity() - returns activity so we can acquireToken within a callback
    // getAuthSilentCallback() - callback defined to handle acquireTokenSilent() case
    // getAuthInteractiveCallback() - callback defined to handle acquireToken() case
    //

    public Activity getActivity() {
        return this;
    }

    /* Callback used in for silent acquireToken calls.
     * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
     * else errors that we need to do an interactive request.
     */
    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, call api now */
                Log.d(TAG, "Successfully authenticated");
                authResult = authenticationResult;
                state.setAuthResult(authResult);

                /* Start authenticated activity */
                startAuthenticated();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                    assert true;

                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                    assert true;

                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                    sampleApp.acquireToken(getActivity(), scopes, getAuthInteractiveCallback());
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    /* Callback used for interactive request.  If succeeds we use the access
     * token to call the api. Does not check cache.
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, call api now */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getIdToken());
                authResult = authenticationResult;
                state.setAuthResult(authResult);

                /* Start authenticated activity */
                startAuthenticated();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                    assert true;
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                    assert true;

                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    //
    // Activity Helpers
    // ==================================
    // learnMore() - starts the learn more activity
    // startAuthenticated() - starts the authenticated user activity
    //

    private void learnMore() {
        startActivity(new Intent(this, LearnMoreActivity.class));
    }

    /* Starts authenticated intent */
    private void startAuthenticated() {
        startActivity(new Intent(this, AuthenticatedActivity.class));
    }
}
