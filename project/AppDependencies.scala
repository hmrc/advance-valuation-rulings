import sbt.*

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "9.7.0"
  private lazy val hmrcMongoPlayVersion     = "2.4.0"

  private val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % hmrcMongoPlayVersion,
    "com.beachape"            %% "enumeratum-play-json"         % "1.8.2",
    "org.typelevel"           %% "cats-core"                    % "2.13.0",
    "org.apache.xmlgraphics"   % "fop"                          % "2.10",
    "uk.gov.hmrc"             %% "crypto-json-play-30"          % "8.1.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "3.0.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30"  % "2.1.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion,
    "org.scalatestplus" %% "scalacheck-1-18"         % "3.2.19.0",
    "org.apache.pdfbox"  % "pdfbox"                  % "3.0.4"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
