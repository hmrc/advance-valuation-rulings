package uk.gov.hmrc.bindingtariffclassification.component.utils

import java.time._

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


class AppConfigWithAFixedDate @Inject()(runModeConfiguration: Configuration, config: ServicesConfig)
  extends AppConfig(runModeConfiguration, config) {
  private val defaultSystemDate: Instant = LocalDate.parse("2019-02-03").atStartOfDay().toInstant(ZoneOffset.UTC)
  private val defaultZoneId = ZoneId.systemDefault()
  override lazy val clock: Clock = Clock.fixed(defaultSystemDate, defaultZoneId)
}
