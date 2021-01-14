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

package uk.gov.hmrc.bindingtariffclassification.config

import java.time.{LocalTime, ZoneOffset}

import play.api.Configuration
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec

import scala.concurrent.duration._

class AppConfigTest extends BaseSpec {

  private def configWith(pairs: (String, String)*): AppConfig =
    new AppConfig(Configuration.from(pairs.map(e => e._1 -> e._2).toMap), serviceConfig)

  "Config" should {

    "build 'clock'" in {
      configWith().clock.getZone shouldBe ZoneOffset.UTC
    }

    "build 'isTestMode'" in {
      configWith("testMode" -> "true").isTestMode  shouldBe true
      configWith("testMode" -> "false").isTestMode shouldBe false
    }

    "build 'ActiveDaysElapsedConfig'" in {
      val config = configWith(
        "scheduler.active-days-elapsed.enabled"  -> "true",
        "scheduler.active-days-elapsed.run-time" -> "00:00",
        "scheduler.active-days-elapsed.interval" -> "1d"
      ).activeDaysElapsed
      config.enabled    shouldBe true
      config.elapseTime shouldBe LocalTime.of(0, 0, 0)
      config.interval   shouldBe 1.days
    }

    "build 'ReferredDaysElapsedConfig'" in {
      val config = configWith(
        "scheduler.referred-days-elapsed.enabled"  -> "true",
        "scheduler.referred-days-elapsed.run-time" -> "00:00",
        "scheduler.referred-days-elapsed.interval" -> "1d"
      ).referredDaysElapsed
      config.enabled    shouldBe true
      config.elapseTime shouldBe LocalTime.of(0, 0, 0)
      config.interval   shouldBe 1.days
    }

    "build 'fileStoreCleanupConfig'" in {
      val config = configWith(
        "scheduler.filestore-cleanup.enabled"  -> "true",
        "scheduler.filestore-cleanup.run-time" -> "03:00",
        "scheduler.filestore-cleanup.interval" -> "7d"
      ).fileStoreCleanup
      config.enabled    shouldBe true
      config.elapseTime shouldBe LocalTime.of(3, 0, 0)
      config.interval   shouldBe 7.days
    }

    "build 'fileStoreUrl'" in {
      //take expected from application.conf
      configWith().fileStoreUrl shouldBe "http://localhost:9583"
    }

    "build 'bankHolidaysUrl'" in {
      //take expected from application.conf
      configWith().bankHolidaysUrl shouldBe "http://localhost:9587"
    }

    "build 'upsert-permitted-agents'" in {
      configWith("upsert-permitted-agents" -> "x,y").upsertAgents shouldBe Seq("x", "y")
      configWith("upsert-permitted-agents" -> "").upsertAgents    shouldBe Seq.empty
    }

    "build 'mongoEncryption' with default" in {
      configWith().mongoEncryption shouldBe MongoEncryption()
    }

    "build 'mongoEncryption' with false" in {
      configWith(
        "mongodb.encryption.enabled" -> "false"
      ).mongoEncryption shouldBe MongoEncryption()
    }

    "build 'mongoEncryption' with value not boolean" in {
      configWith(
        "mongodb.encryption.key" -> "ABC"
      ).mongoEncryption shouldBe MongoEncryption()
    }

    "build 'mongoEncryption' with value false and key ABC" in {
      configWith(
        "mongodb.encryption.enabled" -> "false",
        "mongodb.encryption.key"     -> "ABC"
      ).mongoEncryption shouldBe MongoEncryption()
    }

    "build 'mongoEncryption' with value true and key ABC" in {
      configWith(
        "mongodb.encryption.enabled" -> "true",
        "mongodb.encryption.key"     -> "ABC"
      ).mongoEncryption shouldBe MongoEncryption(enabled = true, key = Some("ABC"))
    }

    "build 'mongoEncryption' with value true and without key" in {
      val caught = intercept[RuntimeException] {
        configWith(
          "mongodb.encryption.enabled" -> "true"
        ).mongoEncryption
      }
      caught.getMessage shouldBe s"Could not find config key 'mongodb.encryption.key'"
    }

    "build 'case reference configuration" in {
      configWith("atar-case-reference-offset"  -> "10").atarCaseReferenceOffset  shouldBe 10
      configWith("other-case-reference-offset" -> "20").otherCaseReferenceOffset shouldBe 20
    }

    "throw and exception when key is missing for duration" in {
      val keyToPass = "run-time"
      val caught = intercept[RuntimeException] {
        configWith().getDuration(keyToPass)
      }

      caught.getMessage shouldBe s"Could not find config key '$keyToPass'"
    }

  }

}
