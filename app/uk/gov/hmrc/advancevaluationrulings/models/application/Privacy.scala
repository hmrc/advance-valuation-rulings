/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.models.application

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class Privacy(override val entryName: String) extends EnumEntry

object Privacy extends Enum[Privacy] with PlayJsonEnum[Privacy] {
  val values: IndexedSeq[Privacy] = findValues

  case object Public extends Privacy("Public")
  case object HmrcOnly extends Privacy("HmrcOnly")
  case object Confidential extends Privacy("Confidential")
}
