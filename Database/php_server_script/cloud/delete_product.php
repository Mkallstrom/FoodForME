<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP POST Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_POST['name']) && isset($_POST['password']) && isset($_POST['key']) && isset($_POST['list'])) {
 
    $name = $_POST['name'];
    $key = $_POST['key'];
    $list = $_POST['list'];
	$password = $_POST['password'];
	
    // include db connect class
    require_once __DIR__ . '/db_connect_account.php';
 
    // connecting to db
    $db = new DB_CONNECT();
	
	// check account
	$result = mysql_query("SELECT password FROM account WHERE name = '$name'");
	if(!empty($result)) 
	{
		// check for empty result
		if(mysql_num_rows($result) > 0) 
		{
			$row = mysql_fetch_array($result);
			if(password_verify($password, $row["password"])){
			// mysql deleting a new row
			$result = mysql_query("DELETE FROM $list WHERE fk_name = '$name' AND product_key = '$key'");
 
			// check if row inserted or not
			if ($result) 
			{
				// successfully inserted into database
				$response["success"] = 1;
				$response["message"] = "Product successfully deleted.";
				// echoing JSON response
				echo json_encode($response);
			} 
			else 
			{
				// failed to insert row
				$response["success"] = 0;
				$response["message"] = "Product deletion unsuccessful.";
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
    $response["message"] = "Required field(s) is missing";
 
    // echoing JSON response
    echo json_encode($response);
}
?>
