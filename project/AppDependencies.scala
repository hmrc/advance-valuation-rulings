import sbt.*

object AppDependencies {

  private val AllTestScope     = "test, it"
  private val bootstrapVersion = "7.23.0"
  private val hmrcMongoVersion = "1.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"           % hmrcMongoVersion,
    "com.beachape"            %% "enumeratum-play-json"         % "1.6.3",
    "org.typelevel"           %% "cats-core"                    % "2.10.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-28" % "1.8.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-28"  % "1.2.0",
    "org.apache.xmlgraphics"   % "fop"                          % "2.9",
    "uk.gov.hmrc"             %% "crypto-json-play-28"          % "7.6.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % bootstrapVersion,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.8",
    "org.scalatest"       %% "scalatest"               % "3.2.17",
    "wolfendale"          %% "scalacheck-gen-regexp"   % "0.1.2",
    "org.scalatestplus"   %% "scalacheck-1-17"         % "3.2.17.0",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "org.mockito"         %% "mockito-scala"           % "1.17.30",
    "org.apache.pdfbox"    % "pdfbox"                  % "2.0.30"
  ).map(_ % AllTestScope)

  def apply(): Seq[ModuleID] = compile ++ test
}
