--- 
Services: active-directory-b2c
platforms: Android
author: danieldobalian
level: 100
client: Android Mobile App
endpoint: AAD B2C
---
# Integrate Azure AD B2C into your Android app 

| [Getting Started](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-overview)| [Library](https://github.com/AzureAD/microsoft-authentication-library-for-android) | [API Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-support)
| --- | --- | --- | --- |

Azure AD B2C is an identity management service that enables you to customize and control how customers sign up, sign in, and manage their profiles when using your application. This code sample will walk you through how to integrate Azure AD B2C into your app using the Microsoft Authentication Library for Android (MSAL). 

This sample will show you how to, 

* Sign in or sign up a user
* Refresh their session silently
* Use tokens to securely call APIs
* Edit the user's profile
* Sign out the user

## Example Code

The MSAL library abstracts away most the complexity of Auth. Once initialized, your app can use MSAL to interactively or silently request tokens for APIs. 

```Java
// Initialize your app with MSAL
PublicClientApplication pApp = new PublicClientApplication(
            this.getApplicationContext(),
            R.raw.auth_config);

// Perform authentication requests
pApp.acquireToken(getActivity(), SCOPES, getAuthInteractiveCallback());

// ...

// Get tokens to call APIs
authenticationResult.getAccessToken();
```

## Optional: Register your App  

The app comes pre-configured for trying the sample.  If you would like to register your own app, please follow 
the steps below. 

You will need to have a native client application registered with Microsoft using the 
[Azure portal](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-app-registration). Once done, update the ***b2c_config.json*** file in the sample. 

## Steps to Run

1. Clone the code. 
    ```
    git clone https://github.com/Azure-Samples/active-directory-android-native-v2 
    ```
2. Open Android Studio 3+, and select *open an existing Android Studio project*. Find the cloned project and open it. 

3. Select *Build* > *Clean Project*. 

4. Select *Run* > *Run 'app'*. 

## Feedback, Community Help, and Support

We use [Stack Overflow](http://stackoverflow.com/questions/tagged/msal) with the community to 
provide support. We highly recommend you ask your questions on Stack Overflow first and browse 
existing issues to see if someone has asked your question before. 

If you find and bug or have a feature request, please raise the issue 
on [GitHub Issues](../../issues). 

To provide a recommendation, visit 
our [User Voice page](https://feedback.azure.com/forums/169401-azure-active-directory).

## Contribute

We enthusiastically welcome contributions and feedback. You can clone the repo and start 
contributing now. Read our [Contribution Guide](Contributing.md) for more information.

This project has adopted the 
[Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). 
For more information see 
the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact 
[opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign-in and access services. We recommend you always take the 
latest version of our library in your app when possible. We 
use [semantic versioning](http://semver.org) so you can control the risk associated with updating 
your app. As an example, always downloading the latest minor version number (e.g. x.*y*.x) ensures 
you get the latest security and feature enhanements but our API surface remains the same. You 
can always see the latest version and release notes under the Releases tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services please report it 
to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your 
submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) 
program. Please do not post security issues to GitHub Issues or any other public site. We will 
contact you shortly upon receiving the information. We encourage you to get notifications of when 
security incidents occur by 
visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing 
to Security Advisory Alerts.



