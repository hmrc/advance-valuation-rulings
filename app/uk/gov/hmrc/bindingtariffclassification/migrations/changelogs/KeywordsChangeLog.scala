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

package uk.gov.hmrc.bindingtariffclassification.migrations.changelogs

import better.files._
import com.github.cloudyrock.mongock.{ChangeLog, ChangeSet}
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertOneModel
import org.bson.Document

import scala.collection.JavaConverters._

@ChangeLog(order = "001")
class KeywordsChangeLog {

  @ChangeSet(order = "001", id = "initialKeywordsMigration", author = "binding-tariff-classification")
  def initialKeywordsMigration(mongoDatabase: MongoDatabase): Unit = {

    using(Resource.getAsStream("keywords.txt")) { stream =>
      val keywords = stream.lines
      val keywordsCollection = mongoDatabase.getCollection("keywords")
      keywordsCollection.bulkWrite(
        keywords.toList.map { keyword =>
          new InsertOneModel(new Document("name", keyword))
        }.asJava
      )
    }
  }
}
