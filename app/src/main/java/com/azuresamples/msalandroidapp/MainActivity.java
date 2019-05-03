package com.azuresamples.msalandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Pair;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.microsoft.identity.client.*;
import com.microsoft.identity.client.exception.*;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

public class MainActivity extends AppCompatActivity {

    /* B2C Constants */
    final static String B2C_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};

    /* These scopes are required but not used for actual tokens */
    final static String B2C_EDIT_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};
    final static String B2C_RESET_SCOPES [] = {"https://fabrikamb2c.onmicrosoft.com/helloapi/demo.read"};

    /* URL of the API we want to request data from */
    final static String API_URL = "https://fabrikamb2chello.azurewebsites.net/hello";

    final static String SISU_POLICY = "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_SUSI";
    final static String EDIT_PROFILE_POLICY = "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_edit_profile";
    final static String RESET_PASSWORD_POLICY = "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/B2C_1_reset";


    /* UI & Debugging Variables */
    private static final String TAG = MainActivity.class.getSimpleName();
    Button loginButton;
    Button editProfileButton;
    Button signOutButton;

    /* Azure AD Variables */
    private PublicClientApplication sampleApp;
    private IAuthenticationResult authResult;
    private String curDomainHint;

    private StringBuilder mLogs;


    //
    // Initiate all our UI, MSAL/B2C
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = (Button) findViewById(R.id.login);
        editProfileButton = (Button) findViewById(R.id.edit);
        signOutButton = (Button) findViewById(R.id.clearCache);

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLoginClicked();
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
        sampleApp = new PublicClientApplication(
                this.getApplicationContext(),
                R.raw.b2c_config);

        /* load in any identity provider we stored */
        curDomainHint = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext())
                .getString("idp", null);

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

        /* Attempt to get a user and get a token silently
         * If this fails we will do an interactive request
         */
        silentRequest(false);

    }

    //
    // Core Identity methods used by MSAL
    // ==================================
    // onActivityResult() - handles redirect from System browser
    // silentRequest() - tries to silently get tokens for a user
    // onLoginClicked() - attempts to get tokens for the API, if it succeeds calls the API & updates UI
    // resetPassword() - resets the user password then kicks off the login flow.
    // onEditProfileClicked() - pops dialogue for user to edit their profiel then gets fresh tokens.
    // onSignOutClicked() - Signs account out of the app & updates UI
    // callApi() - called on successful token acquisition which makes an HTTP request to our API
    //

    /* Handles the redirect from the System Browser */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    private void silentRequest(final boolean forceRefresh) {
        /* Attempt to get a user and get a token silently
         * If this fails we will do an interactive request
         */
        sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {

                if (!accounts.isEmpty()) {
                    sampleApp.acquireTokenSilentAsync(
                            B2C_SCOPES,
                            accounts.get(0),
                            SISU_POLICY,
                            forceRefresh,
                            getAuthSilentCallback());
                } else {
                    /* No accounts or >1 account */
                    Log.d(TAG, "No Accounts found");
                }
            }
        });


    }

    /* Use MSAL to acquireToken for the end-user
     * Callback will call api w/ access token & update UI
     */
    private void onLoginClicked() {
        if (curDomainHint != null) {
            List idProvider = new ArrayList<Pair<String, String>>();
            idProvider.add(new Pair<String, String>("domain_hint", curDomainHint));
            sampleApp.acquireToken(
                    getActivity(),
                    B2C_SCOPES,
                    (String) null,
                    UiBehavior.SELECT_ACCOUNT,
                    idProvider,
                    getAuthInteractiveCallback());
        } else {
            sampleApp.acquireToken(
                    getActivity(),
                    B2C_SCOPES,
                    getAuthInteractiveCallback());
        }
    }

    /* Use MSAL to reset the users password
     * Callback should call a SiSu policy I think
     */
    private void resetPassword() {
        sampleApp.acquireToken(
                getActivity(),
                B2C_RESET_SCOPES,
                RESET_PASSWORD_POLICY,
                getResetCallback());
    }

    /* Use MSAL to edit their profile
     * Callback will land them on authenticated page.
     */
    private void onEditProfileClicked() {
        List<Pair<String, String>> idProvider = null;

        if(!TextUtils.isEmpty(curDomainHint)){
            idProvider = new ArrayList<>();
            idProvider.add(new Pair<String, String>("domain_hint", curDomainHint));
        }

        sampleApp.acquireToken(
                getActivity(),
                B2C_EDIT_SCOPES,
                (String) null,
                UiBehavior.SELECT_ACCOUNT,
                idProvider,
                null,
                EDIT_PROFILE_POLICY,
                getEditCallback());

    }

    /* Clears an account's tokens from the cache.
     * Logically similar to "sign out" but only signs out of this app.
     */
    private void onSignOutClicked() {

        /* Attempt to get a account and remove their cookies from cache */
        List<IAccount> accounts = null;

        try {
            // accounts = sampleApp.getAccounts();

            sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
                @Override
                public void onAccountsLoaded(final List<IAccount> accounts) {
                    if (accounts == null) {
                        /* We have no accounts */
                        Log.d(TAG, "No Accounts found");

                    } else {
                        for (final IAccount account : accounts) {
                            sampleApp.removeAccount(
                                    account,
                                    new PublicClientApplication.AccountsRemovedCallback() {
                                        @Override
                                        public void onAccountsRemoved(Boolean isSuccess) {
                                            if (isSuccess) {
                                                /* successfully removed account */

                                            } else {
                                                /* failed to remove account */
                                            }
                                        }
                                    });
                        }
                        /* Set identity/domain provider hint to null */
                        curDomainHint = null;

                        Toast.makeText(getBaseContext(), "Signed Out", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });

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
        apiText.setText("API Response: " + apiResponse.toString());
    }

    /* Set the UI for successful token acquisition data */
    private void updateSuccessUI() {
        // hide the old buttons
        loginButton.setVisibility(View.INVISIBLE);

        // display sign out and edit buttons
        signOutButton.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.VISIBLE);
        findViewById(R.id.welcome).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.welcome)).setText(
                "Welcome!\n\n\n\nUser's unique identifier\n\n" +
                authResult.getAccount().getAccountIdentifier().getIdentifier());
        findViewById(R.id.apiData).setVisibility(View.VISIBLE);
    }

    /* Set the UI for signed out account */
    private void updateSignedOutUI() {
        loginButton.setVisibility(View.VISIBLE);

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
    // getResetCallback() - callback defined to handle the acquireToken() for reset password
    // getEditCallback() - callback defined to handle the acquireToken() for edit profile

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
            public void onSuccess(IAuthenticationResult authenticationResult) {
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
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token, call API now */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getIdToken());

                /* Store the auth result */
                authResult = authenticationResult;

                /* Set the identity provider used for login */
                try {
                    Map<String, String> claims = parseJWT(authResult.getIdToken());
                    curDomainHint = claims.get("idp");

                    /* Store value into shared preferences */
                    PreferenceManager
                            .getDefaultSharedPreferences(getBaseContext()).edit()
                            .putString("idp", curDomainHint)
                            .apply();


                } catch (ParseException e) {
                    Log.d(TAG, e.toString());
                }

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

                    /* AADB2C90118 indicates we need to invoke reset password */
                    if (exception.getMessage().toUpperCase().contains("aadb2c90118")) {
                        resetPassword();
                    }

                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    private AuthenticationCallback getResetCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                sampleApp.removeAccount(
                        authenticationResult.getAccount(),
                        new PublicClientApplication.AccountsRemovedCallback() {
                            @Override
                            public void onAccountsRemoved(Boolean isSuccess) {
                                if (isSuccess) {
                                    /* successfully removed account */
                                } else {
                                    /* failed to remove account */
                                }
                            }
                        });

                // Once user has reset password invoke the SiSu flow
                onLoginClicked();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Reset Password failed: " + exception.toString());

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

    private AuthenticationCallback getEditCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                sampleApp.removeAccount(
                        authenticationResult.getAccount(),
                        new PublicClientApplication.AccountsRemovedCallback() {
                            @Override
                            public void onAccountsRemoved(Boolean isSuccess) {
                                if (isSuccess) {
                                    /* successfully removed account */
                                } else {
                                    /* failed to remove account */
                                }
                            }
                        });

                /* Once the user has edited their profile, need to get updated claims via SiSu. */
                onLoginClicked();

            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Edit Profile failed: " + exception.toString());

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

    /* Accepts an ID token and returns set of claims */
    public static Map<String, String> parseJWT(final String idToken) throws ParseException {
        final JWTClaimsSet claimsSet;
        final JWT jwt = JWTParser.parse(idToken);
        claimsSet = jwt.getJWTClaimsSet();
        final Map<String, Object> claimsMap = claimsSet.getClaims();
        final Map<String, String> claimsMapStr = new HashMap<>();

        for (final Map.Entry<String, Object> entry : claimsMap.entrySet()) {
            claimsMapStr.put(entry.getKey(), entry.getValue().toString());
        }

        return claimsMapStr;

    }
}
