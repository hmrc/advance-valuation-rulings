import sbt.*

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "9.0.0"
  private lazy val hmrcMongoPlayVersion     = "2.1.0"

  private val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % hmrcMongoPlayVersion,
    "com.beachape"            %% "enumeratum-play-json"         % "1.8.1",
    "org.typelevel"           %% "cats-core"                    % "2.12.0",
    "org.apache.xmlgraphics"   % "fop"                          % "2.9",
    "uk.gov.hmrc"             %% "crypto-json-play-30"          % "8.0.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "3.0.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30"  % "2.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion,
    "org.mockito"       %% "mockito-scala"           % "1.17.37",
    "wolfendale"        %% "scalacheck-gen-regexp"   % "0.1.2",
    "org.scalatestplus" %% "scalacheck-1-17"         % "3.2.18.0",
    "org.apache.pdfbox"  % "pdfbox"                  % "3.0.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test
}
