/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.{Json, OFormat}

sealed trait RequestedMethod

object RequestedMethod {

  implicit lazy val format: OFormat[RequestedMethod] = Json.configured(jsonConfig).format
}

final case class MethodOne(
  saleBetweenRelatedParties: Option[String],
  goodsRestrictions: Option[String],
  saleConditions: Option[String]
) extends RequestedMethod

object MethodOne {

  implicit lazy val format: OFormat[MethodOne] = Json.format
}

final case class MethodTwo(
  whyNotOtherMethods: String,
  previousIdenticalGoods: String
) extends RequestedMethod

object MethodTwo {

  implicit lazy val format: OFormat[MethodTwo] = Json.format
}

final case class MethodThree(
  whyNotOtherMethods: String,
  previousSimilarGoods: String
) extends RequestedMethod

object MethodThree {

  implicit lazy val format: OFormat[MethodThree] = Json.format
}

final case class MethodFour(
  whyNotOtherMethods: String,
  deductiveMethod: String
) extends RequestedMethod

object MethodFour {

  implicit lazy val format: OFormat[MethodFour] = Json.format
}

final case class MethodFive(
  whyNotOtherMethods: String,
  computedValue: String
) extends RequestedMethod

object MethodFive {

  implicit lazy val format: OFormat[MethodFive] = Json.format
}

final case class MethodSix(
  whyNotOtherMethods: String,
  adaptedMethod: AdaptedMethod,
  valuationDescription: String
) extends RequestedMethod

object MethodSix {

  implicit lazy val format: OFormat[MethodSix] = Json.format
}

sealed abstract class AdaptedMethod(override val entryName: String) extends EnumEntry

object AdaptedMethod extends Enum[AdaptedMethod] with PlayJsonEnum[AdaptedMethod] {
  val values: IndexedSeq[AdaptedMethod] = findValues

  case object MethodOne extends AdaptedMethod("MethodOne")
  case object MethodTwo extends AdaptedMethod("MethodTwo")
  case object MethodThree extends AdaptedMethod("MethodThree")
  case object MethodFour extends AdaptedMethod("MethodFour")
  case object MethodFive extends AdaptedMethod("MethodFive")
  case object Unable extends AdaptedMethod("Unable")
}
