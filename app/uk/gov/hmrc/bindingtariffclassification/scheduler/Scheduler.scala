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

package uk.gov.hmrc.bindingtariffclassification.scheduler

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.SchedulerRunEvent
import uk.gov.hmrc.bindingtariffclassification.repository.SchedulerLockRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class Scheduler @Inject()(actorSystem: ActorSystem,
                          appConfig: AppConfig,
                          schedulerLockRepository: SchedulerLockRepository,
                          schedulerDateUtil: SchedulerDateUtil,
                          job: ScheduledJob) {

  Logger.info(s"Scheduling job [${job.name}] to run periodically at [${job.firstRunTime}] with interval [${job.interval.length} ${job.interval.unit}]")
  actorSystem.scheduler.schedule(durationUntil(nextRunDate), job.interval, new Runnable() {
    override def run(): Unit = {
      execute()
    }
  })

  def execute(): Future[Unit] = {
    val event = SchedulerRunEvent(job.name, closestRunDate)

    Logger.info(s"Scheduled Job [${job.name}]: Acquiring Lock")
    schedulerLockRepository.lock(event).map {
      case true =>
        Logger.info(s"Scheduled Job [${job.name}]: Successfully acquired lock, Starting Job.")
        job.execute() map { _ =>
          Logger.info(s"Scheduled Job [${job.name}]: Completed Successfully")
        } recover {
          case t: Throwable => Logger.error(s"Scheduled Job [${job.name}]: Failed", t)
        }
      case false => Logger.info(s"Scheduled Job [${job.name}]: Failed to acquire Lock. It may have been running already.")
    }
  }

  private def nextRunDate: Instant = {
    schedulerDateUtil.nextRun(job.firstRunTime, job.interval)
  }

  private def closestRunDate: Instant = {
    schedulerDateUtil.closestRun(job.firstRunTime, job.interval)
  }

  private def durationUntil(date: Instant): FiniteDuration = {
    val now = Instant.now(appConfig.clock)

    if (!now.isAfter(date)) FiniteDuration(now.until(date, ChronoUnit.SECONDS), TimeUnit.SECONDS)
    else throw new IllegalArgumentException(s"Cannot calculate the duration until a date in the past [$date]")
  }

}
