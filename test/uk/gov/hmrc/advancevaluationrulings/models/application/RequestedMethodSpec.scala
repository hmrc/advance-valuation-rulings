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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class RequestedMethodSpec extends SpecBase {

  "MethodOne" - {
    "serialize and deserialize correctly" in {
      val methodOne = MethodOne(Some("relatedParties"), Some("goodsRestrictions"), Some("saleConditions"))
      val json      = Json.toJson(methodOne)
      json.validate[MethodOne] mustBe JsSuccess(methodOne)
    }
  }

  "MethodTwo" - {
    "serialize and deserialize correctly" in {
      val methodTwo = MethodTwo("whyNotOtherMethods", "previousIdenticalGoods")
      val json      = Json.toJson(methodTwo)
      json.validate[MethodTwo] mustBe JsSuccess(methodTwo)
    }
  }

  "MethodThree" - {
    "serialize and deserialize correctly" in {
      val methodThree = MethodThree("whyNotOtherMethods", "previousSimilarGoods")
      val json        = Json.toJson(methodThree)
      json.validate[MethodThree] mustBe JsSuccess(methodThree)
    }
  }

  "MethodFour" - {
    "serialize and deserialize correctly" in {
      val methodFour = MethodFour("whyNotOtherMethods", "deductiveMethod")
      val json       = Json.toJson(methodFour)
      json.validate[MethodFour] mustBe JsSuccess(methodFour)
    }
  }

  "MethodFive" - {
    "serialize and deserialize correctly" in {
      val methodFive = MethodFive("whyNotOtherMethods", "computedValue")
      val json       = Json.toJson(methodFive)
      json.validate[MethodFive] mustBe JsSuccess(methodFive)
    }
  }

  "MethodSix" - {
    "serialize and deserialize correctly" in {
      val methodSix = MethodSix("whyNotOtherMethods", AdaptedMethod.MethodOne, "valuationDescription")
      val json      = Json.toJson(methodSix)
      json.validate[MethodSix] mustBe JsSuccess(methodSix)
    }
  }

  "AdaptedMethod" - {
    "serialize and deserialize correctly" in {
      val method = AdaptedMethod.MethodOne
      val json   = Json.toJson(method)
      json.validate[AdaptedMethod] mustBe JsSuccess(method)
    }

    "contain all expected values" in {
      AdaptedMethod.values must contain allOf (
        AdaptedMethod.MethodOne,
        AdaptedMethod.MethodTwo,
        AdaptedMethod.MethodThree,
        AdaptedMethod.MethodFour,
        AdaptedMethod.MethodFive,
        AdaptedMethod.Unable
      )
    }
  }
}
