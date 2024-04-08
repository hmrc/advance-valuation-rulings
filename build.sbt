import uk.gov.hmrc.DefaultBuildSettings.itSettings

ThisBuild / scalaVersion := "2.13.13"
ThisBuild / majorVersion := 0

val appName = "advance-valuation-ruling"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(
      // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
    )
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
        "-Wconf:src=routes/.*:s"
      )
    )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle")
