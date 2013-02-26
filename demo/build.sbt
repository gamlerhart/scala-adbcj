name := "Scala ADBCJ Demo"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies += "info.gamlor.adbcj" %% "scala-adbcj" % "0.6-SNAPSHOT"

libraryDependencies += "org.adbcj" % "adbcj-connection-pool" % "0.6-SNAPSHOT"

libraryDependencies += "org.adbcj" % "mysql-async-driver" % "0.6-SNAPSHOT"


resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Gamlor-Repo" at "https://raw.github.com/gamlerhart/gamlor-mvn/master/snapshots"
