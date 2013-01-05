package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class Authenticator extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Read in the preferences
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        String udid = app_preferences.getString(UDIDKey, UndefinedUDID);
        String secret = app_preferences.getString(SecretKey, UndefinedSecret);
        String address = app_preferences.getString(AddressKey, UndefinedAddress);
        String deviceName = app_preferences.getString(DeviceNameKey, UndefinedDeviceName);

        //Read in the version from the manifest
        String clientVersion = String.valueOf(getVersion());

        //Check if we want to open the front door or flat door
        String doorToOpen = FrontDoor;
        String authCode = null;
        if (getIntent().hasExtra(FlatDoor)) {
            doorToOpen = FlatDoor;
            authCode = getIntent().getCharSequenceExtra(FlatDoor).toString();
        }

        //Create the POST arg String
        StringBuilder postBuilder = new StringBuilder();
        postBuilder.append("deviceName=" + deviceName + "&udid=" + udid + "&secret=" + secret + "&clientVersion=" + clientVersion + "&doorToOpen=" + doorToOpen);
        if (doorToOpen.equals(FlatDoor))
            postBuilder.append("&authCode=" + authCode);
        String postArgs = postBuilder.toString();

        //Now run the connection code (Hope it runs asynchronously and we do not need AsyncTask --- NOPE
        String[] args = {address, postArgs};
        AsyncTask task = new AuthenticationHandlerTask();
        task.execute(args);


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

    //Inner class for AsyncTask
    class AuthenticationHandlerTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... strings) {
            HttpsURLConnection urlConnection = null;
            URL url = null;
            String[] returnArray = new String[2];

            try {
                //Try to create the URL, return an error if it fails
                url = new URL(strings[0]);

                String password = "password";
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(getResources().getAssets().open("LH Technologies Root CA.bks"), password.toCharArray());

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(keyStore);

                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tmf.getTrustManagers(), null);

                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setSSLSocketFactory(context.getSocketFactory());
                urlConnection.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");

                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());

                //Write our stuff to the output stream;
                out.write(strings[1]);

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                //Read the answer
                String decodedString;
                String returnString = "";
                while ((decodedString = in.readLine()) != null) {
                    returnString += decodedString;
                }
                in.close();

                //Now return our returnCode and finish
                returnArray[0] = returnString;
            } catch (MalformedURLException e) {
                returnArray[0] = ClientErrorMalformedURL;
            } catch (Exception e) {
                returnArray[0] = ClientErrorUndefined;
                returnArray[1] = e.getLocalizedMessage();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                return returnArray;
            }
        }

        protected void onPostExecute(String[] returnCodes) {
            Intent intent = getIntent();
            intent.putExtra(AuthenticatorReturnCode, returnCodes[0]);
            intent.putExtra(AuthenticatorErrorDescription, returnCodes[1]);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
