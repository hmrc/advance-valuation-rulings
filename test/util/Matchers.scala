/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.Instant

import org.scalatest.matchers.{MatchResult, Matcher}

object Matchers {

  def roughlyBe(time: Instant) = new RoughlyMatches(time)

  protected class RoughlyMatches(time: Instant) extends Matcher[Instant] {

    override def apply(d: Instant): MatchResult = MatchResult(
      d.isBefore(time.plusSeconds(60)) && d.isAfter(time.minusSeconds(60)),
      s"date [$d] was not within a minute of [$time]",
      s"date [$d] was within a minute of [$time]"
    )
  }

}
