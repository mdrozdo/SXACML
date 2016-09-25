name := """sxacml-admin"""

version := "1.0-SNAPSHOT"

lazy val module = (project in file("OntoPlay"))
  .enablePlugins(PlayJava)

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .aggregate(module)
  .dependsOn(module)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

