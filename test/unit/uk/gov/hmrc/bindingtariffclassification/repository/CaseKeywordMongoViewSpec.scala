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

package uk.gov.hmrc.bindingtariffclassification.repository

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.api.{DB, ReadConcern}
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.mongo.MongoSpecSupport
import util.CaseData.{createCase, createKeyword}

import scala.concurrent.ExecutionContext.Implicits.global

class CaseKeywordMongoViewSpec
  extends BaseMongoIndexSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MongoSpecSupport
    with Eventually {
  self =>

  private val mongoDbProvider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val config = mock[AppConfig]
  private val repository = newMongoRepository
  private val view = newMongoAggregation

  private def newMongoRepository: CaseMongoRepository =
    new CaseMongoRepository(mongoDbProvider, new SearchMapper(config), new UpdateMapper)

  private def newMongoAggregation: CaseKeywordMongoView =
    new CaseKeywordMongoView(mongoDbProvider)

  private val keyword1 = createKeyword()
  private val keyword2 = createKeyword()
  private val keyword3 = createKeyword()


  private val caseWithKeywords: Case = createCase(
    keywords = Set(keyword1, keyword2, keyword3)
  )

  private val caseHeader = CaseHeader(reference = "ref", None, None, None,
    ApplicationType.BTI, CaseStatus.REJECTED)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  private def collectionSize: Int =
    await(
      repository.collection
        .count(selector = None, limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
    ).toInt

  private val pagination = Pagination(
    page = 2,
    pageSize = 11
  )

  "fetchKeywordsFromCases" should {
    "return None when there is no matching attachment" in {
      await(repository.insert(caseWithKeywords))
      collectionSize shouldBe 1

      await(view.fetchKeywordsFromCases(pagination)) shouldBe None
    }

    "return keywords from the Cases" in {
      await(repository.insert(caseWithKeywords))
      collectionSize shouldBe 1

      await(view.fetchKeywordsFromCases(pagination)) shouldBe Some(Paged(CaseKeyword(keyword1, List(caseHeader))))
    }
  }
}