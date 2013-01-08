<?php
	include "import.php";
	
	//Tabelle auslesen
	$result = mysql_query ("SELECT log.datetime, devices.deviceName, log.event FROM log , devices WHERE log.deviceudid=devices.udid ORDER BY datetime");
	if (!$result)
		echo 'Abfrage konnte nicht ausgeführt werden: ' . mysql_error();
	$size = mysql_num_rows ( $result );
	while ( $row = mysql_fetch_row ( $result ) ) {  
		echo $row[0];
		echo ", ";
		echo $row[1];
		echo ", ";
		echo $row[2];
		echo "\n";
	}

?>
