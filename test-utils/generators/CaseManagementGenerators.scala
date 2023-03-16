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

import org.scalacheck.Gen
import uk.gov.hmrc.advancevaluationrulings.models.{CaseWorker, Contact, EORIDetails, Role, ValuationApplication}

trait CaseManagementGenerators {
  this: ModelGenerators =>

  def eoriDetailsGen: Gen[EORIDetails] = for {
    eori <- eoriNumberGen
    businessName <- stringsWithMaxLength(10)
    addressLine1 <- stringsWithMaxLength(10)
    addressLine2 <- stringsWithMaxLength(10)
    addressLine3 <- stringsWithMaxLength(10)
    postcode <- stringsWithMaxLength(10)
    country <- stringsWithMaxLength(10)
  } yield EORIDetails(
      eori,
      businessName,
      addressLine1,
      addressLine2,
      addressLine3,
      postcode,
      country
  )

  val contactGen: Gen[Contact] = for {
    name <- stringsWithMaxLength(10)
    email <- stringsWithMaxLength(10)
    phone <- Gen.option(stringsWithMaxLength(10))
  } yield Contact(name, email, phone)

  val valuationApplicationGen: Gen[ValuationApplication] = for {
    eoriDetails <- eoriDetailsGen
    contact <- contactGen
  } yield ValuationApplication(
    eoriDetails,
    contact,
    "goodNameValue",
    "goodDescriptionValue",
    None,
    None,
    None,
    None,
    None,
    None,
  )

  val caseWorkerGen: Gen[CaseWorker] = for {
    id <- stringsWithMaxLength(10)
    name <- stringsWithMaxLength(10)
    email <- stringsWithMaxLength(10)
    role = Role.CLASSIFICATION_OFFICER
  } yield CaseWorker(id, Some(name), Some(email), role)

}
