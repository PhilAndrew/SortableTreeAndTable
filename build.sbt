organization := "com.example"

name := "SortableTableAndTree"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.2"

val korolevVersion = "0.4.2"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.+",
  "com.github.fomkin" %% "korolev-server-blaze" % korolevVersion,
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
