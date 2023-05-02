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
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{CDSEstablishmentAddress, ContactInformation}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{CachedTraderDetails, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global

class TraderDetailsRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CachedTraderDetails]
    with MockitoSugar
    with OptionValues
    with ScalaFutures {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.traderDetailsTtlInSeconds) thenReturn 60

  val traderDetails = TraderDetailsResponse(
    "EORINoValue",
    "CDSFullNameValue",
    CDSEstablishmentAddress(
      "streetAndNumberValue",
      "cityValue",
      "countryCodeValue",
      Some("postalCodeValue")
    ),
    true,
    Some(ContactInformation(
      Some("personOfContactValue"),
      Some(true),
      Some("streetAndNumberValue"),
      Some("cityValue"),
      Some("postalCodeValue"),
      Some("countryCodeValue"),
      Some("telephoneNumberValue"),
      Some("faxNumberValue"),
      Some("emailAddressValue"),
      Some("emailVerificationTimestampValue")
    ))
  )

  val cachedTraderDetails = CachedTraderDetails(
    index = traderDetails.EORINo,
    data = traderDetails,
    lastUpdated = instant.minusSeconds(60)
  )

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  private val configuration = Configuration("crypto.key" -> aesKey)

  private implicit val crypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  protected override val repository = new TraderDetailsRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied trader details answers to `now`, and save them" in {

      val expectedResult = traderDetails

      val setResult = repository.set(traderDetails).futureValue
      val updatedRecord = find(
          Filters.equal("index", traderDetails.EORINo)
      ).futureValue.headOption.value

      setResult mustEqual Done
      updatedRecord.data mustEqual expectedResult
    }

    "must store the data section as encrypted bytes" in {

      repository.set(traderDetails).futureValue

      val record = repository.collection
        .find[BsonDocument](
            Filters.equal("index", traderDetails.EORINo)
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

  ".get (by EORI)" - {

    "when there is a record for this user id and draft id" - {

      "must update the lastUpdated time and get the record" in {
         val data = CachedTraderDetails(
            index = traderDetails.EORINo,
            data = traderDetails,
            lastUpdated = instant.minusSeconds(60)
         )

        insert(data).futureValue

        repository.get(traderDetails.EORINo).futureValue

        val result = find(
          Filters.equal("index", traderDetails.EORINo)
        ).futureValue.headOption.value

        result.lastUpdated mustEqual instant
      }
    }

    "when there is a record for this user id with a different draft id" - {

      "must return None" in {
        repository.set(traderDetails).futureValue

        val result = repository.get("NotEoriValue").futureValue

        result must not be defined
      }
    }

  }

  ".clear" - {

    "must remove a record" in {
      insert(cachedTraderDetails).futureValue

      val deleteResult = repository.clear(traderDetails.EORINo).futureValue

      val lookupResult = find(
        Filters.equal("index", traderDetails.EORINo)
      ).futureValue.headOption

      deleteResult mustEqual Done
      lookupResult must not be defined
    }

    "must return Done when there is no record to remove" in {
      val result = repository.clear("user id that does not exist").futureValue

      result mustEqual Done
    }
  }

}
