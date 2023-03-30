import sbt._

object AppDependencies {

  private val AllTestScope     = "test, it"
  private val bootstrapVersion = "7.14.0"
  private val hmrcMongoVersion = "1.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion,
    "com.beachape"      %% "enumeratum-play-json"      % "1.6.3",
    "org.typelevel"     %% "cats-core"                 % "2.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootstrapVersion % AllTestScope,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.0"         % AllTestScope,
    "org.scalatest"       %% "scalatest"               % "3.2.15"         % AllTestScope,
    "org.scalacheck"      %% "scalacheck"              % "1.15.4"         % AllTestScope,
    "wolfendale"          %% "scalacheck-gen-regexp"   % "0.1.2"          % AllTestScope,
    "org.scalatestplus"   %% "scalacheck-1-17"         % "3.2.15.0"       % AllTestScope,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % AllTestScope,
    "org.mockito"         %% "mockito-scala"           % "1.16.42"        % AllTestScope
  )
}
