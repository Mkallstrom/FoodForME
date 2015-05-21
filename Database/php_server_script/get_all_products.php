<?php
 
/*
 * Following code will list all the products
 */
 function toUtf8(&$v, $k) {
	$v = utf8_encode($v);
}
// array for JSON response
$response = array();
 
// include db connect class
require_once __DIR__ . '/db_connect.php';
 
// connecting to db
$db = new DB_CONNECT();
 
// get all products from products table
$result = mysql_query("SELECT * FROM barcode_to_product") or die(mysql_error());
 
// check for empty result
if (mysql_num_rows($result) > 0) {
    // looping through all results
    // products node
    $response["barcode_to_product"] = array();
 
    while ($row = mysql_fetch_array($result)) {
        // temp user array
        $product = array();
        $product["barcode"] = $row["barcode"];
        $product["product_name"] = $row["product_name"];
 
        // push single product into final response array
        array_push($response["barcode_to_product"], $product);
    }
    // success
    $response["success"] = 1;
 
    // echoing JSON response
    array_walk_recursive($response, 'toUtf8');
    echo json_encode($response);
} else {
    // no products found
    $response["success"] = 0;
    $response["message"] = "No products found";

    // echo no users JSON
    array_walk_recursive($response, 'toUtf8');
    echo json_encode($response);
}
?>