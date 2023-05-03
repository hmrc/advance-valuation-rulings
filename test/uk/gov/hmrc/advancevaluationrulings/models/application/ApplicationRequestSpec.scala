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

package uk.gov.hmrc.advancevaluationrulings.models.application

import play.api.libs.json.{Json, JsSuccess}
import uk.gov.hmrc.advancevaluationrulings.models.DraftId

import generators.Generators
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ApplicationRequestSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  import ApplicationRequestSpec._

  "ApplicationRequest" should {
    "be able to deserialize successful body" in {
      ApplicationRequest.format.reads(Json.parse(body)) shouldBe JsSuccess(
        ApplicationRequest(
          draftId = draftId,
          trader = eoriDetails,
          agent = None,
          contact = contact,
          requestedMethod = requestedMethod,
          goodsDetails,
          attachments = Seq.empty
        )
      )
    }

    "should be able to write body" in {
      ApplicationRequest.format.writes(
        ApplicationRequest(
          draftId = draftId,
          trader = eoriDetails,
          agent = None,
          contact = contact,
          requestedMethod = requestedMethod,
          goodsDetails = goodsDetails,
          attachments = Seq.empty
        )
      ) shouldBe Json.parse(body)
    }
  }
}

object ApplicationRequestSpec extends Generators {
  val randomString: String = stringsWithMaxLength(8).sample.getOrElse("random")

  val draftId: DraftId = DraftId(0)

  val eoriDetails = TraderDetail(
    eori = randomString,
    businessName = randomString,
    addressLine1 = randomString,
    addressLine2 = Some(randomString),
    addressLine3 = None,
    postcode = randomString,
    countryCode = randomString,
    phoneNumber = Some(randomString)
  )

  val contact = ContactDetails(
    name = randomString,
    email = randomString,
    phone = Some(randomString)
  )

  val requestedMethod = MethodThree(
    whyNotOtherMethods = randomString,
    previousSimilarGoods = randomString
  )

  val goodsDetails = GoodsDetails(
    goodsName = randomString,
    goodsDescription = randomString,
    envisagedCommodityCode = Some(randomString),
    knownLegalProceedings = Some(randomString),
    confidentialInformation = Some(randomString)
  )

  val goodsDetailsNoDetails = GoodsDetails(
    goodsName = randomString,
    goodsDescription = randomString,
    envisagedCommodityCode = None,
    knownLegalProceedings = None,
    confidentialInformation = None
  )

  val body =
    s"""{
    |"draftId": "$draftId",
    |"trader": {
    |  "eori": "$randomString",
    |  "businessName": "$randomString",
    |  "addressLine1": "$randomString",
    |  "addressLine2": "$randomString",
    |  "postcode": "$randomString",
    |  "countryCode": "$randomString",
    |  "phoneNumber": "$randomString"
    |},
    |"contact": {
    |  "name": "$randomString",
    |  "email": "$randomString",
    |  "phone": "$randomString"
    |},
    |"requestedMethod" : {
    |  "whyNotOtherMethods" : "$randomString",
    |  "previousSimilarGoods" : "$randomString",
    |  "type" : "MethodThree"
    |},
    |"goodsDetails": {
    |  "goodsName": "$randomString",
    |  "goodsDescription": "$randomString",
    |  "envisagedCommodityCode": "$randomString",
    |  "knownLegalProceedings": "$randomString",
    |  "confidentialInformation": "$randomString"
    |},
    |"attachments": []
    }""".stripMargin
}
