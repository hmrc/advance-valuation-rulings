import play.sbt.routes.RoutesKeys

lazy val microservice = Project("advance-valuation-rulings", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, ScalafmtPlugin)
  .settings(
    majorVersion        := 0,
    scalaVersion        := "2.13.8",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Ywarn-dead-code",
      "-Ywarn-unused:imports",   // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",    // Warn if a local definition is unused.
      "-Ywarn-unused:params",    // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars",   // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates"   // Warn if a private member is unused.
    ),
    PlayKeys.playDefaultPort := 12601
  )
  .settings(inConfig(Test)(testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(scoverageSettings)
  .settings(RoutesKeys.routesImport ++= Seq("uk.gov.hmrc.advancevaluationrulings.models.application.ApplicationId"))

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it",
    baseDirectory.value / "test-utils"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution := false,
  fork := true
)

lazy val scoverageSettings = {
  Seq(
    coverageExcludedPackages := """;uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*;testonly""",
    coverageExcludedFiles := "<empty>;.*javascript.*;.*Routes.*;.*testonly.*",
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageMinimumStmtTotal := 50
  )
}