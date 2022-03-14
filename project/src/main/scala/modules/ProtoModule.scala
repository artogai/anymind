package anymind.build.modules

import sbt._
import Keys._
import sbtprotoc.ProtocPlugin
import anymind.build.Dependencies
import scalapb.GeneratorOption

object ProtoModule extends AutoPlugin {
  override def requires: Plugins      = ProtocPlugin && SubModule
  override def trigger: PluginTrigger = noTrigger

  object autoImport {
    val scalapbGenOptions = settingKey[Set[GeneratorOption]]("ScalaPb options")
  }

  import ProtocPlugin.autoImport._
  import autoImport._
  override def projectSettings: Seq[Setting[_]] =
    Seq(
      libraryDependencies ++= Seq(
        Dependencies.scalapb.runtime
      ),
      scalapbGenOptions := Set(
        GeneratorOption.NoLenses,
        GeneratorOption.SingleLineToProtoString,
      ),
      Compile / PB.targets := Seq(
        scalapb.gen(scalapbGenOptions.value) -> (Compile / sourceManaged).value / "scalapb"
      ),
    )
}
