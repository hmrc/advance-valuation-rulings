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

package generators

import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.advancevaluationrulings.models.common._
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ETMPError, ErrorDetail, SourceFaultDetail}
import uk.gov.hmrc.advancevaluationrulings.models.etmp._
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.advancevaluationrulings.models.application.ApplicationId
import wolfendale.scalacheck.regexp.RegexpGen

trait ModelGenerators extends Generators {

  def applicationIdGen: Gen[ApplicationId] =
    for {
      value <- Gen.choose(1, 999999999)
    } yield ApplicationId(value)

  def regimeGen: Gen[Regime] = Gen.oneOf(Regime.values)

  def eoriNumberGen: Gen[String] = RegexpGen.from("^[A-Z]{2}[0-9A-Z]{12}$")

  def queryGen: Gen[Query] = for {
    regime                   <- regimeGen
    acknowledgementReference <- stringsWithMaxLength(32)
    eori                     <- eoriNumberGen
  } yield Query(regime, acknowledgementReference, taxPayerID = None, EORI = Option(eori))

  def sourceFaultDetail: Gen[SourceFaultDetail] =
    Gen.listOf(stringsWithMaxLength(10)).map(SourceFaultDetail(_))

  def errorDetailGen: Gen[ErrorDetail] = for {
    timestamp         <- localDateTimeGen
    correlationId     <- stringsWithMaxLength(36)
    errorCode         <- intsBelowValue(999)
    source            <- stringsWithMaxLength(10)
    errorMessage      <- Gen.option(stringsWithMaxLength(35))
    sourceFaultDetail <- Gen.option(sourceFaultDetail)
  } yield ErrorDetail(
    timestamp,
    correlationId,
    errorCode.toString,
    source,
    errorMessage,
    sourceFaultDetail
  )

  def CDSEstablishmentAddressGen: Gen[CDSEstablishmentAddress] = for {
    streetAndNumber <- stringsWithMaxLength(70)
    city            <- stringsWithMaxLength(35)
    countryCode     <- stringsWithMaxLength(2)
    postalCode      <- Gen.option(stringsWithMaxLength(9))
  } yield CDSEstablishmentAddress(streetAndNumber, city, countryCode, postalCode)

  def responseDetailGen: Gen[ResponseDetail] = for {
    eoriNo                     <- eoriNumberGen
    cdsFullName                <- stringsWithMaxLength(512)
    cdsEstablishmentAddressGen <- CDSEstablishmentAddressGen
  } yield ResponseDetail(eoriNo, cdsFullName, cdsEstablishmentAddressGen)

  def subscriptionDisplayResponseGen: Gen[SubscriptionDisplayResponse] =
    responseDetailGen.map(SubscriptionDisplayResponse(_))

  def ETMPErrorGen: Gen[ETMPError] = errorDetailGen.map(ETMPError(_))

  def ETMPSubscriptionDisplayResponseGen: Gen[ETMPSubscriptionDisplayResponse] =
    subscriptionDisplayResponseGen.map(ETMPSubscriptionDisplayResponse(_))

  def applicationContactsDetailsGen: Gen[ApplicationContactDetails] = for {
    name  <- stringsWithMaxLength(512)
    email <- RegexpGen.from("^\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}$")
    phone <- RegexpGen.from("^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$")
  } yield ApplicationContactDetails(
    name = name,
    email = email,
    phone = phone
  )

  def supportingDocumentsGen: Gen[SupportingDocuments] = for {
    fileName       <- stringsWithMaxLength(512)
    downloadUrl    <- stringsWithMaxLength(512)
    isConfidential <- Arbitrary.arbitrary[Boolean]
    id              = UUID.randomUUID.toString
  } yield SupportingDocuments(
    files = Map(id -> SupportingDocument(fileName, downloadUrl, isConfidential))
  )

  def registeredDetailsCheckGen: Gen[RegisteredDetailsCheck] = for {
    registeredDetailsAreCorrect <- Arbitrary.arbitrary[Boolean]
    registeredDetails           <- ETMPSubscriptionDisplayResponseGen
  } yield {
    val responseDetail = registeredDetails.subscriptionDisplayResponse.responseDetail
    RegisteredDetailsCheck(
      value = registeredDetailsAreCorrect,
      eori = responseDetail.EORINo,
      name = responseDetail.CDSFullName,
      streetAndNumber = responseDetail.CDSEstablishmentAddress.streetAndNumber,
      city = responseDetail.CDSEstablishmentAddress.city,
      country = responseDetail.CDSEstablishmentAddress.countryCode,
      postalCode = responseDetail.CDSEstablishmentAddress.postalCode
    )
  }

  def userAnswersGen: Gen[UserAnswers] =
    for {
      importGoods                              <- Arbitrary.arbitrary[Boolean]
      registeredDetailsCheck                   <- registeredDetailsCheckGen
      applicationContactDetails                <- Gen.option(applicationContactsDetailsGen)
      valuationMethod                          <- Gen.oneOf(ValuationMethod.values)
      isThereASaleInvolved                     <- Gen.option(Arbitrary.arbitrary[Boolean])
      isSaleBetweenRelatedParties              <- Gen.option(Arbitrary.arbitrary[Boolean])
      areThereRestrictionsOnTheGoods           <- Gen.option(Arbitrary.arbitrary[Boolean])
      isTheSaleSubjectToConditions             <- Gen.option(Arbitrary.arbitrary[Boolean])
      descriptionOfGoods                       <- stringsWithMaxLength(512)
      hasCommodityCode                         <- Arbitrary.arbitrary[Boolean]
      commodityCode                            <- Gen.option(intsBelowValue(99999))
      haveTheGoodsBeenSubjectToLegalChallenges <- Arbitrary.arbitrary[Boolean]
      hasConfidentialInformation               <- Arbitrary.arbitrary[Boolean]
      doYouWantToUploadDocuments               <- Arbitrary.arbitrary[Boolean]
      supportingDocuments                      <- Gen.option(supportingDocumentsGen)
    } yield UserAnswers(
      importGoods = importGoods,
      checkRegisteredDetails = registeredDetailsCheck,
      applicationContactDetails = applicationContactDetails,
      valuationMethod = valuationMethod,
      isThereASaleInvolved = isThereASaleInvolved,
      isSaleBetweenRelatedParties = isSaleBetweenRelatedParties,
      areThereRestrictionsOnTheGoods = areThereRestrictionsOnTheGoods,
      isTheSaleSubjectToConditions = isTheSaleSubjectToConditions,
      descriptionOfGoods = descriptionOfGoods,
      hasCommodityCode = hasCommodityCode,
      commodityCode = commodityCode.map(_.toString),
      haveTheGoodsBeenSubjectToLegalChallenges = haveTheGoodsBeenSubjectToLegalChallenges,
      hasConfidentialInformation = hasConfidentialInformation,
      doYouWantToUploadDocuments = doYouWantToUploadDocuments,
      uploadSupportingDocument = supportingDocuments
    )

  def valuationRulingsApplicationGen: Gen[ValuationRulingsApplication] =
    for {
      userAnswers       <- userAnswersGen
      applicationNumber <- stringsWithMaxLength(32)
      localDateTime     <- localDateTimeGen
    } yield ValuationRulingsApplication(
      data = userAnswers,
      applicationNumber = applicationNumber,
      lastUpdated = localDateTime.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
    )

}
