/*
 * Copyright 2023 HM Revenue & Customs
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

package util

import java.time.{Clock, Instant, ZoneOffset}

trait FixedTimeFixtures {

  val DAY_IN_SECONDS = 60 * 60 * 24

  val fixedTime  = Instant.parse("2021-02-01T09:00:00.00Z")
  val fixedClock = Clock.fixed(fixedTime, ZoneOffset.UTC)

  val fixedTimePlusADay = fixedTime.plusSeconds(DAY_IN_SECONDS)
  val fixedClockPlusADay = Clock.fixed(fixedTimePlusADay, ZoneOffset.UTC)

  /**
   * Returns an Instant which is specified days in the past relative to [[fixedTime]]
   * @param count number of days in the past
   */
  def daysInThePast(count: Int): Instant = {
    require(count >= 0)
    fixedTime.minusSeconds(DAY_IN_SECONDS * count)
  }

  /**
   * Returns an Instant which is specified days in the future relative to [[fixedTime]]
   * @param count number of days in the future
   */
  def daysInTheFuture(count: Int): Instant = {
    require(count <= 0)
    fixedTime.plusSeconds(DAY_IN_SECONDS * count)
  }
}

object FixedTimeFixtures extends FixedTimeFixtures