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

import uk.gov.hmrc.advancevaluationrulings.models.errors.{ErrorDetail, ETMPError, SourceFaultDetail}
import uk.gov.hmrc.advancevaluationrulings.models.etmp._

import org.scalacheck.Gen
import wolfendale.scalacheck.regexp.RegexpGen

trait ModelGenerators extends Generators {

  def regimeGen: Gen[Regime] = Gen.oneOf(Regime.values)

  def EORIGen: Gen[String] = RegexpGen.from("[1-9]\\d?(,\\d{3})+")

  def queryGen: Gen[Query] = for {
    regime                   <- regimeGen
    acknowledgementReference <- stringsWithMaxLength(32)
    eori                     <- EORIGen
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
    eoriNo                     <- RegexpGen.from("^[A-Z]{2}[0-9A-Z]+$")
    cdsFullName                <- stringsWithMaxLength(512)
    cdsEstablishmentAddressGen <- CDSEstablishmentAddressGen
  } yield ResponseDetail(eoriNo, cdsFullName, cdsEstablishmentAddressGen)

  def subscriptionDisplayResponseGen: Gen[SubscriptionDisplayResponse] =
    responseDetailGen.map(SubscriptionDisplayResponse(_))

  def ETMPErrorGen: Gen[ETMPError] = errorDetailGen.map(errorDetail => ETMPError(errorDetail))

  def ETMPSubscriptionDisplayResponseGen: Gen[ETMPSubscriptionDisplayResponse] =
    subscriptionDisplayResponseGen.map(ETMPSubscriptionDisplayResponse(_))
}
