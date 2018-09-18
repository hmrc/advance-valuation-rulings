/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.uk.gov.hmrc.bindingtariffclassification.controllers

import play.api.http.HeaderNames.{CACHE_CONTROL, USER_AGENT}
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.bindingtariffclassification.controllers.MicroserviceHelloWorld
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class MicroserviceHelloWorldControllerSpec extends UnitSpec with WithFakeApplication{

  val fakeRequest = FakeRequest("GET", "/hello")
  val controller = new MicroserviceHelloWorld()

  "GET /hello" should {

    "return 400 when the User-Agent header is not sent" in {
      val result = controller.hello()(fakeRequest.withHeaders(CACHE_CONTROL -> "Y"))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 200 when the User-Agent header has a unique value" in {
      val result = controller.hello()(fakeRequest.withHeaders(USER_AGENT -> "unit test"))
      status(result) shouldBe Status.OK
    }

    "return 400 when the User-Agent header has multiple values" in {
      val headers = Seq(USER_AGENT -> "unit test 1", USER_AGENT -> "unit test 2", CACHE_CONTROL -> "Y")
      val result = controller.hello()(fakeRequest.withHeaders(headers: _*))
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

}
