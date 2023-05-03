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

package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import generators.ModelGenerators
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TraderDetailsResponseSpec extends AnyFreeSpec with Matchers with ModelGenerators {

  val sampleResponseDetail = responseDetailGen.sample.get

  "apply (ResponseDetail)" - {

    "when the consentToDisclosureOfPersonalData" - {

      "is defined and 1, then consentToDisclosureOfPersonalData is true" in {
        val responseDetail = sampleResponseDetail.copy(consentToDisclosureOfPersonalData = Some("1"))

        val result = TraderDetailsResponse(responseDetail)

        result.consentToDisclosureOfPersonalData mustEqual true
      }

      "is defined and 0, then consentToDisclosureOfPersonalData is false" in {
        val responseDetail = sampleResponseDetail.copy(consentToDisclosureOfPersonalData = Some("0"))

        val result = TraderDetailsResponse(responseDetail)

        result.consentToDisclosureOfPersonalData mustEqual false
      }

      "is None, then consentToDisclosureOfPersonalData is false" in {
        val responseDetail = sampleResponseDetail.copy(consentToDisclosureOfPersonalData = None)

        val result = TraderDetailsResponse(responseDetail)

        result.consentToDisclosureOfPersonalData mustEqual false
      }

    }

  }

}
