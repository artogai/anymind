ThisBuild / organization := "dev.ogai"
name                     := "anymind"
ThisBuild / scalaVersion := "2.13.7"

lazy val common   = protoModule("common")
lazy val gateway  = module("gateway").dependsOn(common)
lazy val wallet   = module("wallet").dependsOn(common)
