ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "CriticalSystemModel",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.5" % Test,
      "org.scalatest"     %% "scalatest" % "3.2.17" % Test
    )
  )