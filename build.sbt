import Dependencies._

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "io.monix",
      scalaVersion := "2.12.10",
      version      := Version.version
    )),
    name := "monix-mini-platform"
  )

lazy val master = (project in file("master"))
  .settings(
    name := "monix-master",
    libraryDependencies ++= MasterDependencies,
    version := Version.version
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val slave = (project in file("slave"))
  .settings(
    name := "monix-slave",
    version := Version.version
  ).enablePlugins(JavaAppPackaging, DockerPlugin)



