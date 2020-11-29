import sbt._

object Dependencies {

  object DependencyVersions {
    val PureConfig = "0.14.0"
    val Monix = "3.3.0"
    val Circe = "0.12.3"
    val Http4s = "0.21.13"

    val Log4jScala = "11.0"
    val Log4j = "2.10.0"
    val ScalaLogging = "3.9.2"

    val Scalatest = "3.0.4"
    val Scalacheck = "1.13.5"
    val Mockito = "2.18.3"
  }

  private val TestDependencies = Seq(
    "org.scalatest"             %% "scalatest"             % DependencyVersions.Scalatest,
    "org.scalacheck"            %% "scalacheck"            % DependencyVersions.Scalacheck
  ).map( _ % Test)

  val MasterDependencies: Seq[ModuleID] =Seq(
    "io.grpc"                   % "grpc-netty"             % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime-grpc"  % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime"       % scalapb.compiler.Version.scalapbVersion % "protobuf",
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
    "com.github.pureconfig"     %% "pureconfig"            % DependencyVersions.PureConfig
  )


  val CommonDependencies: Seq[ModuleID] =Seq(
    "io.grpc"                   % "grpc-netty"             % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime-grpc"  % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb"      %% "scalapb-runtime"       % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )

}
