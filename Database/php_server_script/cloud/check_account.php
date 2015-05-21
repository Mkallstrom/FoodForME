<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP Post Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_GET['name']) && isset($_GET['password'])) {
 
    $name = $_GET['name'];
    $password = $_GET['password'];
 
    // include db connect class
    require_once __DIR__ . '/db_connect_account.php';
 
    // connecting to db
    $db = new DB_CONNECT();
	
    $result = mysql_query("SELECT password FROM account WHERE name = '$name'");
    if(!empty($result)) {
		// check for empty result
		if(mysql_num_rows($result) > 0) {
			$row = mysql_fetch_array($result);
			// success
			if(password_verify($password, $row["password"]))
			{
			  $response["success"] = 1;
			  $response["message"] = "Successful login.";
			}
			else
			{
				$response["success"] = 0;
				$response["message"] = "Invalid information.";
			}
			  
		} else {
		       // no product found
		       $response["success"] = 0;
			   $response["message"] = "No rows were returned";
		}
	} else {
	       // no product found
	       $response["success"] = 0;
		   $response["message"] = "Result was empty";
	}
	echo json_encode($response);
} else {
    // required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to check an account";
 
    // echoing JSON response
    echo json_encode($response);
}
?>
