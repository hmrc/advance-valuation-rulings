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

import generators.Generators
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.DraftId

class ApplicationRequestSpec extends SpecBase {

  import ApplicationRequestSpec._

  "ApplicationRequest" - {
    "be able to deserialize successful body" in {
      ApplicationRequest.format.reads(Json.parse(body)) mustBe JsSuccess(
        ApplicationRequest(
          draftId = draftId,
          trader = eoriDetails,
          agent = None,
          contact = contact,
          requestedMethod = requestedMethod,
          goodsDetails,
          attachments = Seq.empty,
          whatIsYourRole = WhatIsYourRole.EmployeeOrg,
          letterOfAuthority = None
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
          attachments = Seq.empty,
          whatIsYourRole = WhatIsYourRole.EmployeeOrg,
          letterOfAuthority = None
        )
      ) mustBe Json.parse(body)
    }
  }
}

object ApplicationRequestSpec extends Generators {
  val randomString: String = stringsWithMaxLength(8).sample.getOrElse("random")

  val draftId: DraftId = DraftId(0)

  val eoriDetails: TraderDetail = TraderDetail(
    eori = randomString,
    businessName = randomString,
    addressLine1 = randomString,
    addressLine2 = Some(randomString),
    addressLine3 = None,
    postcode = randomString,
    countryCode = randomString,
    phoneNumber = Some(randomString),
    isPrivate = Some(true)
  )

  val contact: ContactDetails = ContactDetails(
    name = randomString,
    email = randomString,
    phone = Some(randomString),
    companyName = Some(randomString),
    jobTitle = Some(randomString)
  )

  val requestedMethod: MethodThree = MethodThree(
    whyNotOtherMethods = randomString,
    previousSimilarGoods = randomString
  )

  val goodsDetails: GoodsDetails = GoodsDetails(
    goodsName = randomString,
    goodsDescription = randomString,
    similarRulingGoodsInfo = Some(randomString),
    similarRulingMethodInfo = Some(randomString),
    envisagedCommodityCode = Some(randomString),
    knownLegalProceedings = Some(randomString),
    confidentialInformation = Some(randomString)
  )

  val goodsDetailsNoDetails: GoodsDetails = GoodsDetails(
    goodsName = randomString,
    goodsDescription = randomString,
    similarRulingGoodsInfo = None,
    similarRulingMethodInfo = None,
    envisagedCommodityCode = None,
    knownLegalProceedings = None,
    confidentialInformation = None
  )

  val body: String =
    s"""{
    |"draftId": "$draftId",
    |"trader": {
    |  "eori": "$randomString",
    |  "businessName": "$randomString",
    |  "addressLine1": "$randomString",
    |  "addressLine2": "$randomString",
    |  "postcode": "$randomString",
    |  "countryCode": "$randomString",
    |  "phoneNumber": "$randomString",
    |  "isPrivate": true
    |},
    |"contact": {
    |  "name": "$randomString",
    |  "email": "$randomString",
    |  "phone": "$randomString",
    |  "companyName": "$randomString",
    |  "jobTitle": "$randomString"
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
    |  "confidentialInformation": "$randomString",
    |  "similarRulingGoodsInfo": "$randomString",
    |  "similarRulingMethodInfo": "$randomString"
    |},
    |"attachments": [],
    |"whatIsYourRole": "${WhatIsYourRole.EmployeeOrg.entryName}"
    }""".stripMargin
}
