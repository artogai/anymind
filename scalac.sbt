val lintOpts = Seq(
  "-Wunused:_",
  "-Wdead-code",
  "-Wvalue-discard",
  "-Wnumeric-widen",
  "-Xlint:_",
)

val scalacOpts = Seq(
  "-language:_",
  "-unchecked",
  "-explaintypes",
  "-Ymacro-annotations",
  "-Wconf:src=src_managed/.*:silent",
  "-Wconf:cat=lint-byname-implicit:silent", // https://github.com/scala/bug/issues/12072
) ++ Seq("-encoding", "UTF-8")

ThisBuild / scalacOptions ++=
  scalacOpts ++ lintOpts

Compile / console / scalacOptions :=
  (Compile / console / scalacOptions)
    .value
    .filterNot(lintOpts.toSet) :+ "-Wconf:any:silent"

Test / console / scalacOptions :=
  (Compile / console / scalacOptions).value
