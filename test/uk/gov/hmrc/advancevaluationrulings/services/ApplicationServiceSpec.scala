/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.services

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ApplicationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val mockCounterRepo = mock[CounterRepository]
  private val mockApplicationRepo = mock[ApplicationRepository]
  private val fixedInstant = Instant.now
  private val fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault())

  private val service = new ApplicationService(mockCounterRepo, mockApplicationRepo, fixedClock)

  override def beforeEach(): Unit = {
    Mockito.reset(mockCounterRepo)
    Mockito.reset(mockApplicationRepo)
    super.beforeEach()
  }

  "save" - {

    "must create an application and return its id" in {

      val id = 123L
      val applicantEori = "applicantEori"

      when(mockCounterRepo.nextId(eqTo(CounterId.ApplicationId))) thenReturn Future.successful(id)
      when(mockApplicationRepo.set(any())) thenReturn Future.successful(Done)

      val request = ApplicationRequest(EORIDetails("eori", "name", "line1", "line2", "line3", "postcode", "GB"))
      val expectedApplication = Application(ApplicationId(id), applicantEori, fixedInstant, fixedInstant)

      val result = service.save(applicantEori, request).futureValue

      result mustEqual ApplicationId(id)
      verify(mockCounterRepo, times(1)).nextId(eqTo(CounterId.ApplicationId))
      verify(mockApplicationRepo, times(1)).set(eqTo(expectedApplication))
    }
  }
}
