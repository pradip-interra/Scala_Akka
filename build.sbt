name := "Demo_Future_Akka_RESTcall"

version := "1.0"

scalaVersion := "2.13.5"

idePackagePrefix := Some("pradip.demo_future_akka_rest")

val akkaVersion = "2.5.26"
val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
)
