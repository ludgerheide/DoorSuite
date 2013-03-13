/*
 * Copyright © 2012 Ludger Heide ludger.heide@gmail.com
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details..
 */

package com.lhtechnologies.DoorApp;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;

import static com.lhtechnologies.DoorApp.CommonStuff.*;

public class AuthenticatorService extends IntentService {
    private String udid, secret, address, deviceName, clientVersion;
    private HttpsURLConnection urlConnection;

    public AuthenticatorService() {
        super("AuthenticatorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Read in the preferences
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        udid = app_preferences.getString(UDIDKey, UndefinedUDID);
        secret = app_preferences.getString(SecretKey, UndefinedSecret);
        address = app_preferences.getString(AddressKey, UndefinedAddress);
        deviceName = app_preferences.getString(DeviceNameKey, UndefinedDeviceName);

        //Read in the version from the manifest
        clientVersion = String.valueOf(getVersion());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(stopAction)) {
            if (urlConnection != null)
                urlConnection.disconnect();
            urlConnection = null;

        } else if (intent.getAction().equals(authenticateAction)) {
            //Check if we want to open the front door or flat door
            String doorToOpen = FrontDoor;
            String authCode = null;
            if (intent.hasExtra(FlatDoor)) {
                doorToOpen = FlatDoor;
                authCode = intent.getCharSequenceExtra(FlatDoor).toString();
            }

            //Now run the connection code (Hope it runs asynchronously and we do not need AsyncTask --- NOPE --YES
            urlConnection = null;
            URL url;

            //Prepare the return intent
            Intent broadcastIntent = new Intent(AuthenticationFinishedBroadCast);

            try {
                //Try to create the URL, return an error if it fails
                url = new URL(address);

                String password = "password";
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(getResources().getAssets().open("LH Technologies Root CA.bks"), password.toCharArray());

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(keyStore);

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tmf.getTrustManagers(), null);

                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setSSLSocketFactory(context.getSocketFactory());
                urlConnection.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER); //FIXME: INSECURE!!!
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");

                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());

                //Write our stuff to the output stream;
                out.write("deviceName=" + deviceName + "&udid=" + udid + "&secret=" + secret + "&clientVersion=" + clientVersion + "&doorToOpen=" + doorToOpen);
                if (doorToOpen.equals(FlatDoor)) {
                    out.write("&authCode=" + authCode);
                    //Put an extra in so the return knows we opened the flat door
                    broadcastIntent.putExtra(FlatDoor, FlatDoor);
                }

                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                //Read the answer
                String decodedString;
                String returnString = "";
                while ((decodedString = in.readLine()) != null) {
                    returnString += decodedString;
                }
                in.close();

                broadcastIntent.putExtra(AuthenticatorReturnCode, returnString);

            } catch (MalformedURLException e) {
                broadcastIntent.putExtra(AuthenticatorReturnCode, ClientErrorMalformedURL);
            } catch (Exception e) {
                broadcastIntent.putExtra(AuthenticatorReturnCode, ClientErrorUndefined);
                broadcastIntent.putExtra(AuthenticatorErrorDescription, e.getLocalizedMessage());
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                //Now send a broadcast with the result
                sendOrderedBroadcast(broadcastIntent, null);
                Log.e(this.getClass().getSimpleName(), "Send Broadcast!");
            }
        }

    }

    private int getVersion() {
        int version = -1;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e1) {
            Log.e(this.getClass().getSimpleName(), "Name not found", e1);
        }
        return version;
    }
}
