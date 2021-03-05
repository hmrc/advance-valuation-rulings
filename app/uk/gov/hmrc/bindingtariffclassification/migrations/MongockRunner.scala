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

package uk.gov.hmrc.bindingtariffclassification.migrations

import com.github.cloudyrock.mongock.driver.mongodb.sync.v4.driver.MongoSync4Driver
import com.github.cloudyrock.standalone.MongockStandalone
import com.github.cloudyrock.standalone.event.StandaloneMigrationSuccessEvent
import com.mongodb.client.{MongoClient, MongoClients}
import com.mongodb.connection.ClusterType
import javax.inject.Inject
import uk.gov.hmrc.bindingtariffclassification.common.Logging
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig

import scala.concurrent.Promise

class MongockRunner @Inject() (appConfig: AppConfig) extends Logging {

  lazy val migrationCompleted = Promise[StandaloneMigrationSuccessEvent]

  private lazy val mongockRunner = {

    val mongoClient: MongoClient = MongoClients.create(appConfig.mongodbUri)

    val mongockDriver = {
      val driver = MongoSync4Driver.withDefaultLock(mongoClient, appConfig.appName)
      if (mongoClient.getClusterDescription().getType() == ClusterType.STANDALONE) {
        driver.disableTransaction()
      }
      driver
    }

    MongockStandalone
      .builder()
      .setDriver(mongockDriver)
      .addChangeLogsScanPackage("uk.gov.hmrc.bindingtariffclassification.migrations.changelogs")
      .setMigrationStartedListener(() => logger.info("Started mongock migrations"))
      .setMigrationSuccessListener { successEvent =>
        logger.info("Finished mongock migrations successfully")
        migrationCompleted.success(successEvent)
      }
      .setMigrationFailureListener { failureEvent =>
        logger.error("Mongock migrations failed", failureEvent.getException)
        migrationCompleted.failure(failureEvent.getException)
      }
      .buildRunner()
  }

  mongockRunner.execute()
}
