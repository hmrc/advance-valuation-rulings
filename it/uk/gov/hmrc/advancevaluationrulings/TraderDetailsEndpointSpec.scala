package uk.gov.hmrc.advancevaluationrulings

import generators.ModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{TraderDetailsRequest, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.utils.{BaseIntegrationSpec, WireMockHelper}

class TraderDetailsEndpointSpec
    extends BaseIntegrationSpec
    with WireMockHelper
    with ModelGenerators {

  override def beforeAll(): Unit = startWireMock()
  override def afterAll(): Unit  = stopWireMock()

  "Trader details endpoint" should {
    "respond with 200 status" in {
      ScalaCheckPropertyChecks.forAll(
        ETMPSubscriptionDisplayRequestGen,
        ETMPSubscriptionDisplayResponseGen
      ) {
        (etmpRequest, etmpResponse) =>
          val traderDetailsRequest = TraderDetailsRequest(
            etmpRequest.params.date,
            etmpRequest.params.query.acknowledgementReference,
            etmpRequest.params.query.taxPayerID,
            etmpRequest.params.query.EORI
          )

          stubPost(
            url = ETMPEndpoint,
            requestBody = Json.toJson(etmpRequest),
            statusCode = 200,
            responseBody = Json.stringify(Json.toJson(etmpResponse)),
            requestHeaders = requestHeaders
          )

          val response =
            wsClient.url(traderDetailsEndpoint).post(Json.toJson(traderDetailsRequest)).futureValue

          response.status mustBe 200
          response.json mustBe Json.toJson(
            TraderDetailsResponse(
              etmpResponse.subscriptionDisplayResponse.responseDetail.EORINo,
              etmpResponse.subscriptionDisplayResponse.responseDetail.CDSFullName,
              etmpResponse.subscriptionDisplayResponse.responseDetail.CDSEstablishmentAddress
            )
          )
      }
    }
  }
}
