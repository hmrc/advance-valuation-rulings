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

package uk.gov.hmrc.bindingtariffclassification.service


import org.mockito.ArgumentMatchers.refEq
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.repository._

import scala.concurrent.Future.successful

class KeywordServiceSpec extends BaseSpec with BeforeAndAfterEach {

  private val keyword = mock[Keyword]
  private val addedKeyword = mock[Keyword]

  private val appConfig = mock[AppConfig]
  private val keywordRepository = mock[KeywordsRepository]

  private val service =
    new KeywordService(appConfig, keywordRepository)

  private final val emulatedFailure = new RuntimeException("Emulated failure.")

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(keywordRepository, appConfig)
  }

  override protected def beforeEach(): Unit =
    super.beforeEach()

  "addKeyword" should {

    "return the keyword after it has being successfully added in the collection" in {
      when(keywordRepository.insert(keyword)).thenReturn(successful(addedKeyword))

      await(service.addKeyword(keyword)) shouldBe addedKeyword
    }

    "propagate any error" in {
      when(keywordRepository.insert(keyword)).thenThrow(emulatedFailure)

      val caught = intercept[RuntimeException] {
        await(service.addKeyword(keyword))
      }
      caught shouldBe emulatedFailure
    }
  }

  "updateKeyword" should {

    "return the keyword after it is updated in the database collection" in {
      when(keywordRepository.update(keyword, upsert = false))
        .thenReturn(successful(Some(addedKeyword)))

      await(service.updateKeyword(keyword, upsert = false)) shouldBe Some(addedKeyword)
    }

    "return None if the user does not exist in the database collection" in {
      when(keywordRepository.update(keyword, upsert = false))
        .thenReturn(successful(None))

      val result = await(service.updateKeyword(keyword, upsert = false))
      result shouldBe None
    }

    "propagate any error" in {
      when(keywordRepository.update(keyword, upsert = false))
        .thenThrow(emulatedFailure)

      val caught = intercept[RuntimeException] {
        await(service.updateKeyword(keyword, upsert = false))
      }
      caught shouldBe emulatedFailure
    }
  }

  "delete" should {
    "delete" in {
      when(keywordRepository.delete(refEq("name"))).thenReturn(successful(()))
      await(service.deleteKeyword(keyword.name) shouldBe ((): Unit))
      verify(keywordRepository, times(1)).delete(refEq("name"))
      verify()
    }

    "propagate any error" in {
      when(keywordRepository.delete(refEq("name"))).thenThrow(emulatedFailure)

      val caught = intercept[RuntimeException] {
        await(service.deleteKeyword(keyword.name))
      }
      caught shouldBe emulatedFailure
    }
  }
}
