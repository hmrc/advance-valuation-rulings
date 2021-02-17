/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.controllers


import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.RESTFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.service.KeywordService
import uk.gov.hmrc.http.HttpVerbs
import util.CaseData

import scala.concurrent.Future._

class KeywordControllerSpec extends BaseSpec with BeforeAndAfterEach {

  override protected def beforeEach() =
    Mockito.reset(keywordService)

  private val newKeywordRequest: NewKeywordRequest = CaseData.createNewKeyword()
  private val keyword1: Keyword = CaseData.createKeyword()
  private val keyword2: Keyword = CaseData.createKeyword()

  private val keywordService = mock[KeywordService]
  private val appConfig = mock[AppConfig]

  private val fakeRequest = FakeRequest()

  private val controller = new KeywordController(appConfig, keywordService, mcc)

  "deleteKeyword" should {

    val req = FakeRequest(method = HttpVerbs.DELETE, path = "/keyword/name")

    "return 204 idf the test mode is enabled" in {
      when(keywordService.deleteKeyword(refEq("name"))).thenReturn(successful(()))

      val result = await(controller.deleteKeyword("name")(req))

      status(result) shouldEqual NO_CONTENT
    }

    "return 500 when an error occurred" in {
      val error = new RuntimeException

      when(keywordService.deleteKeyword(refEq("name"))).thenReturn(failed(error))

      val result = await(controller.deleteKeyword("name")(req))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }


  "addKeyword" should {

    "return 201 when the keyword has been created successfully" in {
      when(keywordService.addKeyword(any[Keyword])).thenReturn(successful(keyword1))

      val result = await(controller.addKeyword()(fakeRequest.withBody(toJson(newKeywordRequest))))

      status(result) shouldEqual CREATED
      jsonBodyOf(result) shouldEqual toJson(keyword1)
    }

    "return 400 when the JSON request payload is not a Keyword" in {
      val body = """{"a":"b"}"""
      val result = await(controller.addKeyword()(fakeRequest.withBody(toJson(body))))

      status(result) shouldEqual BAD_REQUEST
    }

    "return 500 when an error occurred" in {
      val error = new DatabaseException {
        override def originalDocument: Option[BSONDocument] = None

        override def code: Option[Int] = Some(11000)

        override def message: String = "duplicate value for db index"
      }

      when(keywordService.addKeyword(any[Keyword])).thenReturn(failed(error))

      val result = await(controller.addKeyword()(fakeRequest.withBody(toJson(newKeywordRequest))))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }

  "approveKeyword" should {

    "return 200 when the keyword has been updated/approved successfully" in {
      when(keywordService.approveKeyword(keyword1, false)).thenReturn(successful(Some(keyword1)))

      val result = await(controller.approveKeyword(keyword1.name)(fakeRequest.withBody(toJson(keyword1))))

      status(result) shouldEqual OK
      jsonBodyOf(result) shouldEqual toJson(keyword1)
    }

    "return 400 when the JSON request payload is invalid" in {
      val body = """{"a":"b"}"""
      val result = await(controller.approveKeyword("")(fakeRequest.withBody(toJson(body))))

      status(result) shouldEqual BAD_REQUEST
    }

    "return 404 when there are no keywords with the provided name" in {
      val keyword3 = Keyword("not in the list")
      when(keywordService.approveKeyword(keyword1, false)).thenReturn(successful(None))

      val result = await(controller.approveKeyword(keyword1.name)(fakeRequest.withBody(toJson(keyword3))))

      status(result) shouldEqual NOT_FOUND
      jsonBodyOf(result).toString() shouldEqual """{"code":"NOT_FOUND","message":"Keyword not found"}"""
    }

    "return 500 when an error occurred" in {
      val error = new RuntimeException
      val keyword3 = Keyword("not in the list")

      when(keywordService.approveKeyword(keyword1, false)).thenReturn(failed(error))

      val result = await(controller.approveKeyword(keyword1.name)(fakeRequest.withBody(toJson(keyword3))))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }
}