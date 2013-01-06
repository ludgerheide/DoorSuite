package com.lhtechnologies.DoorApp;

final class CommonStuff {
    static final String ServerReturnInternalFailure = "FAIL";
    static final String ServerReturnAuthFailure = "SECRET_INCORRECT";
    static final String ServerReturnRegistrationPending = "REGISTRATION_PENDING";
    static final String ServerReturnRegistrationStarted = "REGISTRATION_STARTED";
    static final String ServerReturnSuccess = "SUCCESS";

    static final String ClientErrorUndefined = "ERROR";
    static final String ClientErrorMalformedURL = "MALFORMED_URL";

    static final String SecretKey = "secret";
    static final String UDIDKey = "udid";
    static final String AddressKey = "address";
    static final String DeviceNameKey = "device";
    static final String UndefinedUDID = "undefined_udid";
    static final String UndefinedSecret = "undefined_secret";
    static final String UndefinedAddress = "undefined_address";
    static final String UndefinedDeviceName = "Undefined Device Name";

    static final String FlatDoor = "FlatDoor";
    static final String FrontDoor = "FrontDoor";

    static final String AuthenticatorReturnCode = "AuthRetCod";
    static final String AuthenticatorErrorDescription = "ErrDesc";
    static final String AuthenticationFinishedBroadCast = "com.lhtechnologies.custom.intent.action.AuthFinBroad";
    static final String stopAction = "STOP";
    static final String authenticateAction = "AUTH";

    final static int timeout = 30;
    final static int buzzerTimeout = 3;
}
