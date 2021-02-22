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

import java.time.{Duration, ZonedDateTime}
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.bindingtariffclassification.common.Logging
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.metrics.HasMetrics
import uk.gov.hmrc.bindingtariffclassification.model.JobRunEvent
import uk.gov.hmrc.bindingtariffclassification.repository.SchedulerLockRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful
import scala.util.control.NonFatal
import java.time.temporal.Temporal

@Singleton
class Scheduler @Inject() (
  actorSystem: ActorSystem,
  appConfig: AppConfig,
  schedulerLockRepository: SchedulerLockRepository,
  schedulerDateUtil: SchedulerDateUtil,
  scheduledJobs: ScheduledJobs,
  val metrics: Metrics
)(implicit ec: ExecutionContext) extends Logging
    with HasMetrics {

  val (enabledJobs, disabledJobs) = scheduledJobs.jobs.partition(_.enabled)

  disabledJobs.foreach(job => logger.warn(s"Scheduled job [${job.name}] is disabled"))

  enabledJobs.foreach { job =>
    logger.info(s"Scheduling job [${job.name}] with schedule [${job.schedule.toString}]")
    scheduleJob(job)
  }

  def scheduleJob(job: ScheduledJob): Unit =
    job.nextRunTime.map { nextRun =>
      actorSystem.scheduler.scheduleOnce(
        durationUntil(nextRun),
        runScheduledJob(nextRun, job)
      )
    }

  def runScheduledJob(runTime: ZonedDateTime, job: ScheduledJob): Runnable = { () =>
    logger.info(s"Scheduled Job [${job.name}]: Acquiring lock")

    val runJob = withMetricsTimerAsync(s"scheduled-job-${job.name}") { timer =>
      schedulerLockRepository.lock(JobRunEvent(job.name, runTime)).flatMap { acquiredLock =>
        if (acquiredLock) {
          logger.info(s"Scheduled Job [${job.name}]: Acquired lock")

          job.execute().map(_ => logger.info(s"Scheduled Job [${job.name}]: Completed successfully")).recover {
            case NonFatal(t) =>
              logger.error(s"Scheduled Job [${job.name}]: Failed", t)
              timer.completeWithFailure()
          }
        } else {
          logger.info(s"Scheduled Job [${job.name}]: Failed to acquire Lock")
          timer.completeWithFailure()
          successful(())
        }
      }
    }

    runJob.onComplete(_ => scheduleJob(job))
  }

  def execute[T](clazz: Class[T]): Future[Unit] =
    Future.sequence(scheduledJobs.jobs.filter(clazz.isInstance(_)).map(_.execute())).map(_ => ())

  private def durationUntil[A <: Temporal](nextRun: A): Duration = {
    val now      = appConfig.clock.instant()
    val duration = Math.max(0L, now.until(nextRun, ChronoUnit.MILLIS))
    Duration.ofMillis(duration)
  }
}
