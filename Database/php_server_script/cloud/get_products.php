@@ -1,42 +0,0 @@
<?php
 
$response = array();


if (isset($_POST['refrigerator_name']) && isset($_POST['refrigerator_password']) && isset($_POST['list'])) {
	require_once __DIR__ . '/db_connect_refrigerator.php';
	$db = new DB_CONNECT();
	
	$result = mysql_query("SELECT data, key FROM list WHERE fk_name = refrigerator_name") or die(mysql_error());
	
	if (mysql_num_rows($result) > 0) {
		$response["list"] = array();
	
		while ($row = mysql_fetch_array($result)) {
			// temp user array
			$product = array();
			
			// TODO: get all the product info
			$product["data"] = $row["data"];
			$product["key"] = $row["key"];
	
			// push single product into final response array
			array_push($response["list"], $product);
		}
		
		$response["success"] = 1;
		echo json_encode($response);
	} else {
		$response["success"] = 0;
		$response["message"] = "No products found";
	
		echo json_encode($response);
	}
	
} else {
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to connect to a refrigerator";
    echo json_encode($response);
}

?>