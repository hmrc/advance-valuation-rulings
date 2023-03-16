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

package uk.gov.hmrc.advancevaluationrulings.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.controllers.ValuationCaseController.CreateValuationRequest

import java.time.Instant

class ValuationCaseSerializationSpec extends AnyWordSpec with Matchers {

  "json serialization" should {

     "return the same object after serialisation and deserialisation" in {

       val application: ValuationApplication = ValuationApplication(
         EORIDetails("an eori","ACME","10","High St","Cheltenham","KK1234LL","UK"),
         Contact("Jones","jones@orange.com"),
         "phone case",
         "a phone case"
       )
       val valuationCase = ValuationCase("reference", CaseStatus.NEW,Instant.now(), 0, application, 0)
       val formatted = Json.toJson(valuationCase)
       val result = Json.fromJson[ValuationCase](formatted)

       result.get shouldBe valuationCase
     }
  }
}
