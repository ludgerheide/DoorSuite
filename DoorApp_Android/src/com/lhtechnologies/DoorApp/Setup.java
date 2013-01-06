package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.*;

import java.security.SecureRandom;
import java.util.UUID;

import static com.lhtechnologies.DoorApp.CommonStuff.*;

public class Setup extends Activity {
    //UI Elements
    private TextView laCode;
    private EditText tfCode;
    private EditText tfUrl;
    private EditText tfDeviceName;
    private RadioButton rbHaveCode;
    private RadioButton rbNoCode;
    private ProgressBar progress;
    private Button buAbort;

    private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
    private ResponseReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.setup);

        //Register as broadcast receiver for the authenticator
        IntentFilter filter = new IntentFilter(AuthenticationFinishedBroadCast);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.setPriority(3);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        laCode = (TextView) findViewById(R.id.laCode);
        tfCode = (EditText) findViewById(R.id.tfCode);
        tfUrl = (EditText) findViewById(R.id.tfUrl);
        tfDeviceName = (EditText) findViewById(R.id.tfDeviceName);
        rbHaveCode = (RadioButton) findViewById(R.id.rbHaveCode);
        rbNoCode = (RadioButton) findViewById(R.id.rbNoCode);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        buAbort = (Button) findViewById(R.id.buAbortSetup);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void continueButton(View view) {
        boolean error = false;
        //Generate the required data and store it
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = app_preferences.edit();

        //UDID
        String udid = UUID.randomUUID().toString();
        editor.putString(UDIDKey, udid);
        //Secret
        String secret = UndefinedSecret;
        if (rbNoCode.isChecked()) {
            secret = getRandomString(256);
        } else {
            if (tfCode.getText().toString().length() > 0)
                secret = tfCode.getText().toString();
            else {
                error = true;
                //Create an alert and show it
                AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this);

                errorAlertBuilder.setTitle(getString(R.string.InvalidTextTitle));
                errorAlertBuilder.setMessage(getString(R.string.InvalidTextExplanantion));
                errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

                // create alert dialog
                AlertDialog alertDialog = errorAlertBuilder.create();

                // show it
                alertDialog.show();
            }
        }
        editor.putString(SecretKey, secret);
        //URL
        String address = tfUrl.getText().toString();
        editor.putString(AddressKey, address);
        //DeviceName
        String deviceName = UndefinedDeviceName;
        if (tfDeviceName.getText().toString().length() > 0)
            deviceName = tfDeviceName.getText().toString();
        else {
            error = true;
            //Create an alert and show it
            AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this);

            errorAlertBuilder.setTitle(getString(R.string.InvalidTextTitle));
            errorAlertBuilder.setMessage(getString(R.string.InvalidTextExplanantion));
            errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

            // create alert dialog
            AlertDialog alertDialog = errorAlertBuilder.create();

            // show it
            alertDialog.show();
        }
        editor.putString(DeviceNameKey, deviceName);

        if (!error) {
            editor.commit();
            //Try to connect
            progress.setVisibility(View.VISIBLE);
            buAbort.setVisibility(View.VISIBLE);
            Intent authenticateIntent = new Intent(this, AuthenticatorService.class);
            authenticateIntent.setAction(authenticateAction);
            startService(authenticateIntent);
        }
    }

    public void abort(View view) {
        Intent stopIntent = new Intent(this, AuthenticatorService.class);
        stopIntent.setAction(stopAction);
        startService(stopIntent);
        progress.setVisibility(View.INVISIBLE);
    }

    public void haveCode(View view) {
        rbHaveCode.setChecked(true);
        rbNoCode.setChecked(false);
        laCode.setVisibility(View.VISIBLE);
        tfCode.setVisibility(View.VISIBLE);
    }

    public void noCode(View view) {
        rbNoCode.setChecked(true);
        rbHaveCode.setChecked(false);
        laCode.setVisibility(View.INVISIBLE);
        tfCode.setVisibility(View.INVISIBLE);
    }


    private static String getRandomString(final int sizeOfRandomString) {
        final SecureRandom random = new SecureRandom();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            progress.setVisibility(View.INVISIBLE);
            buAbort.setVisibility(View.INVISIBLE);
            String reason;
            String title;
            boolean success = false;
            if (intent.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = intent.getStringExtra(AuthenticatorReturnCode);
                if (returnCode.equals(ServerReturnRegistrationStarted)) {
                    title = getString(R.string.RegistrationStartedTitle);
                    reason = getString(R.string.RegistrationStartedExplanantion);
                    success = true;
                } else {
                    title = getString(R.string.RegistrationFailedTitle);
                    reason = getString(R.string.RegistrationFailedExplanantion) + "\n" + returnCode;
                    if (intent.hasExtra(AuthenticatorErrorDescription))
                        reason += "\n" + intent.getStringExtra(AuthenticatorErrorDescription);
                }
            } else {
                title = getString(R.string.RegistrationFailedTitle);
                reason = getString(R.string.RegistrationFailedExplanantion) + "\n" + "RESULT_NOT_OK";
            }

            //Create an alert and show it
            AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(context);

            errorAlertBuilder.setTitle(title);
            errorAlertBuilder.setMessage(reason);
            if (success) {
                errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
            } else
                errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

            // create alert dialog
            AlertDialog alertDialog = errorAlertBuilder.create();

            // show it
            alertDialog.show();
            abortBroadcast();
        }
    }
}
