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

package uk.gov.hmrc.bindingtariffclassification.config

import java.time.{Clock, LocalTime}

import javax.inject._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{Duration, FiniteDuration}

@Singleton
class AppConfig @Inject()(
                           val configuration: Configuration,
                           config: ServicesConfig
                         ) {

  private def configNotFoundError(key: String): Nothing =
    throw new RuntimeException(s"Could not find config key '$key'")

  def getDuration(key: String): Duration =
    configuration.getOptional[String](key).map(Duration.create).getOrElse(configNotFoundError(key))

  lazy val isTestMode: Boolean = getBooleanConfig("testMode")

  lazy val caseReferenceStart: Long = configuration.get[Long]("case-reference-start")
  lazy val btiReferenceOffset: Long = configuration.get[Long]("bti-reference-offset")
  lazy val liabilityReferenceOffset: Long = configuration.get[Long]("liability-reference-offset")

  lazy val clock: Clock = Clock.systemUTC()

  lazy val activeDaysElapsed: JobConfig = JobConfig(
    LocalTime.parse(configuration.get[String]("scheduler.active-days-elapsed.run-time")),
    getDuration("scheduler.active-days-elapsed.interval").asInstanceOf[FiniteDuration]
  )

  lazy val referredDaysElapsed: JobConfig = JobConfig(
    LocalTime.parse(configuration.get[String]("scheduler.referred-days-elapsed.run-time")),
    getDuration("scheduler.referred-days-elapsed.interval").asInstanceOf[FiniteDuration]
  )

  lazy val authorization: String = configuration.get[String]("auth.api-token")

  private def getBooleanConfig(key: String, default: Boolean = false): Boolean = {
    configuration.getOptional[Boolean](key).getOrElse(default)
  }

  def bankHolidaysUrl: String = config.baseUrl("bank-holidays")

  lazy val upsertAgents: Seq[String] = configuration.get[String]("upsert-permitted-agents").split(",").filter(_.nonEmpty)

  def getString(key: String): String =
    configuration.getOptional[String](key).getOrElse(configNotFoundError(key))

  lazy val mongoEncryption: MongoEncryption = {
    val encryptionEnabled = getBooleanConfig("mongodb.encryption.enabled")
    val encryptionKey = {
      if (encryptionEnabled) Some(getString("mongodb.encryption.key"))
      else None
    }

    if (encryptionEnabled && encryptionKey.isDefined) Logger.info("Mongo encryption enabled")
    else Logger.info("Mongo encryption disabled")

    MongoEncryption(encryptionEnabled, encryptionKey)
  }

}

case class MongoEncryption(enabled: Boolean = false, key: Option[String] = None)
case class JobConfig(elapseTime: LocalTime, interval: FiniteDuration)
