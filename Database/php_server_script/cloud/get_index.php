<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP Post Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_GET['name'])&& isset($_GET['password']) && isset($_GET['list'])) {
 
    $name = $_GET['name'];
	$list = $_GET['list'];
	$password = $_GET['password'];
 
    // include db connect class
    require_once __DIR__ . '/db_connect_account.php';
 
    // connecting to db
    $db = new DB_CONNECT();
 
 
	// check account
	$result = mysql_query("SELECT * FROM account WHERE name = '$name'");
	if(!empty($result)) 
	{
		$row = mysql_fetch_array($result);
		if(password_verify($password, $row["password"])){
		// check for empty result
		if(mysql_num_rows($result) > 0) 
		{
			$result = mysql_query("SELECT * FROM indices WHERE name = '$name'");
 
			if(!empty($result)) 
			{
				// check for empty result
				if(mysql_num_rows($result) > 0) 
				{			
					// success
					$response["success"] = 1;
					$result = mysql_fetch_array($result);
					$index = array();
					$index["index"] = $result[$list];
					$response["index"] = array();
					array_push($response["index"], $index);
				} 
				else 
				{
					// no index found
					$response["success"] = 0;
				}
			} 
			else 
			{
				// no index found
				$response["success"] = 0;
			}
			echo json_encode($response);
		}
		}
		else 
		{
		       $response["success"] = 0;
			   $response["message"] = "Account check failed.";
			   echo json_encode($response);
		}
	}
	else 
	{
	    $response["success"] = 0;
		$response["message"] = "Account check failed.";
		echo json_encode($response);
	}
} 		
else 
{
    // required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to get index.";
 
    // echoing JSON response
    echo json_encode($response);
}
?>
