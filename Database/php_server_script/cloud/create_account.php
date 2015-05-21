<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP POST Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_POST['name']) && isset($_POST['password']) && isset($_POST['indexInventory']) && isset($_POST['indexShoppingList']) && isset($_POST['indexRequirements'])) {
 
    $name = $_POST['name'];
    $password = $_POST['password'];
	$inventory = $_POST['indexInventory'];
    $shoppinglist = $_POST['indexShoppingList'];
	$requirements = $_POST['indexRequirements'];
 
    // include db connect class
    require_once __DIR__ . '/db_connect_account.php';
 
    // connecting to db
    $db = new DB_CONNECT();
	
	$hash = password_hash($password, PASSWORD_DEFAULT);
 
    // mysql inserting a new row
    $result = mysql_query("INSERT INTO account(name, password) VALUES('$name', '$hash')");
	if ($result) {
        // successfully inserted into database
		$result = mysql_query("INSERT INTO indices(name,inventory,shoppinglist,requirements) VALUES('$name', $inventory, $shoppinglist, $requirements)");
		if ($result) {
			$response["success"] = 1;
			$response["message"] = "Account created successfully.";
		}
		else{
			$response["success"] = 0;
			$response["message"] = "ERROR:Indices could not be created.";
		}
		echo json_encode($response);
    } 
	else 
	{
        // failed to insert row
        $response["success"] = 0;
        $response["message"] = "ERROR: An inventory could not be created.";
 
        // echoing JSON response
        echo json_encode($response);
    }


} else {
    // required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to create an inventory";
 
    // echoing JSON response
    echo json_encode($response);
}
?>
