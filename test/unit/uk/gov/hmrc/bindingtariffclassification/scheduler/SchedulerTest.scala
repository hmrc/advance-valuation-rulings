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

package uk.gov.hmrc.bindingtariffclassification.scheduler

import java.time._
import java.time.temporal.ChronoUnit

import akka.actor.{ActorSystem, Cancellable}
import cron4s.Cron
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{atLeastOnce, never, reset, verify}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.JobRunEvent
import uk.gov.hmrc.bindingtariffclassification.repository.SchedulerLockRepository
import util.TestMetrics

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.concurrent.ExecutionContext.Implicits.global

class SchedulerTest extends BaseSpec with BeforeAndAfterEach with Eventually {

  private val zone: ZoneId        = ZoneOffset.UTC
  private val schedulerRepository = mock[SchedulerLockRepository]
  private val job                 = mock[ScheduledJob]
  private val actorSystem         = mock[ActorSystem]
  private val internalScheduler   = mock[akka.actor.Scheduler]
  private val config              = mock[AppConfig]
  private val now                 = date("2018-12-25T12:00:00")
  private val clock               = Clock.fixed(now.toInstant, zone)
  private val util                = mock[SchedulerDateUtil]

  private def date(datetime: String) =
    LocalDateTime.parse(datetime).atZone(zone)

  private def givenTheLockSucceeds(): Unit =
    given(schedulerRepository.lock(any[JobRunEvent])).willReturn(successful(true))

  private def givenTheLockFails(): Unit =
    given(schedulerRepository.lock(any[JobRunEvent])).willReturn(successful(false))

  private def theLockEvent: JobRunEvent = {
    val captor: ArgumentCaptor[JobRunEvent] = ArgumentCaptor.forClass(classOf[JobRunEvent])
    verify(schedulerRepository).lock(captor.capture())
    captor.getValue
  }

  private def runTheJobImmediately: Answer[Cancellable] = (invocation: InvocationOnMock) => {
    val arg: Runnable = invocation.getArgument(1)
    if (arg != null) {
      arg.run()
    }
    Cancellable.alreadyCancelled
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    given(config.clock) willReturn clock
    given(actorSystem.scheduler) willReturn internalScheduler
    given(internalScheduler.scheduleOnce(any[Duration], any[Runnable])(any[ExecutionContext])) will runTheJobImmediately
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(schedulerRepository, job, internalScheduler, actorSystem, config)
  }

  private def theDelay: Duration = {
    val initialDelayCaptor: ArgumentCaptor[Duration] = ArgumentCaptor.forClass(classOf[Duration])
    verify(internalScheduler, atLeastOnce()).scheduleOnce(initialDelayCaptor.capture(), any[Runnable])(any[ExecutionContext])
    initialDelayCaptor.getValue()
  }

  "Scheduler" should {

    "Execute by class" in {
      givenTheLockFails() // Ensures the job isn't run by the scheduler
      given(config.clock).willReturn(Clock.fixed(now.toInstant, zone))

      val job1 = mock[ActiveDaysElapsedJob]
      given(job1.name) willReturn "name1"
      given(job1.nextRunTime).willReturn(Some(now)).willReturn(None)

      val job2 = mock[ReferredDaysElapsedJob]
      given(job2.name) willReturn "name2"
      given(job2.nextRunTime).willReturn(Some(now)).willReturn(None)

      // When
      whenTheSchedulerStarts(withJobs = Set(job1, job2)).execute(job1.getClass)

      // Then
      verify(job1).execute()
      verify(job2, never()).execute()
    }

    "Run job with valid schedule" in {
      // Given
      givenTheLockSucceeds()
      given(job.enabled) willReturn true
      given(job.name) willReturn "name"
      given(job.schedule) willReturn Cron.unsafeParse("0 0 12 * * ?")
      given(job.nextRunTime).willReturn(Some(now)).willReturn(None)

      // When
      whenTheSchedulerStarts()

      // Then
      theDelay shouldBe Duration.ZERO

      val lockEvent = theLockEvent
      lockEvent.name    shouldBe "name"
      lockEvent.runDate shouldBe date("2018-12-25T12:00:00")
    }

    "Run job with valid schedule and future run-date" in {
      // Given
      givenTheLockSucceeds()
      given(job.name) willReturn "name"
      given(job.enabled) willReturn true
      given(job.schedule) willReturn Cron.unsafeParse("0 20 12 * * ?")
      given(job.nextRunTime).willReturn(Some(now.plus(20, ChronoUnit.MINUTES))).willReturn(None)

      // When
      whenTheSchedulerStarts()

      // Then
      theDelay shouldBe Duration.ofMinutes(20)

      val lockEvent = theLockEvent
      lockEvent.name    shouldBe "name"
      lockEvent.runDate shouldBe date("2018-12-25T12:20:00")
    }

    "Schedule job immediately given a run date in the past" in {
      // Given
      givenTheLockSucceeds()
      given(job.name) willReturn "name"
      given(job.enabled) willReturn true
      given(job.schedule) willReturn Cron.unsafeParse("0 40 11 * * ?")
      given(job.nextRunTime).willReturn(Some(now.minus(20, ChronoUnit.MINUTES))).willReturn(None)

      // When
      whenTheSchedulerStarts()

      // Then
      theDelay shouldBe Duration.ZERO

      val lockEvent = theLockEvent
      lockEvent.name    shouldBe "name"
      lockEvent.runDate shouldBe date("2018-12-25T11:40:00")
    }

    "Not execute the job if the lock fails" in {
      // Given
      givenTheLockFails()
      given(job.enabled) willReturn true
      given(job.schedule) willReturn Cron.unsafeParse("0 0 12 * * ?")
      given(job.nextRunTime) willReturn Some(now)

      // When
      whenTheSchedulerStarts()

      verify(job, never()).execute()
    }

    "Not execute the job if disabled" in {
      // Given
      givenTheLockSucceeds()
      given(job.enabled) willReturn false
      given(job.name) willReturn "name"
      given(job.schedule) willReturn Cron.unsafeParse("0 0 12 * * ?")
      given(job.nextRunTime) willReturn Some(now)

      // When
      whenTheSchedulerStarts()

      verify(job, never()).execute()
    }

  }

  private def whenTheSchedulerStarts(withJobs: Set[ScheduledJob] = Set(job)): Scheduler =
    new Scheduler(actorSystem, config, schedulerRepository, util, ScheduledJobs(withJobs), new TestMetrics)
}
