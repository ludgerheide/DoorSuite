package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.security.KeyStore;
import java.util.UUID;
import java.security.SecureRandom;
import android.view.inputmethod.EditorInfo;
import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.HostnameVerifier;


public class OpenDoor extends Activity {
    //Instance variables
    public TextView tvStatus;
    public EditText tfUrl;
    public ProgressBar prActivity;
    public Button buProcess;
    public Button buAbort;

    public CountDownTimer authCountdown;
    public AuthenticationHandlerTask task;

    private String udid;
    private String secret;
    private String address;

    private final Context context = this;

    private static final String SecretKey = "secret" ;
    private static final String UDIDKey = "udid";
    private static final String AddressKey = "address";
    private static final String UndefinedUDID = "undefined_udid";
    private static final String UndefinedSecret = "undefined_secret";
    private static final String UndefinedAddress = "undefined_address";

    private static final String ServerReturnInternalFailure = "FAIL";
    private static final String ServerReturnAuthFailure = "SECRET_INCORRECT";
    private static final String ServerReturnRegistrationPending = "REGISTRATION_PENDING";
    private static final String ServerReturnRegistrationStarted = "REGISTRATION_STARTED";
    private static final String ServerReturnClientError = "ERROR";
    private static final String ServerReturnSuccess = "SUCCESS";
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";

    private final static int timeout = 30;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tvStatus =  (TextView) findViewById(R.id.tvAuhthenticated);
        tfUrl = (EditText) findViewById(R.id.tfUrl);
        prActivity = (ProgressBar) findViewById(R.id.prActivity);
        buProcess = (Button) findViewById(R.id.buProcess);
        buAbort = (Button) findViewById(R.id.buAbort);

        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        udid = app_preferences.getString(UDIDKey, UndefinedUDID);
        secret = app_preferences.getString(SecretKey, UndefinedSecret);
        address = app_preferences.getString(AddressKey, UndefinedAddress);

        //If the udid is not set,it defaults to undefined_udid
        //We then create a new one and store it
        if(udid.equals(UndefinedUDID)) {
            udid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString(UDIDKey, udid);
            editor.commit();
        }

        if(secret.equals(UndefinedSecret)) {
            secret = getRandomString(256);
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString(SecretKey, secret);
            editor.commit();
        }

        if(address.equals(UndefinedAddress)) {
            address = "http://192.168.15.12/";
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString(AddressKey, address);
            editor.commit();
        }

        tfUrl.setText(address);
    }

    @Override
    protected void onPause() {
        address = tfUrl.getText().toString();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putString(AddressKey, address);
        editor.commit();
        super.onPause();
    }

    public void process(View view) {
        //Set the UI to indicate Progress
        prActivity.setVisibility(View.VISIBLE);
        tvStatus.setText("Status: Connecting");
        tvStatus.setTextColor(Color.YELLOW);
        buProcess.setEnabled(false);
        buAbort.setVisibility(View.VISIBLE);

        URL url;

        try {
            url = new URL(tfUrl.getText().toString());
            URL[] urls = {url};
            task = new AuthenticationHandlerTask();
            task.execute(urls);
        } catch (MalformedURLException e) {
            AlertDialog.Builder malformedUrlAlertBuilder = new AlertDialog.Builder(context);

            malformedUrlAlertBuilder.setTitle("Malformed URL!");
            malformedUrlAlertBuilder.setMessage("The URL you entered is invalid! Please check the URL and try again!");
            malformedUrlAlertBuilder.setNeutralButton("OK", null);

            // create alert dialog
            AlertDialog alertDialog = malformedUrlAlertBuilder.create();

            // show it
            alertDialog.show();
        }
    }

    public void abort(View view) {
        task.cancel(true);
        resetUI();
    }

    public void resetUI() {
        prActivity.setVisibility(View.INVISIBLE);
        tvStatus.setText("Status: Not authenticated");
        tvStatus.setTextColor(Color.RED);
        buProcess.setEnabled(true);
        buAbort.setVisibility(View.INVISIBLE);
    }

    private static String getRandomString(final int sizeOfRandomString)
    {
        final SecureRandom random=new SecureRandom();
        final StringBuilder sb=new StringBuilder();
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    //Inner class for AsyncTask
    class AuthenticationHandlerTask extends AsyncTask<URL, Void, String> {
        HttpsURLConnection urlConnection;
        URL url;

        protected String doInBackground(URL... params) {
            try {
                url = params[0];

                String password = "moosemoose";
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(getResources().getAssets().open("keystore.jks"), password.toCharArray());

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

                //Write our stuff to the output stream
                String deviceName = android.os.Build.MODEL;

                out.write("deviceName=" + deviceName + "&udid=" + udid + "&secret=" + secret);
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                //Read the answer
                String decodedString = "";
                String returnString = "";
                while ((decodedString = in.readLine()) != null) {
                    returnString +=decodedString;
                }
                in.close();

                urlConnection.disconnect();
                urlConnection = null;
                url = null;
                return returnString;
            } catch(Exception e) {
                if(urlConnection != null)
                    urlConnection.disconnect();
                urlConnection = null;
                url = null;
                e.printStackTrace();
                return "ERROR";
            }
        }

        protected void onPostExecute(String result) {
            if(result.equals(ServerReturnSuccess)) {
                //Start the timer
                authCountdown = new CountDownTimer(timeout * 1000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        tvStatus.setText("Status: Authenticated - " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        resetUI();
                    }
                }.start();

                //Set the UI
                prActivity.setVisibility(View.INVISIBLE);
                tvStatus.setTextColor(Color.GREEN);
                buAbort.setVisibility(View.INVISIBLE);
            } else {
                String reason;
                if(result.equals(ServerReturnAuthFailure))
                    reason = "The server reported an authentication failure. Please contact the administrator";
                else if(result.equals(ServerReturnInternalFailure))
                    reason = "The server reported an internal failure. Please contact the administrator";
                else if(result.equals(ServerReturnRegistrationStarted))
                    reason = "Your device has been registered and is pending confirmation. If this is not the first time you see this alert, please contact the administrator";
                else if(result.equals(ServerReturnRegistrationPending))
                    reason = "Your device is still pending confirmation. Please contact the administrator";
                else if(result.equals(ServerReturnClientError))
                    reason = "The client reported an error establishing the connection. Please contact the administrator";
                else
                    reason = "Unknown failure. Please contact the administrator";

                //Create an alert and show it
                AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(context);

                errorAlertBuilder.setTitle("Authentication Failed!");
                errorAlertBuilder.setMessage(reason);
                errorAlertBuilder.setNeutralButton("OK", null);

                // create alert dialog
                AlertDialog alertDialog = errorAlertBuilder.create();

                // show it
                alertDialog.show();

                //reset the UI
                resetUI();
            }
        }
    }

}

