import sbt._

object Dependencies {

  object DependencyVersions {
    val PureConfig = "0.16.0"
    val MonixKafka = "1.0.0-RC7"
    val Monix = "3.3.0"
    val MonixConnect = "0.6.0"
    val Circe = "0.12.3"
    val Http4s = "0.21.13"

    val Log4jScala = "11.0"
    val Log4j = "2.10.0"
    val ScalaLogging = "3.9.2"

    val Scalatest = "3.2.9"
    val Scalacheck = "1.13.5"
    val Mockito = "2.18.3"
  }

  private val TestDependencies = Seq(
    "org.scalatest"             %% "scalatest"             % DependencyVersions.Scalatest).map( _ % Test)

  val DispatcherDependencies: Seq[ModuleID] = Seq(
    "io.monix"                  %% "monix"                 % DependencyVersions.Monix,
    "org.http4s"                %% "http4s-server"         % DependencyVersions.Http4s,
    "org.http4s"                %% "http4s-core"           % DependencyVersions.Http4s,
    "org.http4s"                %% "http4s-dsl"            % DependencyVersions.Http4s,
    "org.http4s"                %% "http4s-circe"          % DependencyVersions.Http4s,
    "org.http4s"                %% "http4s-blaze-server"   % DependencyVersions.Http4s,
    "io.circe"                  %% "circe-core"            % DependencyVersions.Circe,
    "io.circe"                  %% "circe-generic"         % DependencyVersions.Circe,
    "io.circe"                  %% "circe-parser"          % DependencyVersions.Circe,
    "com.typesafe.scala-logging"    %% "scala-logging" % DependencyVersions.ScalaLogging,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.github.pureconfig"     %% "pureconfig"            % DependencyVersions.PureConfig,
    "io.monix" %% "monix-bio" % "1.1.0",
    "io.monix" %% "monix-kafka-1x" % DependencyVersions.MonixKafka,
    "org.scalatest"             %% "scalatest"             % DependencyVersions.Scalatest
  ) ++ TestDependencies

  val WorkerDependencies: Seq[ModuleID] =Seq(
    "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
    "io.grpc"                   % "grpc-netty"             % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime-grpc"  % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime"       % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "io.monix"                  %% "monix"                 % DependencyVersions.Monix,
    "io.monix"                  %% "monix-mongodb"         % DependencyVersions.MonixConnect,
    "io.monix" %% "monix-redis" % DependencyVersions.MonixConnect,
    "com.typesafe.scala-logging" %% "scala-logging" % DependencyVersions.ScalaLogging,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.github.pureconfig"     %% "pureconfig"            % DependencyVersions.PureConfig,
    "io.monix" %% "monix-kafka-1x" % DependencyVersions.MonixKafka
  )

  val FeederDependencies: Seq[ModuleID] = Seq(
    "io.monix"                  %% "monix"                 % DependencyVersions.Monix,
    "io.monix"                  %% "monix-s3"         % DependencyVersions.MonixConnect,
    "io.monix" %% "monix-redis" % DependencyVersions.MonixConnect,
    "com.typesafe.scala-logging" %% "scala-logging" % DependencyVersions.ScalaLogging,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.github.pureconfig"     %% "pureconfig"            % DependencyVersions.PureConfig,
    "io.monix" %% "monix-bio" % "1.1.0",
    "io.circe"                  %% "circe-core"            % DependencyVersions.Circe,
    "io.circe"                  %% "circe-generic"         % DependencyVersions.Circe,
    "io.circe"                  %% "circe-parser"          % DependencyVersions.Circe
  )

  val ProtobufDependencies: Seq[ModuleID] =Seq(
    "io.grpc"                   % "grpc-netty"             % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime-grpc"  % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime"       % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "compilerplugin" % "0.11.3"
  )

}
