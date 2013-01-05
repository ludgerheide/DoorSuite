package com.lhtechnologies.DoorApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.UUID;

import static com.lhtechnologies.DoorApp.CommonStuff.*;

public class Setup extends Activity {
    //UI Elements
    TextView laCode;
    EditText tfCode, tfUrl, tfDeviceName;
    RadioButton rbHaveCode, rbNoCode;
    ProgressBar progress;

    static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
    static final int activityNumber = 42;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.setup);

        laCode = (TextView) findViewById(R.id.laCode);
        tfCode = (EditText) findViewById(R.id.tfCode);
        tfUrl = (EditText) findViewById(R.id.tfUrl);
        tfDeviceName = (EditText) findViewById(R.id.tfDeviceName);
        rbHaveCode = (RadioButton) findViewById(R.id.rbHaveCode);
        rbNoCode = (RadioButton) findViewById(R.id.rbNoCode);
        progress = (ProgressBar) findViewById(R.id.progressBar);
    }

    public void continueButton(View view) {
        //Generate the required data and store it
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = app_preferences.edit();

        //UDID
        String udid = UUID.randomUUID().toString();
        editor.putString(UDIDKey, udid);
        //Secret
        String secret = getRandomString(256);
        editor.putString(SecretKey, secret);
        //URL
        String address = tfUrl.getText().toString();
        editor.putString(AddressKey, address);
        //DeviceName
        String deviceName = tfDeviceName.getText().toString();
        editor.putString(DeviceNameKey, deviceName);

        editor.commit();

        //Try to connect
        progress.setVisibility(View.VISIBLE);
        startActivityForResult(new Intent(this, Authenticator.class), activityNumber);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        progress.setVisibility(View.INVISIBLE);
        String reason = null;
        String title = null;
        if (requestCode == activityNumber) {
            if (resultCode == RESULT_OK && data.hasExtra(AuthenticatorReturnCode)) {
                String returnCode = data.getStringExtra(AuthenticatorReturnCode);
                if (returnCode.equals(ServerReturnRegistrationStarted)) {
                    title = getString(R.string.RegistrationStartedTitle);
                    reason = getString(R.string.RegistrationStartedExplanantion);
                } else {
                    title = getString(R.string.RegistrationFailedTitle);
                    reason = getString(R.string.RegistrationFailedExplanantion) + "\n" + returnCode;
                    if (data.hasExtra(AuthenticatorErrorDescription))
                        reason += "\n" + data.getStringExtra(AuthenticatorErrorDescription);
                }
            } else {
                title = getString(R.string.RegistrationFailedTitle);
                reason = getString(R.string.RegistrationFailedExplanantion) + "\n" + "RESULT_NOT_OK";
            }

        }


        //Create an alert and show it
        AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this.getApplicationContext());

        errorAlertBuilder.setTitle(title);
        errorAlertBuilder.setMessage(reason);
        errorAlertBuilder.setNeutralButton(getString(R.string.OKButtonTitle), null);

        // create alert dialog
        AlertDialog alertDialog = errorAlertBuilder.create();

        // show it
        alertDialog.show();
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
}
