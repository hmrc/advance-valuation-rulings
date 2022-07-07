import play.sbt.PlayImport._
import sbt._
import play.core.PlayVersion.current

object AppDependencies {

  lazy val compile = Seq(
    "uk.gov.hmrc"          %% "bootstrap-backend-play-28"     % "6.2.0",
    "uk.gov.hmrc"          %% "crypto"                        % "6.1.0",
    "uk.gov.hmrc"          %% "play-json-union-formatter"     % "1.15.0-play-28",
    "uk.gov.hmrc"          %% "simple-reactivemongo"          % "8.1.0-play-27",
    "uk.gov.hmrc"          %% "mongo-lock"                    % "7.1.0-play-27",
    "org.reactivemongo"    %% "reactivemongo-akkastream"      % "0.18.8",
    "com.typesafe.play"    %% "play-json"                     % "2.9.2",
    "org.typelevel"        %% "cats-core"                     % "2.8.0",
    "com.github.pathikrit" %% "better-files"                  % "3.9.1",
    "org.quartz-scheduler" % "quartz"                         % "2.3.2",
    ws
  )

  val jettyVersion = "9.4.27.v20200227"

  val test: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock"                  % "2.27.2",
    "com.typesafe.play"      %% "play-test"                % current ,
    "org.mockito"            % "mockito-core"              % "4.6.1",
    "org.jsoup"              % "jsoup"                     % "1.15.2",
    "org.pegdown"            % "pegdown"                   % "1.6.0" ,
    "org.scalatest"          %% "scalatest"                % "3.2.9" ,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0" ,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.9.0",
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.35.10",
    "org.scalacheck"         %% "scalacheck"               % "1.16.0",
    "uk.gov.hmrc"            %% "http-verbs-play-28"       % "13.8.1-RC1",
    "uk.gov.hmrc"            %% "service-integration-test" % "1.3.0-play-28",
    "uk.gov.hmrc"            %% "reactivemongo-test"       % "5.0.0-play-27",
    "org.scalaj"             %% "scalaj-http"              % "2.4.2",

    "org.eclipse.jetty" % "jetty-server"  % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion
  ).map(_ % "test, it")

}
