import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "io.monix",
      scalaVersion := "2.12.13",
      version := Version.version
    )),
    publishArtifact := false,
    name := "monix-mini-platform"
  ).enablePlugins(DockerPlugin, JavaAppPackaging)

lazy val dispatcher = (project in file("dispatcher"))
  .settings(
    name := "dispatcher",
    libraryDependencies ++= Dependencies.DispatcherDependencies,
    version := Version.dispatcherVersion,
    maintainer in Docker := "Pau Alarcón",
    dockerUsername in Docker := Some("paualarco"),
    dockerBaseImage in Docker := "golang:1.10-alpine3.7"
  )
  .aggregate(common)
  .dependsOn(common)
  .enablePlugins(DockerPlugin, JavaAppPackaging)

lazy val worker = (project in file("worker"))
  .settings(
    name := "worker",
    libraryDependencies ++= Dependencies.DispatcherDependencies,
    version := Version.workerVersion,
    maintainer in Docker := "Pau Alarcón",
    dockerUsername in Docker := Some("paualarco"),
  )
  .aggregate(common)
  .dependsOn(common)
  .enablePlugins(DockerPlugin, JavaAppPackaging)

lazy val feeder = (project in file("feeder"))
  .settings(
    name := "feeder",
    libraryDependencies ++= Dependencies.DispatcherDependencies,
    version := Version.version,
    maintainer in Docker := "Pau Alarcón",
    dockerUsername in Docker := Some("paualarco"),
  )

lazy val common = (project in file("common"))
  .settings(
    name := "common",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
    ),
    libraryDependencies ++= Dependencies.DispatcherDependencies,
    version := Version.version
  )
