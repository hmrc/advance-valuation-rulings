package uk.gov.hmrc.advancevaluationrulings

import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ErrorResponse, ValidationError, ValidationErrors}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.advancevaluationrulings.utils.{BaseIntegrationSpec, WireMockHelper}

import generators.ModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TraderDetailsEndpointSpec
    extends BaseIntegrationSpec
    with WireMockHelper
    with ModelGenerators {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  "Trader details endpoint" should {
    "respond with 200 status" in {
      ScalaCheckPropertyChecks.forAll(
        queryGen,
        ETMPSubscriptionDisplayResponseGen
      ) {
        (etmpQuery, etmpResponse) =>
          stubGet(
            url = etmpQueryUrl(etmpQuery),
            statusCode = 200,
            responseBody = Json.stringify(Json.toJson(etmpResponse)),
            requestHeaders = requestHeaders
          )

          val response = wsClient
            .url(traderDetailsRequestUrl(etmpQuery.acknowledgementReference, etmpQuery.EORI.value))
            .get()
            .futureValue

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
      ScalaCheckPropertyChecks.forAll(queryGen, ETMPErrorGen) {
        (etmpQuery, etmpErrorResponse) =>
          val errorCode    = etmpErrorResponse.errorDetail.errorCode
          val errorMessage = etmpErrorResponse.errorDetail.errorMessage

          stubGet(
            url = etmpQueryUrl(etmpQuery),
            statusCode = 500,
            responseBody = Json.stringify(Json.toJson(etmpErrorResponse)),
            requestHeaders = requestHeaders
          )

          val response = wsClient
            .url(traderDetailsRequestUrl(etmpQuery.acknowledgementReference, etmpQuery.EORI.value))
            .get()
            .futureValue

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
          ScalaCheckPropertyChecks.forAll(queryGen) {
            etmpQuery =>
              stubGet(
                url = etmpQueryUrl(etmpQuery),
                statusCode = 200,
                responseBody = etmpResponse,
                requestHeaders = requestHeaders
              )

              val response =
                wsClient
                  .url(
                    traderDetailsRequestUrl(
                      etmpQuery.acknowledgementReference,
                      etmpQuery.EORI.value
                    )
                  )
                  .get()
                  .futureValue

              response.status mustBe 500
              response.json mustBe Json.toJson(
                ErrorResponse(500, ValidationErrors(Seq(ValidationError(expectedError))))
              )
          }
        }
    }
  }
}
