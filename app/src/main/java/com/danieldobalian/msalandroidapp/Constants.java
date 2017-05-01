package com.danieldobalian.msalandroidapp;

/**
 * Created by dadaboli on 4/20/17.
 */

public class Constants {

    /* Azure AD b2c Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/tfp/%s/%s";
    final static String TENANT = "fabrikamb2c.onmicrosoft.com";
    final static String CLIENT_ID = "90c0fe63-bcf2-44d5-8fb7-b8bbc0b29dc6";

    final static String REDIRECT_URI =
            "msal90c0fe63-bcf2-44d5-8fb7-b8bbc0b29dc6://auth";
    final static String SCOPES = "https://fabrikamb2c.onmicrosoft.com/demoapi/demo.read";
    final static String API_URL = "https://localhost:5050/";

    final static String SISU_POLICY = "B2C_1_SUSI";
    final static String EDIT_PROFILE_POLICY = "B2C_1_edit_profile";


}
