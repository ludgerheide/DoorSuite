<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>Admin-Panel</title>
		<meta http-equiv="content-type" content="text/html; charset=UTF8">
	</head>
	<body>
		<?php
			include "import.php";
		
			//Tabelle auslesen
			$result = mysql_query ("SELECT `id`,`deviceName`,`authenticated` FROM `devices` ORDER BY authenticated");
			if (!$result)
				echo 'Abfrage konnte nicht ausgeführt werden: ' . mysql_error();
			$size = mysql_num_rows ( $result );
			
			//Tabele als html darstellen
			//Ergebnistabelle ausgeben
			echo	'<table border="1">
					<tr>
					<td>id</td>
					<td>Gerätename</td>
					<td>Registrierungsstaus</td>
					<td>Registrierung ändern</td>
					</tr>';
			while ( $row = mysql_fetch_row ( $result ) ) {  
				echo '<tr>';
				echo '<td>'.$row[0].'</td>';
				echo '<td>'.$row[1].'</td>';
				echo '<td>'.$row[2].'</td>';
				//register/deRegister depending on authenticated status.
				if($row[2] == 0)
					echo "	<td><form method=\"post\" action=\"process.php\">
								<input name=\"action\" type=hidden value=\"Register\">
								<input name=\"id\" type=\"hidden\" value=\"".$row[0]."\">
								<input type=\"submit\" value=\"Registrierung bestätigen\">
							</form></td>";
				else
					echo "	<td><form method=\"post\" action=\"process.php\">
								<input name=\"action\" type=hidden value=\"Unregister\">
								<input name=\"id\" type=\"hidden\" value=\"".$row[0]."\">
								<input type=\"submit\" value=\"Registrierung löschen\">
							</form></td>";
				echo '</tr>';
			}
			echo '</table>';
		?>
	</body>
</html>
