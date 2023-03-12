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

package uk.gov.hmrc.advancevaluationrulings.models.common

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class ValuationMethod(override val entryName: String) extends EnumEntry

object ValuationMethod extends Enum[ValuationMethod] with PlayJsonEnum[ValuationMethod] {
  val values: IndexedSeq[ValuationMethod] = findValues

  case object Method1 extends ValuationMethod("method1")
  case object Method2 extends ValuationMethod("method2")
  case object Method3 extends ValuationMethod("method3")
  case object Method4 extends ValuationMethod("method4")
  case object Method5 extends ValuationMethod("method5")
  case object Method6 extends ValuationMethod("method6")
}
