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

import java.time.{Duration, LocalTime}
import org.quartz.{CronExpression, Job, JobExecutionContext}
import uk.gov.hmrc.bindingtariffclassification.config.JobConfig
import uk.gov.hmrc.lock.LockKeeper

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration => ScalaDuration}

abstract class ScheduledJob(implicit ec: ExecutionContext) extends Job with LockKeeper {
  def jobConfig: JobConfig

  def execute(): Future[Unit]

  def name: String = jobConfig.name

  def enabled: Boolean = jobConfig.enabled

  def schedule: CronExpression = jobConfig.schedule

  def execute(context: JobExecutionContext): Unit = {
    // Quartz gives us no choice but to block here
    Await.result(tryLock(execute()), ScalaDuration.Inf)
  }
}
