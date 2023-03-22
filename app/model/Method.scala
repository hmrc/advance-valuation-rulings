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

package model

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.JsObject

sealed abstract class Method

final case class MethodOne(
                      saleBetweenRelatedParties: Option[String],
                      goodsRestrictions: Option[String],
                      saleConditions: Option[String]
                    ) extends Method

sealed trait IdenticalGoodsExplanation extends Any

final case class PreviousIdenticalGoods(val value: String) extends AnyVal with IdenticalGoodsExplanation
final case class OtherUsersIdenticalGoods(val value: String) extends AnyVal with IdenticalGoodsExplanation

final case class MethodTwo(
                      whyNotOtherMethods: String,
                      detailedDescription: IdenticalGoodsExplanation
                    ) extends Method


sealed trait SimilarGoodsExplanation extends Any

final case class PreviousSimilarGoods(val value: String) extends AnyVal with SimilarGoodsExplanation
final case class OtherUsersSimilarGoods(val value: String) extends AnyVal with SimilarGoodsExplanation

final case class MethodThree(
                        whyNotOtherMethods: String,
                        detailedDescription: SimilarGoodsExplanation
                      ) extends Method

final case class MethodFour(
                       whyNotOtherMethods: String,
                       deductiveMethod: String
                     ) extends Method

final case class MethodFive(
                       whyNotOtherMethods: String,
                       computedValue: String
                     ) extends Method

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

final case class MethodSix(
                      whyNotOtherMethods: String,
                      adoptMethod: AdaptedMethod,
                      valuationDescription: String
                    ) extends Method
