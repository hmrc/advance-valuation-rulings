import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  lazy val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-play-25" % "3.13.0",
    "uk.gov.hmrc" %% "play-json-union-formatter" % "1.4.0",
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.2.0",
    ws
  )

  lazy val scope: String = "test,it"

  lazy val test = Seq(
    "org.mockito" % "mockito-core" % "2.23.0" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.scalaj" %% "scalaj-http" % "2.4.1" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "uk.gov.hmrc" %% "hmrctest" % "3.2.0" % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "3.1.0" % scope
  )

}
