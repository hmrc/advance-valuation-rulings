import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  lazy val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-play-25"          % "4.12.0",
    "uk.gov.hmrc" %% "crypto"                     % "5.3.0",
    "uk.gov.hmrc" %% "play-json-union-formatter"  % "1.5.0",
    "uk.gov.hmrc" %% "simple-reactivemongo"       % "7.22.0-play-25",
    "io.megl"     %% "play-json-extra"            % "2.4.3",
    ws
  )

  lazy val scope: String = "test,it"

  lazy val test = Seq(
    "com.github.tomakehurst" %  "wiremock"            % "2.22.0"        % scope,
    "org.mockito"            %  "mockito-core"        % "2.26.0"        % scope,
    "org.pegdown"            %  "pegdown"             % "1.6.0"         % scope,
    "org.scalaj"             %% "scalaj-http"         % "2.4.1"         % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"  % "2.0.1"         % scope,
    "uk.gov.hmrc"            %% "hmrctest"            % "3.8.0-play-25" % scope,
    "uk.gov.hmrc"            %% "reactivemongo-test"  % "4.16.0-play-25" % scope
  )

}
