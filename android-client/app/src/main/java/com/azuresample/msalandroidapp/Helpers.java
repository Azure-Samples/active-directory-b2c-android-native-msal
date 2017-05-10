package com.azuresample.msalandroidapp;

import android.util.Base64;

import com.microsoft.identity.client.User;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by danieldobalian on 5/9/17.
 */

public class Helpers {
    public static User getUserByPolicy(List<User> users, String policy) {
        for (int i = 0; i < users.size(); i++) {
            User curUser = users.get(i);
            String userIdentifier = Base64UrlDecode(curUser.getUserIdentifier().split("\\.")[0]);
            if (userIdentifier.contains(policy.toLowerCase())) {
                return curUser;
            }
        }

        return (User) null;
    }

    private static String Base64UrlDecode(String s) {
        byte[] data = Base64.decode(s, Base64.DEFAULT | Base64.URL_SAFE);
        String output = "";
        try {
            output = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            return output;
        }
    }


}
