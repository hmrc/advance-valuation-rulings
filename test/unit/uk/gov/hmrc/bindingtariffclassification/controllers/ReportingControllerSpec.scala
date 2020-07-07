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

import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.model.reporting.{CaseReport, ReportResult}
import uk.gov.hmrc.bindingtariffclassification.service.ReportService

import scala.concurrent.Future

class ReportingControllerSpec extends BaseSpec with BeforeAndAfterEach {

  private val reportService = mock[ReportService]

  private val fakeRequest = FakeRequest()
  private val controller = new ReportingController(reportService, mcc)

  override protected def afterEach(): Unit = {
    super.afterEach()
    Mockito.reset(reportService)
  }


  "GET report" should {
    "Delegate to service" in {
      val report = mock[CaseReport]

      given(reportService.generate(report)) willReturn Future.successful(Seq.empty[ReportResult])

      val result = await(controller.report(report)(fakeRequest))

      status(result) shouldBe OK
    }
  }

}
