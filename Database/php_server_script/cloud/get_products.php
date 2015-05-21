<?php
 
function toUtf8(&$v, $k) {
	$v = utf8_encode($v);
}

$response = array();


if (isset($_GET['name']) && isset($_GET['password']) && isset($_GET['list'])) {

     $name = $_GET['name'];
     $list = $_GET['list'];
	 $password = $_GET['password'];

	require_once __DIR__ . '/db_connect_account.php';
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
			$result = mysql_query("SELECT data, product_key FROM $list WHERE fk_name = '$name'") or die(mysql_error());
	
			if (mysql_num_rows($result) > 0) 
			{
				$response[$list] = array();
	
				while ($row = mysql_fetch_array($result)) 
				{
					// temp user array
					$product = array();
			
					// TODO: get all the product info
					$product["data"] = $row["data"];
					$product["key"] = $row["product_key"];
	
					// push single product into final response array
					array_push($response[$list], $product);
				}
				$response["success"] = 1;
				array_walk_recursive($response, 'toUtf8');
				echo json_encode($response);
			}
			else
			{
				// no products found
				$response["success"] = 0;
				$response["message"] = "No products found";
				// echo no users JSON
				array_walk_recursive($response, 'toUtf8');
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
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing to get products.";
    echo json_encode($response);
}

?>