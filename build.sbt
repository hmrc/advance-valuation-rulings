import play.core.PlayVersion
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport._
import play.sbt.PlayScala
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Tests
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._


val appName = "binding-tariff-classification"

lazy val appDependencies: Seq[ModuleID] = compile ++ test

lazy val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-25" % "3.10.0",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.4.0",
  "uk.gov.hmrc" %% "play-reactivemongo" % "6.2.0",
  ws
)

lazy val scope: String = "test,it"

lazy val test = Seq(
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % "2.23.0" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.scalatest" %% "scalatest" % "3.0.4" % scope,
  "uk.gov.hmrc" %% "hmrctest" % "3.1.0" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "3.0.0" % scope
)

lazy val plugins: Seq[Plugins] = Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val microservice = (project in file("."))
  .enablePlugins(plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 0)
  .settings(
    name := appName,
    scalaVersion := "2.11.11",
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    routesGenerator := StaticRoutesGenerator
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in Test := Seq(Tests.Filter(unitFilter)),
    unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test/unit"),
    addTestReportOption(Test, "test-reports")
  )
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := Seq((baseDirectory in IntegrationTest).value / "test/it"),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo)
  .settings(ivyScala := ivyScala.value map {
    _.copy(overrideScalaVersion = true)
  })

lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"
lazy val allItPhases = "tit->it;it->it;it->compile;compile->compile"

lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest

def unitFilter(name: String): Boolean = name startsWith "unit"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = {
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }
}

// Coverage configuration
coverageMinimum := 97
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
