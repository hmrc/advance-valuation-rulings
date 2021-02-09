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

import java.time.{Instant, LocalDate, LocalTime}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig

import scala.concurrent.duration.FiniteDuration
import java.time.ZonedDateTime
import java.time.temporal.TemporalUnit
import java.time.temporal.ChronoUnit

@Singleton
class SchedulerDateUtil @Inject() (appConfig: AppConfig) {

  def nextRun(offset: LocalTime, interval: FiniteDuration): Instant = {
    val now = ZonedDateTime.now(appConfig.clock).withNano(0)

    val offsetToday = now.`with`(offset)

    if (offsetToday.isBefore(now))
      offsetToday.plus(interval.toMillis, ChronoUnit.MILLIS).toInstant
    else
      offsetToday.toInstant
  }

  def closestRun(offset: LocalTime, interval: FiniteDuration): Instant = {
    val now = ZonedDateTime.now(appConfig.clock).withNano(0)

    val offsetToday = now.`with`(offset)

    val prevRun =
      if (offsetToday.isBefore(now)) offsetToday else offsetToday.minus(interval.toMillis, ChronoUnit.MILLIS)
    val nextRun =
      if (!offsetToday.isBefore(now)) offsetToday else offsetToday.plus(interval.toMillis, ChronoUnit.MILLIS)

    val timeUntilNext = ChronoUnit.MILLIS.between(now, nextRun)
    val timeSinceLast = ChronoUnit.MILLIS.between(prevRun, now)

    if (timeUntilNext <= timeSinceLast)
      nextRun.toInstant
    else
      prevRun.toInstant
  }

}
