package uk.gov.hmrc.advancevaluationrulings.repositories

import com.fasterxml.jackson.core.JsonParseException
import org.mockito.MockitoSugar
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.application.DraftSummary
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global

class UserAnswersRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with MockitoSugar
    with OptionValues
    with ScalaFutures {

  private val instant          = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val answers       =
    UserAnswers("userId", DraftId(1), Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.userAnswersTtlInDays) thenReturn 1

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  private val configuration = Configuration("crypto.key" -> aesKey)

  private implicit val crypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  protected override val repository: UserAnswersMongoRepository = new UserAnswersMongoRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = answers copy (lastUpdated = instant)

      val setResult     = repository.set(answers).futureValue
      val updatedRecord = find(
        Filters.and(
          Filters.equal("userId", answers.userId),
          Filters.equal("draftId", answers.draftId)
        )
      ).futureValue.headOption.value

      setResult mustEqual Done
      updatedRecord mustEqual expectedResult
    }

    "must store the data section as encrypted bytes" in {

      repository.set(answers).futureValue mustBe Done

      val record = repository.collection
        .find[BsonDocument](
          Filters.and(
            Filters.equal("userId", answers.userId),
            Filters.equal("draftId", answers.draftId)
          )
        )
        .headOption()
        .futureValue
        .value

      val json = Json.parse(record.toJson)
      val data = (json \ "data").as[String]

      assertThrows[JsonParseException] {
        Json.parse(data)
      }
    }
  }

  ".get (by user id and draft id)" - {

    "when there is a record for this user id and draft id" - {

      "must update the lastUpdated time and get the record" in {

        insert(answers).futureValue.wasAcknowledged() mustBe true

        val result         = repository.get(answers.userId, answers.draftId).futureValue
        val expectedResult = answers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is a record for this user id with a different draft id" - {

      "must return None" in {

        val differentAnswers = answers.copy(draftId = DraftId(2))

        insert(differentAnswers).futureValue.wasAcknowledged() mustBe true

        repository.get("userId", DraftId(1)).futureValue must not be defined
      }
    }

    "when there is a record for this draft id for a different user id" - {

      "must return None" in {

        val differentAnswers = answers.copy(userId = "another user id")

        insert(differentAnswers).futureValue.wasAcknowledged() mustBe true

        repository.get("userId", DraftId(1)).futureValue must not be defined
      }
    }

    "when there is no record for this user id and draft id" - {

      "must return None" in {

        repository.get("user id that does not exist", DraftId(2)).futureValue must not be defined
      }
    }
  }

  ".get (by draft id only)" - {

    "when there is a record for this draft id" - {

      "must get the record" in {

        insert(answers).futureValue.wasAcknowledged() mustBe true

        val result = repository.get(answers.draftId).futureValue

        result.value mustEqual answers
      }
    }

    "when there is no record for this user id and draft id" - {

      "must return None" in {

        repository.get(DraftId(2)).futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(answers).futureValue.wasAcknowledged() mustBe true

      val result = repository.clear(answers.userId, answers.draftId).futureValue

      result mustEqual Done
      repository.get(answers.userId, answers.draftId).futureValue must not be defined
    }

    "must return Done when there is no record to remove" in {

      repository.clear("user id that does not exist", DraftId(2)).futureValue mustEqual Done
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now`" in {

        insert(answers).futureValue.wasAcknowledged() mustBe true

        repository.keepAlive(answers.userId, answers.draftId).futureValue mustBe Done

        val expectedUpdatedAnswers = answers copy (lastUpdated = instant)

        val updatedAnswers = find(
          Filters.and(
            Filters.equal("userId", answers.userId),
            Filters.equal("draftId", answers.draftId)
          )
        ).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must succeed" in {

        repository.keepAlive("user id that does not exist", DraftId(2)).futureValue mustEqual Done
      }
    }
  }

  ".summaries" - {

    "must return summaries of all records belonging this user" in {

      val answers2 = answers.copy(draftId = DraftId(2))
      val answers3 = answers.copy(userId = "other user id", draftId = DraftId(3))

      insert(answers).futureValue.wasAcknowledged() mustBe true
      insert(answers2).futureValue.wasAcknowledged() mustBe true
      insert(answers3).futureValue.wasAcknowledged() mustBe true

      val result = repository.summaries("userId").futureValue

      result must contain theSameElementsAs Seq(
        DraftSummary(DraftId(1), None, Instant.ofEpochSecond(1), None),
        DraftSummary(DraftId(2), None, Instant.ofEpochSecond(1), None)
      )
    }
  }
}
