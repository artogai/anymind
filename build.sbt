ThisBuild / organization := "dev.ogai"
name                     := "anymind"
ThisBuild / scalaVersion := "2.13.7"

lazy val common   = protoModule("common")
lazy val gateway  = module("gateway")
lazy val wallet   = module("wallet")
