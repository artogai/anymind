package anymind.build.modules

import sbt._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin

object RootModule extends AutoPlugin {

  override def requires: Plugins      = JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  object autoImport {
    val lib = anymind.build.Dependencies

    def module(id: String) =
      Project(id, file(id)).enablePlugins(SubModule)

    def protoModule(id: String) =
      module(id).enablePlugins(ProtoModule)
  }
}
