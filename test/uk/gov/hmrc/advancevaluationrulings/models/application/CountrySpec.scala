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

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CountrySpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  ".fromCountryCode" - {

    "must return a country when given a known country code" in {

      forAll(Gen.oneOf(Country.allCountries)) {
        country => Country.fromCountryCode(country.code) mustEqual country
      }
    }

    "must return a country with name `Unknown` when given a country code that isn't known" in {

      val unknownCodes =
        arbitrary[String].suchThat(string => !Country.allCountries.map(_.code).contains(string))

      forAll(unknownCodes) {
        code => Country.fromCountryCode(code) mustEqual Country(code, "Unknown")
      }
    }
  }
}
