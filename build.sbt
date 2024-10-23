import uk.gov.hmrc.DefaultBuildSettings.itSettings

ThisBuild / scalaVersion := "3.5.1"
ThisBuild / majorVersion := 0

val appName = "advance-valuation-rulings"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(PlayScala, SbtDistributablesPlugin)
    // Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(PlayKeys.playDefaultPort := 12601)
    .settings(CodeCoverageSettings.settings)
    .settings(
      play.sbt.routes.RoutesKeys.routesImport ++= Seq(
        "uk.gov.hmrc.advancevaluationrulings.models.application.ApplicationId",
        "uk.gov.hmrc.advancevaluationrulings.models.DraftId"
      )
    )
    .settings(
      libraryDependencies ++= AppDependencies(),
      scalacOptions ++= List(
        "-feature",
        "-Wconf:msg=unused import&src=conf/.*:s",
        "-Wconf:msg=unused import&src=views/.*:s",
        "-Wconf:src=routes/.*:s"
      )
    )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice)
  .settings(itSettings())
  .settings(libraryDependencies ++= AppDependencies())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
