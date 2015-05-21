<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP Post Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_GET['name']) && isset($_GET['password']) && isset($_GET['list'])) {
 
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
			$result = mysql_query("UPDATE indices SET $list=$list+1 WHERE name = '$name'");
 
			if($result) 
			{
				// check for empty result
				$response["success"] = 1;
				$response["message"] = "Index successfully increased.";
 
				// echoing JSON response
				echo json_encode($response);
			}
			else 
			{
				// no inventory found
				$response["success"] = 0;
				$response["message"] = "Index increase unsuccessful.";
				// echoing JSON response
				echo json_encode($response);
			}
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
    $response["message"] = "Required field(s) is missing to increase index";
 
    // echoing JSON response
    echo json_encode($response);
}
?>
