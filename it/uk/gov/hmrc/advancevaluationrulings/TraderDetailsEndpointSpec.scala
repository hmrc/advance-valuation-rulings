package uk.gov.hmrc.advancevaluationrulings

import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ErrorResponse, ValidationError, ValidationErrors}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{TraderDetailsRequest, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.utils.{BaseIntegrationSpec, WireMockHelper}

import generators.ModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

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

    "respond with 500 status when ETMP returns an error" in {
      ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen, ETMPErrorGen) {
        (etmpRequest, etmpErrorResponse) =>
          val traderDetailsRequest = TraderDetailsRequest(
            etmpRequest.params.date,
            etmpRequest.params.query.acknowledgementReference,
            etmpRequest.params.query.taxPayerID,
            etmpRequest.params.query.EORI
          )

          val errorCode    = etmpErrorResponse.errorDetail.errorCode
          val errorMessage = etmpErrorResponse.errorDetail.errorMessage

          stubPost(
            url = ETMPEndpoint,
            requestBody = Json.toJson(etmpRequest),
            statusCode = 500,
            responseBody = Json.stringify(Json.toJson(etmpErrorResponse)),
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
                    s"Error code: [$errorCode] with detail [${errorMessage.getOrElse("N/A")}]"
                  )
                )
              )
            )
          )
      }
    }

    val invalidETMPResponseScenarios = Table(
      ("testDescription", "etmpResponse", "expectedError"),
      (
        "invalid schema",
        "{}",
        "Failed to parse json response. Error: /subscriptionDisplayResponse: error.path.missing"
      ),
      (
        "invalid json",
        "invalid json",
        "Failed to convert response to json. Error: Unrecognized token 'invalid': " +
          "was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n " +
          "at [Source: (String)\"invalid json\"; line: 1, column: 8]"
      )
    )

    forAll(invalidETMPResponseScenarios) {
      (testDescription, etmpResponse, expectedError) =>
        s"return 500 status when ETMP returns an $testDescription response" in {
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
                responseBody = etmpResponse,
                requestHeaders = requestHeaders
              )

              val response =
                wsClient
                  .url(traderDetailsEndpoint)
                  .post(Json.toJson(traderDetailsRequest))
                  .futureValue

              response.status mustBe 500
              response.json mustBe Json.toJson(
                ErrorResponse(500, ValidationErrors(Seq(ValidationError(expectedError))))
              )
          }
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

          val invalidTraderDetailsRequest = Json.toJson("{}")
          val response = wsClient.url(traderDetailsEndpoint).post(invalidTraderDetailsRequest).futureValue

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
