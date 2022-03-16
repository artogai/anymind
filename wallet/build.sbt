libraryDependencies ++=
  Seq(
    lib.akka.actor,
    lib.akka.stream,
    lib.akka.alpakka,
    lib.rocksdb,
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

dockerExposedVolumes := Seq("/opt/docker/rocksdb")
