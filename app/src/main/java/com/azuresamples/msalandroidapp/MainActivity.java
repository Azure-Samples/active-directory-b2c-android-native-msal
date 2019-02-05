package com.azuresamples.msalandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.microsoft.identity.client.*;
import com.microsoft.identity.client.exception.*;

public class MainActivity extends AppCompatActivity {

    /* B2C Constants */
    final static String B2C_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};

    // These scopes are required but not used for actual tokens
    final static String B2C_EDIT_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};
    final static String B2C_RESET_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};

    final static String API_URL = "https://fabrikamb2chello.azurewebsites.net/hello";

    final static String SISU_POLICY = "https://login.microsoftonline.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_SUSI";
    final static String EDIT_PROFILE_POLICY = "https://login.microsoftonline.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_edit_profile";
    final static String RESET_PASSWORD_POLICY = "https://login.microsoftonline.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_reset";

    /* Azure AD v2 Configs */
//    final static String SCOPES [] = {"https://graph.microsoft.com/User.Read"};
//    final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    /* UI & Debugging Variables */
    private static final String TAG = MainActivity.class.getSimpleName();
    Button linkAccountButton;
    Button resetPasswordButton;
    Button editProfileButton;
    Button signOutButton;

    /* Azure AD Variables */
    private PublicClientApplication sampleApp;
    private AuthenticationResult authResult;

    private StringBuilder mLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linkAccountButton = (Button) findViewById(R.id.linkAccount);
        resetPasswordButton = (Button) findViewById(R.id.resetPassword);
        editProfileButton = (Button) findViewById(R.id.edit);
        signOutButton = (Button) findViewById(R.id.clearCache);

        linkAccountButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLinkAccountClicked();
            }
        });

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onResetPasswordClicked();
            }
        });

        editProfileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditProfileClicked();
            }
        });

        signOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSignOutClicked();
            }
        });

        /* Configure your sample app and save state for this activity */
        sampleApp = null;
        if (sampleApp == null) {
            sampleApp = new PublicClientApplication(
                    this.getApplicationContext(),
                    R.raw.b2c_config);
        }

        /* Enable logging */
        mLogs = new StringBuilder();
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.getInstance().setEnablePII(true);
        Logger.getInstance().setEnableLogcatLog(true);
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                mLogs.append(message).append('\n');
            }
        });



        /* Attempt to get a user and acquireTokenSilent
         * If this fails we do an interactive request
         */
        List<IAccount> accounts = null;

        try {
            accounts = sampleApp.getAccounts();

            if (accounts != null && accounts.size() == 1) {
                /* We have 1 account */

                sampleApp.acquireTokenSilentAsync(B2C_SCOPES, accounts.get(0), getAuthSilentCallback());
            } else {
                /* We have no account or >1 account */
            }
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "Account at this position does not exist: " + e.toString());
        }

    }

    //
    // Core Identity methods used by MSAL
    // ==================================
    // onActivityResult() - handles redirect from System browser
    // onLinkAccountClicked() - attempts to get tokens for the API, if it succeeds calls the API & updates UI
    // onSignOutClicked() - Signs account out of the app & updates UI
    // callApi() - called on successful token acquisition which makes an HTTP request to our API
    //

    /* Handles the redirect from the System Browser */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /* Use MSAL to acquireToken for the end-user
     * Callback will call api w/ access token & update UI
     */
    private void onLinkAccountClicked() {
        sampleApp.acquireToken(getActivity(), B2C_SCOPES, getAuthInteractiveCallback());
    }

    /* Use MSAL to reset the users password
     * Callback should call a SiSu policy I think
     */
    private void onResetPasswordClicked() {
        // TODO: Acquire token for reset password. Probably need new scopes, policy, and new callback
    }

    /* Use MSAL to edit their profile
     * Callback should land them on auth'd page.
     */
    private void onEditProfileClicked() {
        // TODO: Acquire token for reset password. Probably need new scopes, policy, and new callback
    }

    /* Clears an account's tokens from the cache.
     * Logically similar to "sign out" but only signs out of this app.
     */
    private void onSignOutClicked() {

        /* Attempt to get a account and remove their cookies from cache */
        List<IAccount> accounts = null;

        try {
            accounts = sampleApp.getAccounts();

            if (accounts == null) {
                /* We have no accounts */

            } else if (accounts.size() == 1) {
                /* We have 1 account */
                /* Remove from token cache */
                sampleApp.removeAccount(accounts.get(0));
                updateSignedOutUI();

            }
            else {
                /* We have multiple accounts */
                for (int i = 0; i < accounts.size(); i++) {
                    sampleApp.removeAccount(accounts.get(i));
                }
            }

            Toast.makeText(getBaseContext(), "Signed Out!", Toast.LENGTH_SHORT)
                    .show();

        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "User at this position does not exist: " + e.toString());
        }
    }

    /* Use Volley to make an HTTP request to perform our API call using an access token */
    private void callApi() {
        Log.d(TAG, "Starting volley request to our API");

        /* Make sure we have a token to send to the API */
        if (authResult.getAccessToken() == null) {return;}

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject parameters = new JSONObject();

        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL,
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                /* Successfully called API, process data and send to UI */
                Log.d(TAG, "Response: " + response.toString());

                updateApiUI(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authResult.getAccessToken());
                return headers;
            }
        };

        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    //
    // Helper methods manage UI updates
    // ================================
    // updateApiUI() - Sets API response in UI
    // updateSuccessUI() - Updates UI when token acquisition succeeds
    // updateSignedOutUI() - Updates UI when app sign out succeeds
    //

    /* Sets the API response */
    private void updateApiUI(JSONObject apiResponse) {
        TextView apiText = (TextView) findViewById(R.id.apiData);
        apiText.setText(apiResponse.toString());
    }

    /* Set the UI for successful token acquisition data */
    private void updateSuccessUI() {
        // hide the old buttons
        linkAccountButton.setVisibility(View.INVISIBLE);
        resetPasswordButton.setVisibility(View.INVISIBLE);

        // display sign out and edit buttons
        signOutButton.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.VISIBLE);
        findViewById(R.id.welcome).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.welcome)).setText("Welcome! \n\nYour unique identifier is: " +
                authResult.getAccount().getAccountIdentifier());
        findViewById(R.id.apiData).setVisibility(View.VISIBLE);
    }

    /* Set the UI for signed out account */
    private void updateSignedOutUI() {
        linkAccountButton.setVisibility(View.VISIBLE);
        resetPasswordButton.setVisibility(View.VISIBLE);

        editProfileButton.setVisibility(View.INVISIBLE);
        signOutButton.setVisibility(View.INVISIBLE);
        findViewById(R.id.welcome).setVisibility(View.INVISIBLE);
        findViewById(R.id.apiData).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.apiData)).setText("No Data");
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
                /* Successfully got a token, call API now */
                Log.d(TAG, "Successfully authenticated");

                /* Store the authResult */
                authResult = authenticationResult;

                /* call API */
                callApi();

                /* update the UI to post call API state */
                updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
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
     * token to call the API. Does not check cache
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, call API now */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getIdToken());

                /* Store the auth result */
                authResult = authenticationResult;

                /* call API */
                callApi();

                /* update the UI to post call API state */
                updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }
}
