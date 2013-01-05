package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private Button buProcess;

    private Context appContext;

    private final static int timeout = 30;
    private final static int myRequestCode = 5784;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        tvStatus = (TextView) findViewById(R.id.tvAuhthenticated);
        prActivity = (ProgressBar) findViewById(R.id.prActivity);
        buProcess = (Button) findViewById(R.id.buProcess);

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

    public void process(View view) {
        //Set the UI to indicate Progress
        prActivity.setVisibility(View.VISIBLE);
        tvStatus.setText(getString(R.string.StatusAuthenticating));
        tvStatus.setTextColor(Color.YELLOW);
        buProcess.setEnabled(false);

        startActivityForResult(new Intent(this, Authenticator.class), myRequestCode);
    }

    void resetUI() {
        prActivity.setVisibility(View.INVISIBLE);
        tvStatus.setText(getString(R.string.StatusNotAuthenticated));
        tvStatus.setTextColor(Color.RED);
        buProcess.setEnabled(true);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String reason = null;
        String title = null;
        if (requestCode == myRequestCode) {
            if (resultCode == RESULT_OK && data.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = data.getStringExtra(AuthenticatorReturnCode);
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
                    tvStatus.setTextColor(Color.GREEN);
                } else {
                    title = getString(R.string.AuthenticationFailedTitle);
                    String result = data.getStringExtra(AuthenticatorReturnCode);
                    if (result.equals(ServerReturnAuthFailure))
                        reason = getString(R.string.AuthFailureExplanantion);
                    else if (result.equals(ServerReturnInternalFailure))
                        reason = getString(R.string.InternalFailureExplanantion);
                    else if (result.equals(ServerReturnRegistrationStarted))
                        reason = getString(R.string.RegistrationStartedExplanantion);
                    else if (result.equals(ServerReturnRegistrationPending))
                        reason = getString(R.string.RegistrationPendingExplanantion);
                    else if (result.equals(ClientErrorUndefined))
                        reason = getString(R.string.ClientErrorExplanantion) + "\n" + data.getStringExtra(AuthenticatorErrorDescription);
                    else if (reason.equals(ClientErrorMalformedURL))
                        reason = getString(R.string.MalformedURLExplanation);
                    else
                        reason = getString(R.string.UnknownErrorExplanation);

                    //Create an alert and show it
                    AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this.getApplicationContext());

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
                title = getString(R.string.RegistrationFailedTitle);
                reason = getString(R.string.RegistrationFailedExplanantion) + "\n" + "RESULT_NOT_OK";

                //Create an alert and show it
                AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this.getApplicationContext());

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

