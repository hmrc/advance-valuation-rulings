import play.sbt.PlayImport._
import sbt._
import play.core.PlayVersion.current

object AppDependencies {
  private lazy val mongoHmrcVersion = "0.68.0"
  val AkkaVersion = "2.6.19"

  lazy val compile = Seq(
    "uk.gov.hmrc"          %% "bootstrap-backend-play-28"     % "6.4.0",
    "uk.gov.hmrc"          %% "crypto"                        % "6.1.0",
    "uk.gov.hmrc"          %% "play-json-union-formatter"     % "1.15.0-play-28",
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-28"            % mongoHmrcVersion,
    "com.lightbend.akka"   %% "akka-stream-alpakka-mongodb"   % "3.0.4",
    "com.typesafe.akka"    %% "akka-stream"                   % AkkaVersion,
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
    "org.scalatest"          %% "scalatest"                % "3.2.12" ,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0" ,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.10.0",
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.62.2",
    "org.scalacheck"         %% "scalacheck"               % "1.16.0",
    "uk.gov.hmrc"            %% "http-verbs-play-28"       % "13.13.0",
    "uk.gov.hmrc"            %% "service-integration-test" % "1.3.0-play-28",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"  % mongoHmrcVersion,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2",

    "org.eclipse.jetty" % "jetty-server"  % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion
  ).map(_ % "test, it")

}
