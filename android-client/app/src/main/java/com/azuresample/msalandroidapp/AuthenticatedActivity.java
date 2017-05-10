package com.azuresample.msalandroidapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.String;

public class AuthenticatedActivity extends AppCompatActivity {

    /* UI & Debugging Variables */
    private static final String TAG = MainActivity.class.getSimpleName();
    Button apiButton;
    Button clearCacheButton;

    /* Azure AD variables */
    AppSubClass appState;
    private AuthenticationResult authResult;
    private PublicClientApplication sampleApp;
    String[] scopes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticated);

        apiButton = (Button) findViewById(R.id.edit);
        clearCacheButton = (Button) findViewById(R.id.clearCache);

        apiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editProfile();
            }
        });

        clearCacheButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearCache();
                finish();
            }
        });

        appState = AppSubClass.getInstance();
        sampleApp = appState.getPublicClient();
        authResult = appState.getAuthResult();
        scopes = Constants.SCOPES.split("\\s+");

        /* Write the token status (whether or not we received each token) */
        this.updateTokenUI();

        /* Calls API, dump out response from UserInfo endpoint into UI */
        this.callAPI();

    }

    //
    // Core Identity methods used by MSAL
    // ==================================
    // onActivityResult() - Catches the redirect from the system browser
    // callAPI() - Calls our api with new access token
    // editProfile() - Calls b2c edit policy with this temporary authority
    // clearCache() - Clears token cache of this app
    //

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /* Use Volley to request the /me endpoint from API
     *  Sets the UI to what we get back
     */
    private void callAPI() {

        Log.d(TAG, "Starting volley request to API");

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject parameters = new JSONObject();

        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, Constants.API_URL,
                parameters,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                /* Successfully called API */
                Log.d(TAG, "Response: " + response);
                try {
                    ((TextView) findViewById(R.id.welcome)).setText("Welcome, "
                            + response.getString("name"));
                    Toast.makeText(getBaseContext(), "Response: " + response.get("name"), Toast.LENGTH_SHORT)
                            .show();
                } catch (JSONException e) {
                    Log.d(TAG, "JSONEXception Error: " + e.toString());
                }
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
                Log.d(TAG, "Token: " + authResult.getAccessToken().toString());
                headers.put("Authorization", "Bearer " + authResult.getAccessToken());
                return headers;
            }
        };

        queue.add(request);
    }

    /* Use Volley to request the /me endpoint from API
     *  Sets the UI to what we get back
     */
    private void editProfile() {
        Log.d(TAG, "Starting volley request to API");
        try {
            String authority = String.format(Constants.AUTHORITY,
                    Constants.TENANT,
                    Constants.EDIT_PROFILE_POLICY);

            User currentUser = Helpers.getUserByPolicy(
                    sampleApp.getUsers(),
                    Constants.EDIT_PROFILE_POLICY);

            sampleApp.acquireToken(
                    this,
                    Constants.SCOPES.split("\\s+"),
                    currentUser,
                    UiBehavior.SELECT_ACCOUNT,
                    null,
                    null,
                    authority,
                    getEditPolicyCallback());
        } catch(MsalClientException e) {
            /* No User */
            Log.d(TAG, "MSAL Exception Generated while getting users: " + e.toString());
        }

    }

    /* Clears a user's tokens from the cache.
     * Logically similar to "signOut" but only signs out of this app.
     */
    private void clearCache() {
        List<User> users = null;
        try {
            Log.d(TAG, "Clearing app cache");
            users = sampleApp.getUsers();

            if (users == null) {
                /* We have no users */

                Log.d(TAG, "Faield to Sign out/clear cache, no user");
            } else if (users.size() == 1) {
                /* We have 1 user */

                /* Remove from token cache */
                sampleApp.remove(users.get(0));

                Log.d(TAG, "Signed out/cleared cache");

            }
            else {
                /* We have multiple users */

                for (int i = 0; i < users.size(); i++) {
                    sampleApp.remove(users.get(i));
                }

                Log.d(TAG, "Signed out/cleared cache for multiple users");
            }

            Toast.makeText(getBaseContext(), "Signed Out!", Toast.LENGTH_SHORT)
                    .show();

        } catch (MsalClientException e) {
            /* No token in cache, proceed with normal unauthenticated app experience */
            Log.d(TAG, "MSAL Exception Generated while getting users: " + e.toString());

        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "User at this position does not exist: " + e.toString());
        }
    }

    //
    // UI & Helper methods
    // ==================================
    // Everything below is some kind of helper to update app UI or do non-essential identity tasks
    // UpdateTokenUI() - Updates UI with token in cache status
    // hasRefreshToken() - Checks if we have a refresh token in our cache
    // UpdateRefreshTokenUI() - Updates UI with RT in cache status
    // getAuthSilentCallback() -
    //

    /* Write the token status (whether or not we received each token) */
    private void updateTokenUI() {
        if (authResult != null) {
            TextView it = (TextView) findViewById(R.id.itStatus);
            TextView at = (TextView) findViewById(R.id.atStatus);

            if(authResult.getIdToken() != null) {
                it.setText(it.getText() + " " + getString(R.string.tokenPresent));
            } else {
                it.setText(it.getText() + " " + getString(R.string.noToken));
            }

            if (authResult.getAccessToken() != null) {
                at.setText(at.getText() + " " + getString(R.string.tokenPresent));
            } else {
                at.setText(at.getText() + " " + getString(R.string.noToken));
            }

            /* Only way to check if we have a refresh token is to actually refresh our tokens */
            hasRefreshToken();
        } else {
            Log.d(TAG, "No authResult, something went wrong.");
        }
    }

    /* Checks if there's a refresh token in the cache.
     * Only way to check is to refresh the tokens and catch Exception.
     * Also is used to refresh the token.
     */
    private void hasRefreshToken() {

        /* Attempt to get a user and acquireTokenSilently
         * If this fails we will do an interactive request
         */
        List<User> users = null;
        try {
            User currentUser = Helpers.getUserByPolicy(sampleApp.getUsers(), Constants.SISU_POLICY);

            if (currentUser != null) {
            /* We have 1 user */
                boolean forceRefresh = true;
                sampleApp.acquireTokenSilentAsync(
                        scopes,
                        currentUser,
                        String.format(Constants.AUTHORITY, Constants.TENANT, Constants.SISU_POLICY),
                        forceRefresh,
                        getAuthSilentCallback());
            } else {
                /* We have no user for this policy*/
                updateRefreshTokenUI(false);
            }
        } catch (MsalClientException e) {
            /* No token in cache, proceed with normal unauthenticated app experience */
            Log.d(TAG, "MSAL Exception Generated while getting users: " + e.toString());

        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "User at this position does not exist: " + e.toString());
        }
    }

    /* Write the token status (whether or not we received each token) */
    private void updateRefreshTokenUI(boolean status) {

        TextView rt = (TextView) findViewById(R.id.rtStatus);

        if (rt.getText().toString().contains(getString(R.string.noToken))
                || rt.getText().toString().contains(getString(R.string.tokenPresent))) {
            rt.setText(R.string.RT);
        }
        if (status) {
            rt.setText(rt.getText() + " " + getString(R.string.tokenPresent));
        } else {
            rt.setText(rt.getText() + " " + getString(R.string.noToken) + " or Invalid");
        }
    }

    /* Callback used in for silent acquireToken calls.
     * Used in here solely to test whether or not we have a refresh token in the cache
     */
    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token */
                updateRefreshTokenUI(true);

                /* If the token is refreshed we should refresh our data */
                callAPI();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());
                updateRefreshTokenUI(false);
                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                    assert true;

                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                    assert true;

                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                    assert true;
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
                updateRefreshTokenUI(true);
            }
        };
    }

    /* Callback used in for silent acquireToken calls.
     * Used in here solely to test whether or not we have a refresh token in the cache
     */
    private AuthenticationCallback getEditPolicyCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token */

                /* Use this method to refresh our token with new claims */
                Log.d(TAG, "Edit Profile: " + authenticationResult.getAccessToken());
                hasRefreshToken();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Edit Profile failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                    assert true;

                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                    assert true;

                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                    assert true;
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled Edit Profile.");
                Toast.makeText(getBaseContext(), getString(R.string.editFailure), Toast.LENGTH_SHORT)
                        .show();
            }
        };
    }
}
