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
import uk.gov.hmrc.bindingtariffclassification.model.Case
import uk.gov.hmrc.mongo.MongoSpecSupport
import util.CaseData.{createAttachment, createBTIApplicationWithAllFields, createCase, createDecision}

import scala.concurrent.ExecutionContext.Implicits.global

class CaseAttachmentViewSpec
    extends BaseMongoIndexSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MongoSpecSupport
    with Eventually {
  self =>

  private val mongoDbProvider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val config     = mock[AppConfig]
  private val repository = newMongoRepository
  private val view       = newMongoView

  private def newMongoRepository: CaseMongoRepository =
    new CaseMongoRepository(mongoDbProvider, new SearchMapper(config))

  private def newMongoView: CaseAttachmentMongoView =
    new CaseAttachmentMongoView(mongoDbProvider)

  private val attachment1           = createAttachment
  private val attachment2           = createAttachment
  private val attachment3           = createAttachment
  private val applicationPdf        = createAttachment
  private val decisionPdf           = createAttachment
  private val letterOfAuthorization = createAttachment

  private val caseWithAttachments: Case = createCase(
    attachments = Seq(attachment1, attachment2, attachment3),
    app = createBTIApplicationWithAllFields(
      applicationPdf        = Some(applicationPdf),
      letterOfAuthorization = Some(letterOfAuthorization)
    ),
    decision = Some(createDecision(decisionPdf = Some(decisionPdf)))
  )

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

  "find" should {
    "return None when there is no matching attachment" in {
      await(repository.insert(caseWithAttachments))
      collectionSize shouldBe 1

      await(view.find("not-an-id")) shouldBe None
    }

    "return attachments from the attachments field" in {
      await(repository.insert(caseWithAttachments))
      collectionSize shouldBe 1

      await(view.find(attachment1.id)) shouldBe Some(attachment1)
      await(view.find(attachment2.id)) shouldBe Some(attachment2)
      await(view.find(attachment3.id)) shouldBe Some(attachment3)
    }

    "return attachments from the application.agent.letterOfAuthorization field" in {
      await(repository.insert(caseWithAttachments))
      collectionSize shouldBe 1

      await(view.find(letterOfAuthorization.id)) shouldBe Some(letterOfAuthorization)
    }

    "return attachments from the application.applicationPdf field" in {
      await(repository.insert(caseWithAttachments))
      collectionSize shouldBe 1

      await(view.find(applicationPdf.id)) shouldBe Some(applicationPdf)
    }

    "return attachments from the decision.decisionPdf field" in {
      await(repository.insert(caseWithAttachments))
      collectionSize shouldBe 1

      await(view.find(decisionPdf.id)) shouldBe Some(decisionPdf)
    }
  }

}
