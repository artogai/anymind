import anymind.build.Dependencies

ThisBuild / scalafixScalaBinaryVersion := scalaBinaryVersion.value
ThisBuild / scalafixDependencies ++= Seq(
  Dependencies.plugins.organizeImports
)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
