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

import org.mockito.BDDMockito.`given`
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.api.DB
import uk.gov.hmrc.bindingtariffclassification.base.BaseSpec
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.{Keyword, Pagination}
import uk.gov.hmrc.bindingtariffclassification.repository.{CaseKeywordMongoView, KeywordsMongoRepository, MongoDbProvider}
import uk.gov.hmrc.bindingtariffclassification.service.KeywordService
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source


class MongockSpec extends BaseSpec with MongoSpecSupport with BeforeAndAfterEach with BeforeAndAfterAll {
  self =>

  private val mongoDbProvider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val config = mock[AppConfig]
  private val repository = newMongoRepository

  private def newMongoRepository: KeywordsMongoRepository =
    new KeywordsMongoRepository(mongoDbProvider)


  //  override lazy val app =
  //    GuiceApplicationBuilder()
  //      .configure(
  //        "metrics.jvm"     -> false,
  //        "metrics.enabled" -> false
  //      )
  //      .overrides(bind[AppConfig].to(config), bind[MongoDbProvider].to(mongoDbProvider))
  //      .disable[Scheduler]
  //      .disable[ScheduledJobs]
  //      .disable[MigrationJobs]
  //      .build()

  private val caseKeywordAggregation = mock[CaseKeywordMongoView]
  private val keywordService = new KeywordService(repository, caseKeywordAggregation)
  private val mongockRunner = mock[MongockRunner]

  override def beforeEach(): Unit = {
    super.beforeEach()
    given(config.appName).willReturn(databaseName)
    given(config.mongodbUri).willReturn(mongoUri)
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  "MongockRunner" should {
    val url = getClass.getClassLoader.getResource("keywords.txt")
    val keywords = (for (line <- Source.fromURL(url, "UTF-8").getLines()) yield line).toSeq

    "ensure that keywords have been added to the collection" in {
      await(mongockRunner.migrationCompleted.future)
      await(keywordService.findAll(Pagination())).results should contain theSameElementsAs keywords.map(Keyword(_))

    }
  }
}
