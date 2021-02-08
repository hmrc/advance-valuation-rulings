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
import reactivemongo.bson._
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global

class UsersRepositorySpec
    extends BaseMongoIndexSpec
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
    new UsersMongoRepository(mongoDbProvider)

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

  "get by id" should {

    "return existing user" in {
      val user = Operator(
        "user-id",
        Some("user"),
        Some("email"),
        Role.CLASSIFICATION_OFFICER
      )
      await(repository.insert(user))
      await(repository.getById("user-id")) shouldBe Some(user)
    }

    "return none if id not found" in {
      await(repository.getById("user-id")) shouldBe None
    }
  }

  "search" should {

    "retrieve all expected users from the collection by role" in {
      val user1 = Operator(
        "id1",
        Some("user"),
        Some("email"),
        Role.CLASSIFICATION_OFFICER
      )
      val user2 = Operator(
        "id2",
        Some("user"),
        Some("email"),
        Role.CLASSIFICATION_OFFICER
      )
      val user3 = Operator(
        "id3",
        Some("user"),
        Some("email"),
        Role.CLASSIFICATION_MANAGER
      )

      await(repository.insert(user1))
      await(repository.insert(user2))
      await(repository.insert(user3))
      collectionSize shouldBe 3

      await(
        repository.search(
          UserSearch(Some(Role.CLASSIFICATION_OFFICER), None),
          Pagination()
        )
      ) shouldBe
        Paged(Seq(user1, user2), Pagination(), 2)

      await(
        repository.search(
          UserSearch(Some(Role.CLASSIFICATION_MANAGER), None),
          Pagination()
        )
      ) shouldBe
        Paged(Seq(user3), Pagination(), 1)
    }

    //TODO: Fix below test, look into UserSearch for logic to retrieve users by Team
    /*
    "retrieve all expected users from the collection by team" in {

      val user1 = Operator(
        "id1",
        Some("user"),
        Role.CLASSIFICATION_OFFICER,
        List(Team("1", "act", List(ApplicationType.LIABILITY_ORDER), List()))
      )

      val user2 = Operator(
        "id2",
        Some("user"),
        Role.CLASSIFICATION_OFFICER,
        List(Team("2", "act", List(ApplicationType.LIABILITY_ORDER), List()))
      )

      await(repository.insert(user1))
      await(repository.insert(user2))
      collectionSize shouldBe 2

      await(repository.search(UserSearch(None, Some("act")), Pagination())) shouldBe
        Paged(Seq(user1, user2), Pagination(), 2)

    }
   */
  }

  "insert" should {
    val user = Operator(
      "user-id",
      Some("user"),
      Some("email"),
      Role.CLASSIFICATION_OFFICER
    )

    "insert a new document in the collection" in {
      val size = collectionSize

      await(repository.insert(user)) shouldBe user
      collectionSize shouldBe 1 + size
      await(repository.collection.find(selectorById(user.id)).one[Operator]) shouldBe Some(
        user
      )
    }

    "fail to insert an existing document in the collection" in {
      await(repository.insert(user)) shouldBe user
      val size = collectionSize

      val caught = intercept[DatabaseException] {
        await(repository.insert(user))
      }

      caught.code shouldBe Some(mongoErrorCode)
      collectionSize shouldBe size
    }
  }

  "update" should {
    val user = Operator(
      "user-id",
      Some("user"),
      Some("email"),
      Role.CLASSIFICATION_OFFICER
    )

    "modify an existing document in the collection" in {
      await(repository.insert(user)) shouldBe user

      val size = collectionSize

      val updatedUser = user.copy(
        name = Some("updated-name"),
        memberOfTeams = List("ACT", "ELM")
      )
      await(repository.update(updatedUser, upsert = false)) shouldBe Some(
        updatedUser
      )
      collectionSize shouldBe size

      await(
        repository.collection.find(selectorById(updatedUser.id)).one[Operator]
      ) shouldBe Some(updatedUser)
    }

    "do nothing when trying to update an unknown document" in {
      val size = collectionSize

      await(repository.update(user, upsert = false)) shouldBe None
      collectionSize shouldBe size
    }

    "upsert a new existing document in the collection" in {
      val size = collectionSize

      await(repository.update(user, upsert = true)) shouldBe Some(user)
      collectionSize shouldBe size + 1
    }
  }

  private def selectorById(id: String) =
    BSONDocument("id" -> id)

}
