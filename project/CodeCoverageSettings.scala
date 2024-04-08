import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

//  private val excludedPackages: Seq[String] = Seq(
//    "<empty>",
//    "Reverse.*",
//    "uk.gov.hmrc.BuildInfo",
//    "app.*",
//    "prod.*",
//    ".*Routes.*",
//    ".*InternalAuthTokenInitialiser.*"
//  )

  private val excludedPackages: Seq[String] = Seq(
//    "<empty>",
//    ".*(Routes).*",
//    "uk.gov.hmrc.advancevaluationrulings.models.common",
//    "uk.gov.hmrc.advancevaluationrulings.models.etmp",
//    "uk.gov.hmrc.advancevaluationrulings.repositories"
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    ".*InternalAuthTokenInitialiser.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 64,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
