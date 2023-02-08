package uk.gov.hmrc.advancevaluationrulings.controllers

import play.api.http.Status
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MicroserviceHelloWorldControllerSpec extends AnyWordSpec with Matchers {

  private val fakeRequest = FakeRequest("GET", "/")
  private val controller  = new MicroserviceHelloWorldController(Helpers.stubControllerComponents())

  "GET /" should {
    "return 200" in {
      val result = controller.hello()(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
