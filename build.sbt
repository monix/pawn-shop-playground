import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer

lazy val sharedSettings = Seq(
  scalaVersion       := "2.13.5",
  organization := "io.monix",
  scalacOptions ++= Seq(
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    // Turns all warnings into errors ;-)
    // Enables linter options
    "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
    "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
    "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
    "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
    "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
    "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
    "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
    "-Xlint:option-implicit", // Option.apply used implicit view
    "-Xlint:delayedinit-select" // Selecting member of DelayedInit
  )
)

lazy val root = (project in file("."))
  .settings(
    publishArtifact := false,
    name := "monix-mini-platform"
    //scalacOptions += "-Ypartial-unification"
  )
  .aggregate(dispatcher, worker, proto)

lazy val dispatcher = (project in file("dispatcher"))
  .dependsOn(proto)
  .aggregate(proto)

  .settings(
    name := "dispatcher",
    libraryDependencies ++= Dependencies.DispatcherDependencies,
    version := Version.dispatcherVersion,
    maintainer in Docker := "Pau Alarcón",
    dockerUsername in Docker := Some("paualarco"),
    dockerBaseImage in Docker := "golang:1.10-alpine3.7",
  ).settings(sharedSettings)
  .enablePlugins(DockerPlugin, JavaAppPackaging)

lazy val worker = (project in file("worker"))
  .dependsOn(proto)
  .aggregate(proto)
  .settings(
    name := "worker",
    libraryDependencies ++= Dependencies.WorkerDependencies,
    version := Version.workerVersion,
    maintainer in Docker := "Pau Alarcón",
    dockerUsername in Docker := Some("paualarco")
  ).settings(sharedSettings)
  .enablePlugins(DockerPlugin, JavaAppPackaging)


lazy val proto = (project in file("proto"))
  .settings(
    name := "proto",
     Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Dependencies.ProtobufDependencies
    //version := Version.version
  ).settings(sharedSettings)

lazy val e2e = (project in file("e2e"))
  .settings(
    name := "e2e",
    libraryDependencies ++= Dependencies.IntegrationTestDependencies,
    //version := Version.workerVersion,
    //Docker / maintainer := "Pau Alarcón",
    //Docker / dockerUsername := Some("paualarco")
  ).settings(sharedSettings)
  .enablePlugins(DockerPlugin, JavaAppPackaging)

