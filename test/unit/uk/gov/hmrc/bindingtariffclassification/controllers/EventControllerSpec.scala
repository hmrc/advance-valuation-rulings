/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.stream.Materializer
import org.mockito.Mockito.{verifyNoMoreInteractions, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.JsonFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.service.{CaseService, EventService}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import util.EventData

import scala.concurrent.Future._

class EventControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with Matchers with BeforeAndAfterEach {

  private implicit val mat: Materializer = fakeApplication.materializer

  private val caseReference = UUID.randomUUID().toString
  private val id = UUID.randomUUID().toString

  private val e1: Event = EventData.createCaseStatusChangeEvent(caseReference)
  private val e2: Event = EventData.createNoteEvent(caseReference)

  private val eventService = mock[EventService]
  private val casesService = mock[CaseService]

  private val fakeRequest = FakeRequest()
  private val appConfig = mock[AppConfig]

  private val controller = new EventController(appConfig, eventService, casesService)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(casesService, eventService)
  }

  "deleteAll()" should {

    "return 403 if the delete mode is disabled" in {

      val result = await(controller.deleteAll()(fakeRequest))

      status(result) shouldEqual FORBIDDEN
      jsonBodyOf(result).toString() shouldEqual """{"code":"FORBIDDEN","message":"You are not allowed to delete."}"""
    }

    "return 204 if the delete mode is enabled" in {
      when(appConfig.isDeleteEnabled).thenReturn(true)
      when(eventService.deleteAll).thenReturn(successful(()))

      val result = await(controller.deleteAll()(fakeRequest))

      status(result) shouldEqual NO_CONTENT
    }

    "return 500 when an error occurred" in {
      val error = new RuntimeException

      when(appConfig.isDeleteEnabled).thenReturn(true)
      when(eventService.deleteAll).thenReturn(failed(error))

      val result = await(controller.deleteAll()(fakeRequest))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }

  "getByCaseReference()" should {

    "return 200 with the expected events" in {
      when(eventService.getByCaseReference(caseReference)).thenReturn(successful(Seq(e1, e2)))

      val result = await(controller.getByCaseReference(caseReference)(fakeRequest))

      status(result) shouldEqual OK
      jsonBodyOf(result) shouldEqual toJson(Seq(e1, e2))
    }

    "return 200 with an empty sequence when there are no events for a specific case" in {
      when(eventService.getByCaseReference(caseReference)).thenReturn(successful(Seq.empty))

      val result = await(controller.getByCaseReference(caseReference)(fakeRequest))

      status(result) shouldEqual OK
      jsonBodyOf(result) shouldEqual toJson(Seq.empty[Event])
    }

    "return 500 when an error occurred" in {
      val error = new RuntimeException

      when(eventService.getByCaseReference(caseReference)).thenReturn(failed(error))

      val result = await(controller.getByCaseReference(caseReference)(fakeRequest))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }

  "create" should {
    val note = Note(Some("note"))
    val timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
    val userId = "user-id"
    val newEvent = NewEventRequest(note, userId, timestamp)
    val event = Event(id = "id", details = note, userId = userId, caseReference = caseReference, timestamp = timestamp)

    "return 201 Created" in {
      val aCase = mock[Case]
      when(aCase.reference).thenReturn(caseReference)
      when(casesService.getByReference(caseReference)).thenReturn(successful(Some(aCase)))
      when(eventService.insert(ArgumentMatchers.any[Event])).thenReturn(successful(event))

      val request = fakeRequest.withBody(toJson(newEvent))
      val result: Result = await(controller.create(caseReference)(request))

      status(result) shouldEqual CREATED
      jsonBodyOf(result) shouldEqual toJson(event)
    }

    "return 404 Not Found for invalid Reference" in {
      when(casesService.getByReference(caseReference)).thenReturn(successful(None))

      val request = fakeRequest.withBody(toJson(newEvent))
      val result: Result = await(controller.create(caseReference)(request))

      verifyNoMoreInteractions(eventService)
      status(result) shouldEqual NOT_FOUND
      jsonBodyOf(result).toString() shouldEqual "{\"code\":\"NOT_FOUND\",\"message\":\"Case not found\"}"
    }
  }

}
