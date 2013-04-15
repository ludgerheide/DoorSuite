/*
 * Copyright © 2012 Ludger Heide ludger.heide@gmail.com
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details..
 */

package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.lhtechnologies.DoorApp.CommonStuff.*;


public class MainActivity extends Activity {
    //Instance variables
    private TextView tvStatus, tvFlatDoor;
    private ProgressBar prActivity;
    private Button buFrontDoor, buFlatDoor, buAbort;
    private EditText tfDoorCode;

    private ResponseReceiver receiver;
    private CountDownTimer flatDoorTimer, frontDoorTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        tvStatus = (TextView) findViewById(R.id.tvAuhthenticated);
        tvFlatDoor = (TextView) findViewById(R.id.tvFlatDoor);
        prActivity = (ProgressBar) findViewById(R.id.prActivity);
        buFrontDoor = (Button) findViewById(R.id.buFrontDoor);
        buFlatDoor = (Button) findViewById(R.id.buFlatDoor);
        buAbort = (Button) findViewById(R.id.buAbortMain);
        tfDoorCode = (EditText) findViewById(R.id.tfDoorCode);

        Context appContext = this.getApplicationContext();

        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        String udid = app_preferences.getString(UDIDKey, UndefinedUDID);
        String secret = app_preferences.getString(SecretKey, UndefinedSecret);
        String address = app_preferences.getString(AddressKey, UndefinedAddress);
        String deviceName = app_preferences.getString(DeviceNameKey, UndefinedDeviceName);

        //If the udid is not set,it defaults to undefined_udid
        //We then start the setup screen
        if (udid.equals(UndefinedUDID) || secret.equals(UndefinedSecret) || address.equals(UndefinedAddress) || deviceName.equals(UndefinedDeviceName)) {
            startActivity(new Intent(this, Setup.class));
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        //resetUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Register as broadcast receiver for the authenticator
        IntentFilter filter = new IntentFilter(AuthenticationFinishedBroadCast);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.setPriority(2);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(FailureNotificationId);
    }

