<?php

/*
 * Following code will get single product details
 */

$response = array();

require_once __DIR__ . '/db_connect.php';

// Class DB_CONNECT from db_connect.php
$db = new DB_CONNECT();

if(isset($_GET["barcode"])) {
	$barcode = $_GET['barcode'];
	$result = mysql_query("SELECT * FROM barcode_to_product WHERE barcode = $barcode");

	if(!empty($result)) {
		// check for empty result
		if(mysql_num_rows($result) > 0) {
			$result = mysql_fetch_array($result);
			$product = array();
			$product["barcode"] = $result["barcode"];
			$product["product_name"] = $result["product_name"];
			
			// success
			$response["success"] = 1;
			$response["product"] = array();
			array_push($response["product"], $product);
		} else {
		       // no product found
		       $response["success"] = 0;
		       $response["message"] = "No product found";
		}
	} else {
	       // no product found
	       $response["success"] = 0;
	       $response["message"] = "No product found";
	}
} else {
       // required field is missing
       $response["success"] = 0;
       $response["message"] = "Required field(s) is missing";
}

echo json_encode($response);
?>
