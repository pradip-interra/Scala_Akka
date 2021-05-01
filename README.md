# ScalaRESTAPICallWithAkka
 Scala Application with power of Akka to make REST API calls, stich to build a workflow nicely, using Akka and JSON marshalling/unmarshalling.
 
 # How it works?
 * Refer to my repository https://github.com/pradip-interra/NodeJs
 * It has a Node + Express application that exposes REST API calls on Movie object with http://localhost:300
 * In this application:
  * We have a simple Node.js + Express server running in http://localhost:3000/api/movies endpoint that
  * Supports POST, GET, PATCH, PUT, DELETE of Movie object consists of Id(int), Name(String) and Genre(String).

# How to run?
* sbt compile
* sbt run

# Sample output:

 Instance GET API Output:Movie(4,3-idiots,satire)
 
 Deleted movie: {"genre":"satire","id":4,"name":"3-idiots"}

