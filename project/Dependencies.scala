package anymind.build
import sbt._

object Dependencies {

  object version {
    val akka          = "2.6.18"
    val akkaHttp      = "10.2.9"
    val akkaHttpCirce = "1.39.2"
    val alpakka       = "2.1.1"
    val kafka         = "2.8.1"
    val scalaPb       = "0.11.8"
    def rocksdb       = "6.28.2"
    val circe         = "0.14.1"
    val slf4j         = "1.7.33"
    val logback       = "1.2.10"

    val organizeImports  = "0.6.0"
    val kindProjector    = "0.13.2"
    val betterMonadicFor = "0.3.1"
  }

  object akka {
    def actor     = "com.typesafe.akka" %% "akka-actor"        % version.akka
    def stream    = "com.typesafe.akka" %% "akka-stream"       % version.akka
    def http      = "com.typesafe.akka" %% "akka-http"         % version.akkaHttp
    def alpakka   = "com.typesafe.akka" %% "akka-stream-kafka" % version.alpakka
    def httpCirce = "de.heikoseeberger" %% "akka-http-circe"   % version.akkaHttpCirce
  }

  object kafka {
    def client = "org.apache.kafka" % "kafka-clients" % version.kafka
  }

  object scalapb {
    def runtime = "com.thesamet.scalapb" %% "scalapb-runtime" % version.scalaPb % "protobuf"
  }

  object circe {
    def core    = "io.circe" %% "circe-core"    % version.circe
    def generic = "io.circe" %% "circe-generic" % version.circe
    def parser  = "io.circe" %% "circe-parser"  % version.circe

    def all = Seq(core, generic, parser)
  }

  def rocksdb = "org.rocksdb" % "rocksdbjni" % version.rocksdb

  object logging {

    // sink
    def slf4j = "org.slf4j" % "slf4j-api" % version.slf4j

    // bridges
    def jcl   = "org.slf4j" % "jcl-over-slf4j"   % version.slf4j
    def log4j = "org.slf4j" % "log4j-over-slf4j" % version.slf4j
    def jul   = "org.slf4j" % "jul-to-slf4j"     % version.slf4j

    // impl
    def logback = "ch.qos.logback" % "logback-classic" % version.logback

    def default = Seq(slf4j, jcl, log4j, jul, logback)

    def exclude = Seq(
      ExclusionRule("commons-logging", "commons-logging"),
      ExclusionRule("log4j", "log4j"),
    )
  }

  object plugins {
    def organizeImports = "com.github.liancheng" %% "organize-imports" % version.organizeImports
    def kindProjector   = "org.typelevel"         % "kind-projector"   % version.kindProjector cross CrossVersion.full
    def betterMonadicFor = "com.olegpy" %% "better-monadic-for" % version.betterMonadicFor
  }

  def exclude = logging.exclude

}
