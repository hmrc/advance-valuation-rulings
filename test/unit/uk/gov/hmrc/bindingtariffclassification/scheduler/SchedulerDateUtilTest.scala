/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bindingtariffclassification.scheduler

import java.time._

import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig

import scala.concurrent.duration._

class SchedulerDateUtilTest extends BaseSpec with BeforeAndAfterEach {

  private val zone              = ZoneOffset.UTC
  private val config: AppConfig = mock[AppConfig]
  private val util              = new SchedulerDateUtil(config)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(config)
  }

  "Next Run" should {
    "Calculate the next run date-time given the run time is now" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T12:00:00"), zone))

      util.nextRun(
        time("12:00"),
        1.day
      ) shouldBe instant("2019-01-01T12:00:00")
    }

    "Calculate the next run date-time given the run time today has not yet passed" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T14:00:00"), zone))

      util.nextRun(
        time("16:00"),
        1.day
      ) shouldBe instant("2019-01-01T16:00:00")
    }

    "Calculate the next run date-time given the run time today has passed" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T14:00:00"), zone))

      util.nextRun(
        time("12:00"),
        1.day
      ) shouldBe instant("2019-01-02T12:00:00")
    }
  }

  "Closest Run" should {
    "Calculate the closest run date given the run time is now" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T12:00:00"), zone))

      util.closestRun(
        time("12:00"),
        1.day
      ) shouldBe instant("2019-01-01T12:00:00")
    }

    "Calculate the closest run date given the run time today has passed and the previous run time is closer" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T08:00:00"), zone))

      util.closestRun(
        time("01:00"),
        1.day
      ) shouldBe instant("2019-01-01T01:00:00")
    }

    "Calculate the closest run date given the run time today has passed and the next run time is closer" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T21:00:00"), zone))

      util.closestRun(
        time("01:00"),
        1.day
      ) shouldBe instant("2019-01-02T01:00:00")
    }

    "Calculate the closest run date given the run time today has not yet passed and the next run time is closer" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T21:00:00"), zone))

      util.closestRun(
        time("22:00"),
        1.day
      ) shouldBe instant("2019-01-01T22:00:00")
    }

    "Calculate the closest run date given the run time today has not yet passed and the previous run time is closer" in {
      given(config.clock).willReturn(Clock.fixed(instant("2019-01-01T01:00:00"), zone))

      util.closestRun(
        time("21:00"),
        1.day
      ) shouldBe instant("2018-12-31T21:00:00")
    }
  }

  private def instant(datetime: String): Instant =
    LocalDateTime.parse(datetime).atZone(zone).toInstant

  private def time(datetime: String): LocalTime =
    LocalTime.parse(datetime)

}
