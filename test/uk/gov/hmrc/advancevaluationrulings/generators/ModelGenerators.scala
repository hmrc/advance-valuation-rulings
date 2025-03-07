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

package uk.gov.hmrc.advancevaluationrulings.generators

import org.scalacheck.{Arbitrary, Gen}
import play.api.http.Status.OK
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application.ApplicationId
import uk.gov.hmrc.advancevaluationrulings.models.etmp._
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

trait ModelGenerators extends Generators {

  def applicationIdGen: Gen[ApplicationId] =
    for {
      value <- Gen.choose(1, 999999999)
    } yield ApplicationId(value)

  def draftIdGen: Gen[DraftId] =
    for {
      value <- Gen.choose(1, 999999999)
    } yield DraftId(value)

  def regimeGen: Gen[Regime] = Gen.oneOf(Regime.values)

  def eoriNumberGen: Gen[String] = for {
    countryCode <- Gen.buildableOfN[String, Char](2, Gen.alphaUpperChar)
    digits      <- Gen.buildableOfN[String, Char](12, Gen.numChar)
  } yield s"$countryCode$digits"

  def cdsEstablishmentAddressGen: Gen[CDSEstablishmentAddress] = for {
    streetAndNumber <- stringsWithMaxLength(70)
    city            <- stringsWithMaxLength(35)
    countryCode     <- stringsWithMaxLength(2)
    postalCode      <- Gen.option(stringsWithMaxLength(9))
  } yield CDSEstablishmentAddress(streetAndNumber, city, countryCode, postalCode)

  def contactInformationGen: Gen[ContactInformation] = for {
    personOfContact           <- Gen.option(stringsWithMaxLength(70))
    streetAndNumber           <- Gen.option(stringsWithMaxLength(70))
    sepCorrAddrIndicator      <- Gen.option(Gen.oneOf(true, false))
    city                      <- Gen.option(stringsWithMaxLength(35))
    postalCode                <- Gen.option(stringsWithMaxLength(9))
    countryCode               <- Gen.option(stringsWithMaxLength(2))
    telephoneNumber           <- Gen.option(stringsWithMaxLength(50))
    faxNumber                 <- Gen.option(stringsWithMaxLength(50))
    emailAddress              <- Gen.option(stringsWithMaxLength(50))
    instant                   <- Gen.option(localDateTimeGen.map(_.toInstant(ZoneOffset.UTC)))
    emailVerificationTimestamp = instant.map(DateTimeFormatter.ISO_INSTANT.format(_))
  } yield ContactInformation(
    personOfContact,
    sepCorrAddrIndicator,
    streetAndNumber,
    city,
    postalCode,
    countryCode,
    telephoneNumber,
    faxNumber,
    emailAddress,
    emailVerificationTimestamp
  )

  def responseDetailGen: Gen[ResponseDetail] = for {
    eoriNo                            <- eoriNumberGen
    cdsFullName                       <- stringsWithMaxLength(512)
    cdsEstablishmentAddressGen        <- cdsEstablishmentAddressGen
    contactInformation                <- Gen.option(contactInformationGen)
    consentToDisclosureOfPersonalData <- Gen.option(Gen.oneOf(Seq("0", "1")))
  } yield ResponseDetail(
    eoriNo,
    cdsFullName,
    cdsEstablishmentAddressGen,
    contactInformation,
    consentToDisclosureOfPersonalData
  )

  def subscriptionDisplayResponseGen: Gen[SubscriptionDisplayResponse] =
    responseDetailGen.map(rd => SubscriptionDisplayResponse(ResponseCommon(OK.toString), Some(rd)))

  def etmpSubscriptionDisplayResponseGen: Gen[ETMPSubscriptionDisplayResponse] =
    subscriptionDisplayResponseGen.map(ETMPSubscriptionDisplayResponse(_))

  def traderDetailsResponseGen: Gen[TraderDetailsResponse] = for {
    registeredDetails                 <- etmpSubscriptionDisplayResponseGen
    consentToDisclosureOfPersonalData <- Arbitrary.arbitrary[Boolean]
  } yield {
    val responseDetail = registeredDetails.subscriptionDisplayResponse.responseDetail.get
    TraderDetailsResponse(
      EORINo = responseDetail.EORINo,
      CDSFullName = responseDetail.CDSFullName,
      CDSEstablishmentAddress = responseDetail.CDSEstablishmentAddress,
      consentToDisclosureOfPersonalData = consentToDisclosureOfPersonalData,
      contactInformation = responseDetail.contactInformation
    )
  }
}
