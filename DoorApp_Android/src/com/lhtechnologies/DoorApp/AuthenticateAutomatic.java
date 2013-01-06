/*
 * Copyright © 2012 Ludger Heide ludger.heide@gmail.com
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details..
 */

package com.lhtechnologies.DoorApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import static com.lhtechnologies.DoorApp.CommonStuff.*;

public class AuthenticateAutomatic extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = app_preferences.edit();

        if (intent.hasExtra(ConnectivityManager.EXTRA_NETWORK_TYPE) && intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY) == ConnectivityManager.TYPE_WIFI) {
            ConnectivityManager myConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (myNetworkInfo.isConnected()) {
                if (myNetworkInfo.getExtraInfo().equals(chosenSSID)) {
                    //Load the last SSID from the prefs
                    String last_ssid = app_preferences.getString(SSIDKey, UndefinedSSID);
                    if (!last_ssid.equals(otherSSID) && !last_ssid.equals(chosenSSID)) {
                        Intent authenticateIntent = new Intent(context, AuthenticatorService.class);
                        authenticateIntent.setAction(authenticateAction);
                        context.startService(authenticateIntent);
                        editor.putString(SSIDKey, chosenSSID);
                        editor.commit();
                    }
                } else {
                    editor.putString(SSIDKey, myNetworkInfo.getExtraInfo());
                    editor.commit();
                }
            } else {
                editor.putString(SSIDKey, UndefinedSSID);
                editor.commit();
            }
        }
    }

}
