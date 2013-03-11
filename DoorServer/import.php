<?php
	//Verbindung mit SQL herstellen
	/* Datenbankserver - In der Regel die IP */
	$db_server = 'localhost';
	/* Datenbankname */
	$db_name = 'doorApp_db';
	/* Datenbankuser */
	$db_user = 'doorApp';
	/* Datenbankpasswort */
	$db_passwort = 'testpass';
	/* Erstellt Connect zu Datenbank her */
	$db = @ mysql_connect ( $db_server, $db_user, $db_passwort );
	$db_select = @ mysql_select_db( $db_name );
        
	$supportedVersion = 1;
?>
