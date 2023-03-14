package uk.gov.hmrc.advancevaluationrulings

import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.common.SubmissionSuccess
import uk.gov.hmrc.advancevaluationrulings.utils.BaseIntegrationSpec

import generators.ModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SubmitAnswersEndpointSpec extends BaseIntegrationSpec with ModelGenerators {

  "Submit Answers endpoint" should {
    "respond with 200 status" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        valuationRulingsApplication =>
          val response = wsClient
            .url(submitAnswersEndpoint)
            .post(Json.toJson(valuationRulingsApplication))
            .futureValue

          response.status mustBe 200
          response.json mustBe Json.toJson(SubmissionSuccess(acknowledged = true))
      }
    }

    "respond with 400 status when given an invalid application submission" in {
      val response = wsClient
        .url(submitAnswersEndpoint)
        .post(Json.toJson("{}"))
        .futureValue

      response.status mustBe 400
    }
  }
}
