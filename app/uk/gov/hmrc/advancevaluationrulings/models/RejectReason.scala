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

package uk.gov.hmrc.advancevaluationrulings.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class RejectReason(override val entryName: String) extends EnumEntry

object RejectReason extends Enum[RejectReason] with PlayJsonEnum[RejectReason] {
  val values: IndexedSeq[RejectReason] = findValues

  case object APPLICATION_WITHDRAWN extends RejectReason("Application withdrawn")
  case object ATAR_RULING_ALREADY_EXISTS extends RejectReason("ATaR ruling already exists")
  case object DUPLICATE_APPLICATION extends RejectReason("Duplicate application")
  case object NO_INFO_FROM_TRADER extends RejectReason("No information from trader")
  case object OTHER extends RejectReason("Other")
}
