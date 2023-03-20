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

import base.BaseSpec
import com.kenshoo.play.metrics.Metrics
import migrations.{AddKeywordsMigrationJob, AmendDateOfExtractMigrationJob, MigrationJobs}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.PlayRunners
import repository.{CaseMongoRepository, CaseRepository, EncryptedCaseMongoRepository}
import scheduler.{ActiveDaysElapsedJob, FileStoreCleanupJob, ReferredDaysElapsedJob, ScheduledJobs}
import util.{FixedTimeFixtures, TestMetrics}

import java.time.Clock

class ModuleTest extends AnyWordSpecLike with Matchers with BeforeAndAfterEach with PlayRunners with FixedTimeFixtures {

  private def app(conf: (String, Any)*): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[Metrics].toInstance(new TestMetrics))
      .configure(conf: _*)
      .build()

  "Module 'bind" should {
    "Bind encryption repository" in {
      val application: Application = app("mongodb.encryption.enabled" -> true)
      running(application) {
        application.injector.instanceOf[CaseRepository].isInstanceOf[EncryptedCaseMongoRepository] shouldBe true
      }
    }

    "Bind standard repository" in {
      val application: Application = app("mongodb.encryption.enabled" -> false)
      running(application) {
        application.injector.instanceOf[CaseRepository].isInstanceOf[CaseMongoRepository] shouldBe true
      }
    }

    "Bind standard repository by default" in {
      val application: Application = app()
      running(application) {
        application.injector
          .instanceOf[CaseRepository]
          .isInstanceOf[CaseMongoRepository] shouldBe true
      }
    }

    "Bind all scheduled jobs" in {
      val application: Application = app()
      running(application) {
        application.injector.instanceOf[ScheduledJobs].jobs shouldBe Set(
          application.injector.instanceOf[ActiveDaysElapsedJob],
          application.injector.instanceOf[ReferredDaysElapsedJob],
          application.injector.instanceOf[FileStoreCleanupJob]
        )
      }
    }

    "Bind all migration jobs" in {
      val application: Application = app()
      running(application) {
        application.injector.instanceOf[MigrationJobs].jobs shouldBe Set(
          application.injector.instanceOf[AmendDateOfExtractMigrationJob],
          application.injector.instanceOf[AddKeywordsMigrationJob]
        )
      }
    }
  }

}
