package uk.gov.hmrc.advancevaluationrulings

import generators.ModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ErrorResponse, ValidationError, ValidationErrors}
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

    "return 500 status when ETMP returns an invalid schema response" in {
      ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen) {
        etmpRequest =>
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
            responseBody = "{}",
            requestHeaders = requestHeaders
          )

          val response =
            wsClient.url(traderDetailsEndpoint).post(Json.toJson(traderDetailsRequest)).futureValue

          response.status mustBe 500
          response.json mustBe Json.toJson(
            ErrorResponse(
              500,
              ValidationErrors(
                Seq(
                  ValidationError(
                    "Failed to parse json response. Error: /subscriptionDisplayResponse: error.path.missing"
                  )
                )
              )
            )
          )
      }
    }

    "return 500 status when ETMP returns an invalid json response" in {
      ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen) {
        etmpRequest =>
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
            responseBody = "invalid json",
            requestHeaders = requestHeaders
          )

          val response =
            wsClient.url(traderDetailsEndpoint).post(Json.toJson(traderDetailsRequest)).futureValue

          response.status mustBe 500
          response.json mustBe Json.toJson(
            ErrorResponse(
              500,
              ValidationErrors(
                Seq(
                  ValidationError(
                    "Failed to convert response to json. Error: Unrecognized token 'invalid': " +
                      "was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n " +
                      "at [Source: (String)\"invalid json\"; line: 1, column: 8]"
                  )
                )
              )
            )
          )
      }
    }

    "return 400 when given an invalid trader details request" in {
      ScalaCheckPropertyChecks.forAll(
        ETMPSubscriptionDisplayRequestGen,
        ETMPSubscriptionDisplayResponseGen
      ) {
        (etmpRequest, etmpResponse) =>
          stubPost(
            url = ETMPEndpoint,
            requestBody = Json.toJson(etmpRequest),
            statusCode = 200,
            responseBody = Json.stringify(Json.toJson(etmpResponse)),
            requestHeaders = requestHeaders
          )

          val response = wsClient.url(traderDetailsEndpoint).post(Json.toJson("{}")).futureValue

          response.status mustBe 400
          response.json mustBe Json.toJson(
            ErrorResponse(
              400,
              ValidationErrors(Seq(ValidationError("field at path: [] missing or invalid")))
            )
          )
      }
    }
  }
}
