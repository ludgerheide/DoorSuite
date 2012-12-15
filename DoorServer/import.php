<?php
	//Verbindung mit SQL herstellen
	/* Datenbankserver - In der Regel die IP */
	$db_server = '**';
	/* Datenbankname */
	db_name = '**';
	/* Datenbankuser */
	$db_user = '**';
	/* Datenbankpasswort */
	$db_passwort = '**';
	/* Erstellt Connect zu Datenbank her */
	$db = @ mysql_connect ( $db_server, $db_user, $db_passwort );
	$db_select = @ mysql_select_db( $db_name );
?>