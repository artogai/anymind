package anymind.build.modules

import sbt.{ Def, _ }
import Keys._
import anymind.build.Dependencies

object SubModule extends AutoPlugin {
  override def requires: Plugins      = RootModule
  override def trigger: PluginTrigger = noTrigger

  import RootModule.autoImport._
  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    addCompilerPlugin(Dependencies.plugins.kindProjector),
    addCompilerPlugin(Dependencies.plugins.betterMonadicFor),
    libraryDependencies ++= lib.logging.default,
    excludeDependencies ++= lib.exclude,
    Test / fork            := true,
  ) ++
    Defaults.itSettings

  override lazy val projectConfigurations = Seq(
    IntegrationTest.extend(Test)
  )

}
