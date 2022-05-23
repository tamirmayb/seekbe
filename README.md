# Seekret - BE - Traffic Analyzer
### Author: Tamir Mayblat

This is the traffic analyzer for the Seekret BE-Challenge, developed using Java as a spring boot application.

### Prerequisite
* Java and tomcat
* Run ```mvn clean install```
* Make sure that regex.json file in the project's resources folder, it should be there by default.
* In application.properties you can set parser defaults (number of workers and paths), note that these properties are filled by default and can be changed as needed.
* Connection to Mongodb is also provided by default and can be changed if needed in application.properties.

### Using the web service
* Start the server and go to http://localhost:8080/seekbe/api/swagger-ui.html#/sb-controller
You should have access to Swagger api page which controls the web service.

### Features
You can find the following apis in the web service:

###### POST /sb/startParser - starts parsing all json files found in requests directory (```parser.sources.path```).  
* The parser creates multiple workers as defined in ```parser.workers``` property.
* Groups requests by service name using a simple regex on the uri
* Finds the service name from the uri filed and matches to regex.
* If match found saves all data to Mongodb in ```requests``` table.
* After a json file has been processed it will be moved to the backup folder regardless of any errors or issues during the process
* Note: Because there's no need for an application monitoring ability the Parser will just wait until completion, then it will exit, and the call should return 'done'...
###### GET /sb/busy - 
* retrieving N most “busy” services (busy service == with most requests to it) in descending order from db.

###### GET /sb/stats - 
* retrieving all URIs for a given service and method

###### GET /sb/delete -
* deletes all saved data for a given service (bonus 2).
  
Also, the server can respond to dynamic changes of regex.json file (bonus 1)

Please let me know if anything is missing or needs modifications. 
### Thanks!


### Need to improve!!!!
##### Naming - Unclear/wrong naming for some functions and variables
* Utils.java: bad name for function checkRegexFile that checks that a file exists, var named directory but is actually a file.
* Return values and errors:
* SBController.java: all controllers return status code 400 regardless of the error that occurred
* Returning string constants from service functions creates a weird  API
* A recurring habit of logging errors rather than rethrowing and logging them in the correct context. This creates some odd code where return values are tested against null to check whether an exception occurred.
##### Wrong use of SDKs
* ParserTask.java: redundant use of synchronizedList as the variable is used by a single thread
* ParserService.java: using sleep to wait for threads instead of other solutions
* RequestService.java: using  mongo findAll query and filters on the results in memory rather than sending the matching query to Mongo
* RequestService.java:82: using findAllAndRemove instead of remove
##### Code flow and readability
* ParserTask.java:74: using default values for checking existence rather than checking existence directly
* ParserTask.java:116: parsing JSON string just to convert it back to a string
* ParserTask.java: the run function is very crowded and difficult to read