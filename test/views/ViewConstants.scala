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

package views

import uk.gov.hmrc.advancevaluationrulings.generators.Generators
import uk.gov.hmrc.advancevaluationrulings.models.application._

import java.time.Instant
import java.time.temporal.ChronoUnit

trait ViewConstants extends Generators {

  val agentOrgRole: String    = "AgentOrg"
  val employeeOrgRole: String = "EmployeeOrg"
  val agentTraderRole: String = "AgentTrader"
  val otherRole: String       = "Other"
  val randomString: String    = stringsWithMaxLength(8).sample.get

  val now: Instant                 = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  val adaptedMethod: AdaptedMethod = AdaptedMethod.MethodFive
  val attachments: Seq[Attachment] = Seq(
    Attachment(
      id = 1,
      name = "attachment",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1337
    )
  )
  val traderDetail: TraderDetail   = TraderDetail(
    eori = "eori",
    businessName = "name",
    addressLine1 = "line1",
    addressLine2 = None,
    addressLine3 = None,
    postcode = "postcode",
    countryCode = "GB",
    phoneNumber = None,
    isPrivate = Some(true)
  )
  val addressLines: String         = traderDetail.addressLines.mkString("\n")

  val methodOne: MethodOne     = MethodOne(
    saleBetweenRelatedParties = Some(randomString),
    goodsRestrictions = Some(randomString),
    saleConditions = Some(randomString)
  )
  val methodTwo: MethodTwo     = MethodTwo(
    whyNotOtherMethods = randomString,
    previousIdenticalGoods = randomString
  )
  val methodThree: MethodThree = MethodThree(
    whyNotOtherMethods = randomString,
    previousSimilarGoods = randomString
  )
  val methodFour: MethodFour   = MethodFour(
    whyNotOtherMethods = randomString,
    deductiveMethod = randomString
  )
  val methodFive: MethodFive   = MethodFive(
    whyNotOtherMethods = randomString,
    computedValue = randomString
  )
  val methodSix: MethodSix     = MethodSix(
    whyNotOtherMethods = randomString,
    adaptedMethod = adaptedMethod,
    valuationDescription = randomString
  )

  private val goodsDetails: GoodsDetails = GoodsDetails(
    goodsDescription = randomString,
    envisagedCommodityCode = Some(randomString),
    knownLegalProceedings = Some(randomString),
    confidentialInformation = Some(randomString),
    similarRulingGoodsInfo = Some(randomString),
    similarRulingMethodInfo = Some(randomString)
  )
  private val contact: ContactDetails    = ContactDetails(
    name = "name",
    email = "email",
    phone = Some("phone"),
    companyName = Some("company name"),
    jobTitle = Some("job title")
  )

  val application: Application = Application(
    id = ApplicationId(1),
    applicantEori = "applicantEori",
    trader = traderDetail,
    agent = None,
    contact = contact,
    goodsDetails = goodsDetails,
    requestedMethod = methodOne,
    attachments = attachments,
    whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
    letterOfAuthority = None,
    submissionReference = "submissionReference",
    created = now,
    lastUpdated = now
  )
}
