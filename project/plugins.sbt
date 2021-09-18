addSbtPlugin("org.scalariform"      % "sbt-scalariform"     % "1.8.2")
addSbtPlugin("com.typesafe.sbt"     % "sbt-native-packager" % "1.3.4")
//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.1"
