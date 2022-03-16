libraryDependencies ++=
  Seq(
    lib.akka.actor,
    lib.akka.stream,
    lib.akka.http,
    lib.akka.httpCirce,
    lib.akka.alpakka,
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerExposedPorts := Seq(8080)
