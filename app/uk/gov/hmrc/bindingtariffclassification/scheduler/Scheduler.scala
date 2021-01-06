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

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.JobRunEvent
import uk.gov.hmrc.bindingtariffclassification.repository.SchedulerLockRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.concurrent.duration.FiniteDuration

@Singleton
class Scheduler @Inject() (
  actorSystem: ActorSystem,
  appConfig: AppConfig,
  schedulerLockRepository: SchedulerLockRepository,
  schedulerDateUtil: SchedulerDateUtil,
  scheduledJobs: ScheduledJobs
) {
  private lazy val logger: Logger = Logger(this.getClass)

  scheduledJobs.jobs.foreach { job =>
    logger.info(
      s"Scheduling job [${job.name}] to run periodically at [${job.firstRunTime}] with interval [${job.interval.length} ${job.interval.unit}]"
    )
    actorSystem.scheduler.schedule(
      durationUntil(nextRunDateFor(job)),
      job.interval,
      new Runnable() {
        override def run(): Unit = {
          val event = JobRunEvent(job.name, closestRunDateFor(job))
          logger.info(s"Scheduled Job [${job.name}]: Acquiring Lock")
          schedulerLockRepository.lock(event).flatMap {
            case true =>
              logger.info(s"Scheduled Job [${job.name}]: Successfully acquired lock. Starting Job.")
              job.execute().map { _ =>
                logger.info(s"Scheduled Job [${job.name}]: Completed Successfully")
              } recover {
                case t: Throwable =>
                  logger.error(s"Scheduled Job [${job.name}]: Failed", t)
              }
            case false =>
              logger.info(s"Scheduled Job [${job.name}]: Failed to acquire Lock. It may have been running already.")
              successful(())
          }
        }
      }
    )
  }

  def execute[T](clazz: Class[T]): Future[Unit] =
    Future.sequence(scheduledJobs.jobs.filter(clazz.isInstance(_)).map(_.execute())).map(_ => ())

  private def nextRunDateFor(job: ScheduledJob): Instant =
    schedulerDateUtil.nextRun(job.firstRunTime, job.interval)

  private def closestRunDateFor(job: ScheduledJob): Instant =
    schedulerDateUtil.closestRun(job.firstRunTime, job.interval)

  private def durationUntil(datetime: Instant): FiniteDuration = {
    val now = Instant.now(appConfig.clock)

    if (datetime.isBefore(now))
      throw new IllegalArgumentException(s"Expected a future or present datetime but was [$datetime]")
    else FiniteDuration(now.until(datetime, ChronoUnit.SECONDS), TimeUnit.SECONDS)
  }

}
