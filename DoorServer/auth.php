<?php

include "serial/php_serial.class.php";

//Variablen auslesen
$udid = $_POST["udid"];
$hashedSecret = hash("sha256", $_POST["secret"]);
$deviceName = $_POST["deviceName"];
$clientVersion = $_POST["clientVersion"];
$doorToOpen = $_POST["doorToOpen"];
if($doorToOpen == "FlatDoor")
    $authCode = $_POST["authCode"];
else
    $authCode = "NOAUTH";

function tryAuth($udid, $hashedSecret, $deviceName, $clientVersion, $doorToOpen, $authCode) {
    include "import.php";
    
    //Versionscheck
    if($clientVersion != $supportedVersion)
        return "UNSUPPORTED_VERSION";
    //Tabelle auslesen
    $result = mysql_query("SELECT * FROM devices WHERE udid=\"" . $udid . "\" ORDER BY id");
    if (!$result)
        echo 'Abfrage konnte nicht ausgeführt werden: ' . mysql_error();
    $size = mysql_num_rows($result);

    if ($size == 0) {
        //If size is zero, the device is not yet registered
        //Add it to the database (sanity check first)
        if ($udid != '') {
            $insert_query = "INSERT INTO devices (deviceName, udid, hashedSecret, authenticated) VALUES ( \"" . $deviceName . "\", \"" . $udid . "\", \"" . $hashedSecret . "\", 0)";
            if (mysql_query($insert_query))
                return "REGISTRATION_STARTED";
            else
                echo 'Abfrage (registrieren) konnte nicht ausgeführt werden: ' . mysql_error();
        } else
            return "FAIL";
    } else {
        //The device is listed, check whether it is authenticated
        $row = mysql_fetch_row($result);
        $isRegistered = $row[4];
        $storedSecret = $row[3];

        if ($isRegistered == 1 && $hashedSecret == $storedSecret) {
            //Open the door
            //Setup
            $serial = new phpSerial;

            // Let's start the class$serial = new phpSerial;
            // First we must specify the device. This works on both linux and windows (if
            // your linux serial device is /dev/ttyS0 for COM1, etc)
            $serial->deviceSet("/dev/ttyAMA0");
            // We can change the baud rate, parity, length, stop bits, flow control
            $serial->confBaudRate(9600);
            $serial->confParity("none");
            $serial->confCharacterLength(8);
            $serial->confStopBits(1);
            $serial->confFlowControl("none");
            // Then we need to open it
            $serial->deviceOpen(); // To write into
            $serial->serialflush();

            //Now check which door we want top open and send the correct message
            if ($doorToOpen == "FrontDoor") {
                $serial->sendMessage("a");
                //Read out the stuff
                $read = $serial->readPort();
                //DEBUG
                //echo $read;
                // If you want to change the configuration, the device must be closed
                $serial->deviceClose();

                if (substr_compare($read, "AuthOn serial", 0, 13) == 0)
                    return "SUCCESS";
                else
                    return "FAIL";
            } else if ($doorToOpen == "FlatDoor") {
                $serial->sendMessage("F:" + $authCode);
                //Read out the stuff
                $read = $serial->readPort();
                //DEBUG
                //echo $read;
                // If you want to change the configuration, the device must be closed
                $serial->deviceClose();

                if (substr_compare($read, "AuthOn FlatDoor", 0, 13) == 0)
                    return "SUCCESS";
                else
                    return "FAIL";
            }
        } else if ($isRegistered == 0)
            return "REGISTRATION_PENDING";
        else
            return "SECRET_INCORRECT";
    }
}

$return = tryAuth($udid, $hashedSecret, $deviceName, $clientVersion, $doorToOpen, $authCode);

$log_query = "INSERT INTO log (datetime, deviceudid, event) VALUES (\"" . date('Y/m/d H:i:s', time()) . "\", \"" . $udid . "\", \"" . $return . "\")";
if (!mysql_query($log_query))
    echo 'Abfrage (loggen) konnte nicht ausgeführt werden: ' . mysql_error();

echo $return;
?>