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

import play.api.http.Status._
import play.api.http.{HttpVerbs, Status}
import scalaj.http.Http

class AuthSpec extends BaseFeatureSpec {

  protected val serviceUrl = s"http://localhost:$port"

  Feature("Authentication") {

    Scenario("Auth header present with correct value") {

      When("I call an endpoint with the correct auth token")
      val result = Http(s"$serviceUrl/cases")
        .header(apiTokenKey, appConfig.authorization)
        .method(HttpVerbs.GET)
        .asString

      Then("The response code should be 200")
      result.code shouldEqual OK
    }

    Scenario("Auth header present with incorrect value") {

      When("I call an endpoint with an incorrect auth token")
      val result = Http(s"$serviceUrl/cases")
        .header(apiTokenKey, "WRONG_TOKEN")
        .method(HttpVerbs.GET)
        .asString

      Then("The response code should be 403")
      result.code shouldEqual Status.FORBIDDEN
      result.body shouldBe "Missing or invalid 'X-Api-Token'"
    }

    Scenario("Auth header not present") {

      When("I call an endpoint with the no auth token")
      val result = Http(s"$serviceUrl/cases")
        .method(HttpVerbs.GET)
        .asString

      Then("The response code should be 403")
      result.code shouldEqual Status.FORBIDDEN
      result.body shouldBe "Missing or invalid 'X-Api-Token'"
    }

    Scenario("Calls to the health endpoint do not require auth token") {
      val result = Http(s"$serviceUrl/ping/ping")
        .method(HttpVerbs.GET)
        .asString

      Then("The response code should be 200")
      result.code shouldBe OK
    }
  }
}
