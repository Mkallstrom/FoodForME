<?php

class DB_CONNECT {
	
      // Constructor
      function __construct() {
      	       $this->connect();
      }

      // destructor
      function __destruct() {
      	       $this->close();
      }

      /**
      * Function to connect with database
      */
      function connect() {
      	   require_once __DIR__ . '/db_config_root_refrigerator.php';
	       
      	   // added @ to remove warning 
	       $con = @mysql_connect(DB_SERVER, DB_USER, DB_PASSWORD) or die(mysql_error());
	       
	       $db = mysql_select_db(DB_DATABASE) or die(mysql_error());

	       return $con;
      }

      /**
      * Function to close the db connection (possibly not needed)
      * http://php.net/mysql_close
      */
      function close() {
      	       mysql_close();
      }
}