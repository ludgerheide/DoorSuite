<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>Redirect</title>
		<meta http-equiv="content-type" content="text/html; charset=UTF8">
		<meta http-equiv="refresh" content="5; URL=admin.php">
	</head>
	<body>
		<?php
			//Variablen initialisieren
			$action = $_POST["action"];
			$id = $_POST["id"];
			
			include "import.php";
			
			if($action == "Register" && $id !='') {
				$update_query="UPDATE devices SET authenticated=1 WHERE id= '$id'";
				if (mysql_query($update_query)) 
					echo 'Gerät erfolgreich registriert. ';
				else 
					echo 'Abfrage (registrieren) konnte nicht ausgeführt werden: ' . mysql_error();				
			} else if($action == "Delete" && $id !='') {
			$update_query="DELETE FROM devices WHERE id= '$id'";
				if (mysql_query($update_query)) 
					echo 'Gerät erfolgreich gelöscht. ';
				else 
					echo 'Abfrage (löschen) konnte nicht ausgeführt werden: ' . mysql_error();				
			} else {
				echo 'Unbekannter Fehler. ';
			}
		?>
		Bitte Warten sie fünf Sekunden oder klicken sie <a href=admin.php>hier</a>
	</body>
</html>
