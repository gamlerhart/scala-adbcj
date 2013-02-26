import sbt._
import Keys._


object AkkaAsyncModules extends Build  {

  import Dependencies._

  lazy val buildSettings = Seq(
    organization := "info.gamlor.adbcj",
    version := "0.6-SNAPSHOT",
    scalaVersion := "2.10.0",
    publishTo := Some(Resolver.file("file",  new File( "C:\\Users\\Gamlor\\Develop\\gamlor-mvn\\snapshots" )) )
  )

  lazy val root = Project("scala-adbcj", file(".")) aggregate(scalaADBCJ)


  lazy val scalaADBCJ: Project = Project(
    id = "scala-adbcj",
    base = file("./scala-adbcj"),
    settings = defaultSettings ++ Seq(
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(scalaTest, ajdbc,ajdbcJdbcBridgeForTests,h2DBForTests,loggingBinding)
    ))


  override lazy val settings = super.settings ++ buildSettings

  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Tools-Repo" at "http://scala-tools.org/repo-releases/",
    resolvers +=  "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + "/.m2/repository",
    resolvers +=  "Gamlor-Repo" at "https://github.com/gamlerhart/gamlor-mvn/raw/master/snapshots",
    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked","-optimize"),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:deprecation"),
    // show full stack traces
    testOptions in Test += Tests.Argument("-oF")
  )


}

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1" % "test"
  val mockito = "org.mockito" % "mockito-core" % "1.9.0-rc1" % "test"


  val ajdbc = "org.adbcj" % "adbcj-api" % "0.6-SNAPSHOT" changing()
  val ajdbcJdbcBridgeForTests = "org.adbcj" % "adbcj-jdbc" % "0.6-SNAPSHOT" % "test" changing()
  val h2DBForTests = "com.h2database" % "h2" % "1.3.161" % "test"


  val ajdbcJdbcBridgeForBenchmark= "org.adbcj" % "adbcj-jdbc" % "0.6-SNAPSHOT"  changing()
  val ajdbcMySQL= "org.adbcj" % "mysql-async-driver" % "0.6-SNAPSHOT"  changing()
  val ajdbcPool= "org.adbcj" % "adbcj-connection-pool" % "0.6-SNAPSHOT"  changing()

  val loggingBinding = "org.slf4j" % "slf4j-simple" % "1.7.2" % "test"

}
