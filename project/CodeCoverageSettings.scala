import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Routes.*",
    ".*InternalAuthTokenInitialiser.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    coverageExcludedPackages := excludedPackages.mkString(";"),
    coverageMinimumStmtTotal := 100,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )
}
