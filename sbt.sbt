Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys ++= Set(
  autoStartServer,
  turbo,
)

ThisBuild / autoStartServer        := false
ThisBuild / includePluginResolvers := true
ThisBuild / turbo                  := true
