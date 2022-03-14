addCommandAlias(
  "styleCheck",
  "scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check",
)
addCommandAlias(
  "styleFix",
  "scalafixAll; scalafmtSbt; scalafmtAll",
)
addCommandAlias(
  "compileAll",
  "compile; Test / compile; IntegrationTest / compile"
)
addCommandAlias(
  "itTest",
  "IntegrationTest / test"
)
addCommandAlias(
  "testAll",
  "test; IntegrationTest / test"
)
