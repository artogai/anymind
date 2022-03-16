addDependencyTreePlugin

addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.5.3")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.4.5")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"          % "0.1.3")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.9.33")
addSbtPlugin("com.thesamet"     % "sbt-protoc"          % "1.0.3")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "1.1.0")
addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.9")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.1"
)
