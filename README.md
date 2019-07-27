---
languages:
- java
page_type: sample
description: "This sample demonstrates all the normal life cycles your application should experience when using AAD."
products:
- azure
- azure-active-directory
urlFragment: integrate-aad-b2c-android
---

# Integrate Azure AD B2C into an Android App Using MSAL  

| [Library](https://github.com/AzureAD/microsoft-authentication-library-for-android) | [API Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- |

The MSAL Android preview gives your app the ability to begin using the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Azure B2C](https://azure.microsoft.com/services/active-directory-b2c/) using industry standard OAuth2 and OpenID Connect.  This sample demonstrates all the normal life cycles your application should experience, including:

* Sign in a user with Local Accounts or Social Identity Providers
* Get an Access Token for a Web Service
* Call the Web Service
* Edit the User's Profile
* Sign out the user

## Code Snapshot

```java
PublicClientApplication pApp = new PublicClientApplication(
                this.getApplicationContext(),
                Constants.CLIENT_ID,
                String.format(Constants.AUTHORITY, Constants.TENANT, Constants.SISU_POLICY));
pApp.acquireToken(getActivity(), scopes, getAuthInteractiveCallback());

// ...

authenticationResult.getAccessToken();
```

## Optional: Register you App  

The app comes pre-configured for testing.  If you would like to register your own app, please follow the steps below. 

You will need to have a native client application registered with Microsoft using the [Azure Portal](https://docs.microsoft.com/azure/active-directory-b2c/active-directory-b2c-app-registration).  Once done, update your app configs in ***Constants.Java***.

Checkout [the sample Web Service](https://github.com/Azure-Samples/active-directory-b2c-javascript-nodejs-webapi) code to run it on your own!

## Steps to Run

1. Clone the code.
    ```
    git clone https://github.com/Azure-Samples/active-directory-b2c-android-native-msal.git
    ```
2. Open Android Studio 2, and select *open an existing Android Studio project*. Find the cloned project and open *android-client*. 

3. Select *Build* > *Clean Project*. 

4. Select *Run* > *Run 'app'*. Make sure the emulator you're using has Chrome, if it doesn't follow [these steps](https://github.com/Azure-Samples/active-directory-general-docs/blob/master/AndroidEmulator.md). In Android Studio, we recommend using the Pixel image with Android 24. 

## Important Info

1. Redirect URI format: `msal<YOUR_CLIENT_ID>://auth` is strictly enforced by MSAL at the current time. 
2. For other docs on Azure AD B2C, checkout [the B2C dev guide](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-overview)

## Community Help and Support

We use [Stack Overflow](http://stackoverflow.com/questions/tagged/azure-active-directory) with the community to provide support. We highly recommend you ask your questions on Stack Overflow first and browse existing issues to see if someone has asked your question before. 

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](https://github.com/Azure-Samples/active-directory-b2c-android-native-msal/issues). 

## Contribute

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when possible. We use [semantic versioning](http://semver.org) so you can control the risk associated with updating your app. As an example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhancements but our API surface remains the same. You can always see the latest version and release notes under the Releases tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://www.microsoft.com/msrc/technical-security-notifications?rtc=1) and subscribing to Security Advisory Alerts.