    public void process(View view) {
        //Set the UI to indicate Progress
        prActivity.setVisibility(View.VISIBLE);
        buAbort.setVisibility(View.VISIBLE);

        Intent authenticateIntent = new Intent(this, AuthenticatorService.class);
        authenticateIntent.setAction(authenticateAction);
        if (view == findViewById(R.id.buFlatDoor)) {
            String authCode = tfDoorCode.getText().toString();
            if (authCode.matches("[0-9]+") && authCode.length() == 4) {
                authenticateIntent.putExtra(FlatDoor, authCode);

                buFlatDoor.setEnabled(false);
                tvFlatDoor.setText(getString(R.string.StatusAuthenticating));
                tvFlatDoor.setTextColor(Color.YELLOW);
                startService(authenticateIntent);
            } else {
                //Create an alert and show it
                AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this);

                errorAlertBuilder.setTitle(getString(R.string.InvalidTextTitle));
                errorAlertBuilder.setMessage(getString(R.string.InvalidTextExplanantion));
                errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

                // create alert dialog
                AlertDialog alertDialog = errorAlertBuilder.create();

                // show it
                alertDialog.show();
                resetUI();
            }
        } else {
            buFrontDoor.setEnabled(false);
            tvStatus.setText(getString(R.string.StatusAuthenticating));
            tvStatus.setTextColor(Color.YELLOW);
            startService(authenticateIntent);
        }

    }

    public void abort(View view) {
        Intent stopIntent = new Intent(this, AuthenticatorService.class);
        stopIntent.setAction(stopAction);
        stopService(stopIntent);
        resetUI();
    }

    void resetUI() {
        prActivity.setVisibility(View.INVISIBLE);
        buAbort.setVisibility(View.INVISIBLE);
        tvStatus.setText(getString(R.string.StatusNotAuthenticated));
        tvStatus.setTextColor(Color.RED);
        tvFlatDoor.setText(getString(R.string.StatusNotAuthenticated));
        tvFlatDoor.setTextColor(Color.RED);
        buFrontDoor.setEnabled(true);
        buFlatDoor.setEnabled(true);
        if (flatDoorTimer != null)
            flatDoorTimer.cancel();
        if (frontDoorTimer != null)
            frontDoorTimer.cancel();
    }

    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String reason;
            String title;
            if (intent.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = intent.getStringExtra(AuthenticatorReturnCode);
                if (returnCode.equals(ServerReturnSuccess)) {
                    prActivity.setVisibility(View.INVISIBLE);
                    buAbort.setVisibility(View.INVISIBLE);
                    if (intent.hasExtra(FlatDoor)) {
                        //We have opened the flat door, therefore deauthenticated, therefore we don't need the front door timer anymore
                        frontDoorTimer.onFinish();
                        frontDoorTimer.cancel();
                        //Start the timer
                        flatDoorTimer = new CountDownTimer(buzzerTimeout * 1000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                tvFlatDoor.setText(getString(R.string.StatusBuzzing) + " – " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                buFlatDoor.setEnabled(true);
                                tvFlatDoor.setText(getString(R.string.StatusNotAuthenticated));
                                tvFlatDoor.setTextColor(Color.RED);
                            }
                        }.start();

                        buFlatDoor.setEnabled(false);
                        tvFlatDoor.setTextColor(Color.GREEN);

                    } else {
                        //Authentication was a success, do the honors

                        //Start the timer
                        frontDoorTimer = new CountDownTimer(timeout * 1000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                tvStatus.setText(getString(R.string.StatusAuthenticated) + " – " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                buFrontDoor.setEnabled(true);
                                tvStatus.setText(getString(R.string.StatusNotAuthenticated));
                                tvStatus.setTextColor(Color.RED);
                            }
                        }.start();

                        //Set the UI
                        buFrontDoor.setEnabled(false);
                        tvStatus.setTextColor(Color.GREEN);
                    }
                } else {
                    title = getString(R.string.AuthenticationFailedTitle);
                    String result = intent.getStringExtra(AuthenticatorReturnCode);
                    if (result.equals(ServerReturnAuthFailure))
                        reason = getString(R.string.AuthFailureExplanantion);
                    else if (result.equals(ServerReturnInternalFailure))
                        reason = getString(R.string.InternalFailureExplanantion);
                    else if (result.equals(ServerReturnRegistrationStarted))
                        reason = getString(R.string.RegistrationStartedExplanantion);
                    else if (result.equals(ServerReturnRegistrationPending))
                        reason = getString(R.string.RegistrationPendingExplanantion);
                    else if (result.equals(ClientErrorUndefined))
                        reason = getString(R.string.ClientErrorExplanantion) + "\n" + intent.getStringExtra(AuthenticatorErrorDescription);
                    else if (result.equals(ClientErrorMalformedURL))
                        reason = getString(R.string.MalformedURLExplanation);
                    else
                        reason = getString(R.string.UnknownErrorExplanation) + result;

                    //Create an alert and show it
                    AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(context);

                    errorAlertBuilder.setTitle(title);
                    errorAlertBuilder.setMessage(reason);
                    errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

                    // create alert dialog
                    AlertDialog alertDialog = errorAlertBuilder.create();

                    // show it
                    alertDialog.show();
                    resetUI();
                }
            } else {
                title = getString(R.string.AuthenticationFailedTitle);
                reason = getString(R.string.AuthFailureExplanantion) + "\n" + "NO_EXTRA";

                //Create an alert and show it
                AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(context);

                errorAlertBuilder.setTitle(title);
                errorAlertBuilder.setMessage(reason);
                errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

                // create alert dialog
                AlertDialog alertDialog = errorAlertBuilder.create();

                // show it
                alertDialog.show();
                resetUI();
            }
            abortBroadcast();
        }
    }
}

