/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.repositories

import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.{CounterId, CounterWrapper}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}

import scala.concurrent.ExecutionContext.Implicits.global

class CounterRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CounterWrapper]
    with OptionValues
    with ScalaFutures
    with BeforeAndAfterEach {

  protected override val repository: CounterMongoRepository = new CounterMongoRepository(mongoComponent)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.seed.futureValue
  }

  "on startup" - {

    "must insert seed records when they do not already exist" in {

      findAll().futureValue must contain theSameElementsAs repository.seeds
    }
  }

  ".seed" - {

    "must not fail when records already exist" in {

      repository.seed.futureValue

      findAll().futureValue must contain theSameElementsAs repository.seeds
    }
  }

  ".ensureApplicationIdIsCorrect" - {

    "must update the application Id index when it is lower than the intended starting index" in {

      repository.seed.futureValue

      repository.collection
        .findOneAndUpdate(
          filter = Filters.eq("_id", CounterId.ApplicationId.toString),
          update = Updates.set("index", 1L),
          options = FindOneAndUpdateOptions()
            .upsert(true)
            .bypassDocumentValidation(false)
        )
        .toFuture()
        .futureValue

      repository.ensureApplicationIdIsCorrect().futureValue

      find(
        Filters.eq("_id", CounterId.ApplicationId.toString)
      ).futureValue.head.index mustBe repository.applicationStartingIndex
    }

    "must not update the application Id index when it is equal to or greater than the intended starting index" in {

      repository.seed.futureValue

      repository.collection
        .findOneAndUpdate(
          filter = Filters.eq("_id", CounterId.ApplicationId.toString),
          update = Updates.set("index", repository.applicationStartingIndex + 1),
          options = FindOneAndUpdateOptions()
            .upsert(true)
            .bypassDocumentValidation(false)
        )
        .toFuture()
        .futureValue

      repository.ensureApplicationIdIsCorrect().futureValue

      find(
        Filters.eq("_id", CounterId.ApplicationId.toString)
      ).futureValue.head.index mustBe repository.applicationStartingIndex + 1
    }

    "must return Done when no document with application ID exists in the collection" in {

      repository.seed.futureValue

      repository.collection
        .deleteOne(Filters.eq("_id", CounterId.ApplicationId.toString))
        .toFuture()
        .futureValue

      repository.ensureApplicationIdIsCorrect().futureValue mustBe Done

      find(
        Filters.eq("_id", CounterId.ApplicationId.toString)
      ).futureValue mustBe empty
    }
  }

  ".nextId" - {

    "must return sequential ids from the correct record" in {

      val applicationIdStartingValue: Long =
        repository.seeds.find(_._id == CounterId.ApplicationId).head.index
      val attachmentIdStartingValue: Long  =
        repository.seeds.find(_._id == CounterId.AttachmentId).head.index

      repository
        .nextId(CounterId.ApplicationId)
        .futureValue mustBe applicationIdStartingValue + 1
      repository
        .nextId(CounterId.ApplicationId)
        .futureValue mustBe applicationIdStartingValue + 2
      repository
        .nextId(CounterId.ApplicationId)
        .futureValue mustBe applicationIdStartingValue + 3

      repository.nextId(CounterId.AttachmentId).futureValue mustBe attachmentIdStartingValue + 1
      repository.nextId(CounterId.AttachmentId).futureValue mustBe attachmentIdStartingValue + 2
      repository.nextId(CounterId.AttachmentId).futureValue mustBe attachmentIdStartingValue + 3
    }
  }
}
