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

package uk.gov.hmrc.advancevaluationrulings.config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ServiceSpec extends AnyFreeSpec with Matchers {

  "Service" - {
    "should return the correct base url" in {
      val service = Service("localhost", "8080", "http")

      service.baseUrl should be("http://localhost:8080")
    }

    "should return the correct string representation" in {
      val service = Service("localhost", "8080", "http")

      service.toString should be("http://localhost:8080")
    }

    "should return the correct string representation from Service class" in {
      val service = Service("localhost", "8080", "http")

      Service.convertToString(service) should be("http://localhost:8080")
    }
  }
}
