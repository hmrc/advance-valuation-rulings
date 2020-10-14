/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.model

import uk.gov.hmrc.bindingtariffclassification.model.RESTFormatters._
import uk.gov.hmrc.play.test.UnitSpec
import util.CaseData
import play.api.libs.json._

class RestFormattersSpec extends UnitSpec{

  "BTIApplication format " should {

    val app = CaseData.createBasicBTIApplication.copy(relatedBTIReference = List("12345678"))

    val json = Json.toJson(app)(formatBTIApplication)

    "Deserialise related BTIs from an array" in {
      formatBTIApplication.reads(json) shouldBe a[JsSuccess[_]]
      formatBTIApplication.reads(json).get.relatedBTIReference shouldBe List("12345678")
    }

    "Deserialise related BTI from a nullable String" in {

      val arrayPruningTransformer = (__ \ 'relatedBTIReference).json.prune

      val stringAddingTransformer = __.json.update(
        __.read[JsObject].map{o => o ++ Json.obj("relatedBTIReference" -> JsString("123456789"))})

      val nullableStringJson = json.transform(arrayPruningTransformer).flatMap(_.transform(stringAddingTransformer)).get

      formatBTIApplication.reads(nullableStringJson) shouldBe a[JsSuccess[_]]
      formatBTIApplication.reads(nullableStringJson).get.relatedBTIReference shouldBe List("123456789")

    }

    "Deserialise related BTI where there is no relatedBTIReference value" in {

      val arrayPruningTransformer = (__ \ 'relatedBTIReference).json.prune

      val nullableStringJson = json.transform(arrayPruningTransformer).get

      formatBTIApplication.reads(nullableStringJson) shouldBe a[JsSuccess[_]]
      formatBTIApplication.reads(nullableStringJson).get.relatedBTIReference shouldBe Nil

    }

    "Deserialise related BTI where relatedBTIReference value is passed as null" in {

      val arrayPruningTransformer = (__ \ 'relatedBTIReference).json.prune
      val stringAddingTransformer = __.json.update(
        __.read[JsObject].map{o => o ++ Json.obj("relatedBTIReference" -> JsNull)})

      val nullableStringJson = json.transform(arrayPruningTransformer).flatMap(_.transform(stringAddingTransformer)).get

      formatBTIApplication.reads(nullableStringJson) shouldBe a[JsSuccess[_]]
      formatBTIApplication.reads(nullableStringJson).get.relatedBTIReference shouldBe Nil
    }
  }
}
