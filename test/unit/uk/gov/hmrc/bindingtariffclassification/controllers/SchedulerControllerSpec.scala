/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.scheduler.{ActiveDaysElapsedJob, ReferredDaysElapsedJob, Scheduler}
import uk.gov.hmrc.http.HttpVerbs

import scala.concurrent.Future.{failed, successful}

class SchedulerControllerSpec extends BaseSpec {

  private val appConfig = mock[AppConfig]
  private val scheduler = mock[Scheduler]

  private val fakeRequest = FakeRequest(method = HttpVerbs.PUT, path = "/scheduler/days-elapsed")

  private val controller = new SchedulerController(appConfig, scheduler, parser, mcc)

  "Increment Active Days Elapsed" should {

    "return 403 if the test mode is disabled" in {
      when(appConfig.isTestMode).thenReturn(false)
      val result = await(controller.incrementActiveDaysElapsed()(fakeRequest))

      status(result) shouldEqual FORBIDDEN
      jsonBodyOf(result).toString() shouldEqual s"""{"code":"FORBIDDEN","message":"You are not allowed to call ${fakeRequest.method} ${fakeRequest.path}"}"""
    }

    "return 204 if the test mode is enabled and the scheduler executed successfully" in {
      when(appConfig.isTestMode).thenReturn(true)
      when(scheduler.execute(classOf[ActiveDaysElapsedJob])).thenReturn(successful(()))

      val result = await(controller.incrementActiveDaysElapsed()(fakeRequest))
      status(result) shouldEqual NO_CONTENT
    }

    "return 500 when an error occurred" in {
      when(appConfig.isTestMode).thenReturn(true)
      when(scheduler.execute(classOf[ActiveDaysElapsedJob])).thenReturn(failed(new RuntimeException))

      val result = await(controller.incrementActiveDaysElapsed()(fakeRequest))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }

  "Increment Referred Days Elapsed" should {

    "return 403 if the test mode is disabled" in {
      when(appConfig.isTestMode).thenReturn(false)
      val result = await(controller.incrementReferredDaysElapsed()(fakeRequest))

      status(result) shouldEqual FORBIDDEN
      jsonBodyOf(result).toString() shouldEqual s"""{"code":"FORBIDDEN","message":"You are not allowed to call ${fakeRequest.method} ${fakeRequest.path}"}"""
    }

    "return 204 if the test mode is enabled and the scheduler executed successfully" in {
      when(appConfig.isTestMode).thenReturn(true)
      when(scheduler.execute(classOf[ReferredDaysElapsedJob])).thenReturn(successful(()))

      val result = await(controller.incrementReferredDaysElapsed()(fakeRequest))
      status(result) shouldEqual NO_CONTENT
    }

    "return 500 when an error occurred" in {
      when(appConfig.isTestMode).thenReturn(true)
      when(scheduler.execute(classOf[ReferredDaysElapsedJob])).thenReturn(failed(new RuntimeException))

      val result = await(controller.incrementReferredDaysElapsed()(fakeRequest))

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      jsonBodyOf(result).toString() shouldEqual """{"code":"UNKNOWN_ERROR","message":"An unexpected error occurred"}"""
    }

  }

}
