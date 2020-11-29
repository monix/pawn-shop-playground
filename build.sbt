import Dependencies._

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "io.monix",
      scalaVersion := "2.13.4",
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
  .aggregate(common)
  .dependsOn(common)

lazy val slave = (project in file("slave"))
  .settings(
    name := "monix-slave",
    libraryDependencies ++= MasterDependencies,
    version := Version.version
  ).enablePlugins(JavaAppPackaging, DockerPlugin)
  .aggregate(common)
  .dependsOn(common)

lazy val common = (project in file("common"))
  .settings(
    name := "monix-common",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
    ),
    libraryDependencies ++= CommonDependencies,
    version := Version.version
  )
