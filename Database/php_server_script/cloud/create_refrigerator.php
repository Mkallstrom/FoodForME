<?php
 
/*
 * Following code will create a new product row
 * All product details are read from HTTP Post Request
 */
 
// array for JSON response
$response = array();
 
// check for required fields
if (isset($_POST['refrigerator_name']) && isset($_POST['refrigerator_password'])) {
 
    $refrigerator_name = $_POST['refrigerator_name'];
    $refrigerator_password = $_POST['refrigerator_password'];
 
    // include db connect class
    require_once __DIR__ . '/db_connect_refrigerator.php';
 
    // connecting to db
    $db = new DB_CONNECT();
 
    // mysql inserting a new row
    $result = mysql_query("INSERT INTO refrigerator(refrigerator_name, refrigerator_password) VALUES('$refrigerator_name', '$refrigerator_password')");
 
    // check if row inserted or not
    if ($result) {
        // successfully inserted into database
        $response["success"] = 1;
        $response["message"] = "Refrigerator successfully created.";
 
        // echoing JSON response
        echo json_encode($response);
    } else {
        // failed to insert row
        $response["success"] = 0;
        $response["message"] = "ERROR: A refrigerator could not be created.";
 
        // echoing JSON response
        echo json_encode($response);
    }
} else {
    // required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to create a refrigerator";
 
    // echoing JSON response
    echo json_encode($response);
}
?>