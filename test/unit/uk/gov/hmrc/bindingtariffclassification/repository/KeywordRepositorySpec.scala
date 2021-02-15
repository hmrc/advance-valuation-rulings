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
import reactivemongo.bson.{BSONDocument, _}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.bindingtariffclassification.model.RESTFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global


class KeywordRepositorySpec extends BaseMongoIndexSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MongoSpecSupport
  with Eventually {
  self =>
  private val mongoErrorCode = 11000

  private val mongoDbProvider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val repository = createMongoRepo

  private def createMongoRepo =
    new KeywordsMongoRepository(mongoDbProvider)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
    collectionSize shouldBe 0
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  private def collectionSize: Int =
    await(
      repository.collection
        .count(
          selector = None,
          limit = Some(0),
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )
    ).toInt


  "insert" should {
    val keyword = Keyword(
      "keyword name", approved = true
    )

    "insert a new document in the collection" in {
      val size = collectionSize

      await(repository.insert(keyword)) shouldBe keyword
      collectionSize shouldBe 1 + size
      await(repository.collection.find(selectorByName(keyword.name)).one[Keyword]) shouldBe Some(
        keyword
      )
    }

    "fail to insert an existing document in the collection" in {
      await(repository.insert(keyword)) shouldBe keyword
      val size = collectionSize

      val caught = intercept[DatabaseException] {
        await(repository.insert(keyword))
      }

      caught.code shouldBe Some(mongoErrorCode)
      collectionSize shouldBe size
    }
  }



  private def selectorByName(name: String) =
    BSONDocument("name" -> name)

}
