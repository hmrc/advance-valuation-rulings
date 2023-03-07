/*
 * Copyright 2023 HM Revenue & Customs
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

import org.bson.BsonObjectId
import org.mongodb.scala.model.{Filters, Updates}
import play.api.Logger
import uk.gov.hmrc.advancevaluationrulings.models.{CaseStatus, CaseWorker, ValuationCase}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.lang.System.Logger
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ValuationCaseRepository @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext
) extends PlayMongoRepository[ValuationCase](
  mongoComponent = mongo,
  collectionName = "mycollection",
  domainFormat   = ValuationCase.fmt,
  indexes        = Seq(/* IndexModel() instances, see Migrate index definitions below  */),
  replaceIndexes = false
){
  def assignCase(reference: String, caseWorker: CaseWorker): Future[Long] =
    for{
      result <- collection.updateOne(Filters.equal("reference", reference),
        Updates.combine(Updates.set("status", CaseStatus.REFERRED.toString),Updates.set("assignee", Codecs.toBson(caseWorker)))).toFuture()
    } yield {
      if(result.wasAcknowledged()) result.getModifiedCount  else throw new Exception("failed to assign case")
    }

  def create(valuation: ValuationCase): Future[BsonObjectId] = collection.insertOne(valuation).toFuture().map{ result =>
    if(result.wasAcknowledged()) result.getInsertedId.asObjectId() else throw new Exception("Failed to insert record")
  }

  def allOpenCases(): Future[List[ValuationCase]] =
              collection.find[ValuationCase](Filters.equal("status","OPEN")).toFuture().map(_.toList)

  def findByReference(reference: String): Future[Seq[ValuationCase]] =
             collection.find[ValuationCase](Filters.equal("reference", reference)).toFuture()

  def findByAssignee(assignee: String): Future[List[ValuationCase]] = {
    collection.find[ValuationCase](Filters.equal("assignee.id", assignee)).toFuture().map(_.toList)
  }

}
