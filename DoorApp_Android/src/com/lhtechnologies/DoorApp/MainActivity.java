package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.lhtechnologies.DoorApp.CommonStuff.*;


public class MainActivity extends Activity {
    //Instance variables
    private TextView tvStatus;
    private ProgressBar prActivity;
    private Button buProcess, buAbort;

    private Context appContext;

    private final static int timeout = 30;
    private ResponseReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Register as broadcast receiver for the authenticator
        IntentFilter filter = new IntentFilter(AuthenticationFinishedBroadCast);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        setContentView(R.layout.main);
        tvStatus = (TextView) findViewById(R.id.tvAuhthenticated);
        prActivity = (ProgressBar) findViewById(R.id.prActivity);
        buProcess = (Button) findViewById(R.id.buProcess);
        buAbort = (Button) findViewById(R.id.buAbort);

        appContext = this.getApplicationContext();

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
    }

    public void process(View view) {
        //Set the UI to indicate Progress
        prActivity.setVisibility(View.VISIBLE);
        buAbort.setVisibility(View.VISIBLE);
        tvStatus.setText(getString(R.string.StatusAuthenticating));
        tvStatus.setTextColor(Color.YELLOW);
        buProcess.setEnabled(false);

        Intent authenticateIntent = new Intent(this, AuthenticatorService.class);
        authenticateIntent.setAction(authenticateAction);
        startService(authenticateIntent);
    }

    public void abort(View view) {
        Intent stopIntent = new Intent(this, AuthenticatorService.class);
        stopIntent.setAction(stopAction);
        startService(stopIntent);
        resetUI();
    }

    void resetUI() {
        prActivity.setVisibility(View.INVISIBLE);
        buAbort.setVisibility(View.INVISIBLE);
        tvStatus.setText(getString(R.string.StatusNotAuthenticated));
        tvStatus.setTextColor(Color.RED);
        buProcess.setEnabled(true);
    }

    public class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String reason;
            String title;
            if (intent.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = intent.getStringExtra(AuthenticatorReturnCode);
                if (returnCode.equals(ServerReturnSuccess)) {
                    //Authentication was a success, do the honors

                    //Start the timer
                    CountDownTimer authCountdown = new CountDownTimer(timeout * 1000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            tvStatus.setText(getString(R.string.StatusAuthenticated) + " – " + millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            resetUI();
                        }
                    }.start();

                    //Set the UI
                    prActivity.setVisibility(View.INVISIBLE);
                    buAbort.setVisibility(View.INVISIBLE);
                    buProcess.setEnabled(false);
                    tvStatus.setTextColor(Color.GREEN);
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
                        reason = getString(R.string.UnknownErrorExplanation);

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
        }
    }
}

