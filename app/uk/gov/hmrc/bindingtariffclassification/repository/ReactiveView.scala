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

import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONAggregationFramework.{Match, PipelineOperator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

abstract class ReactiveView[A, ID](
                                    viewName: String,
                                    collectionName: String,
                                    mongo: () => DB
                                  ) {

  private def initView: Future[JSONCollection] = {
    def getView: JSONCollection       = mongo().collection[JSONCollection](viewName)
    def getCollection: JSONCollection = mongo().collection[JSONCollection](collectionName)

    getView.drop(failIfNotFound = false).flatMap { _ =>
      getCollection
        .createView(viewName, pipeline.headOption.getOrElse(Match(Json.obj())), pipeline.tail, None)
        .map(_ => getView)
    }
  }

  lazy val view: JSONCollection = Await.result(awaitable = initView, atMost = 30.seconds)

  protected def pipeline: Seq[PipelineOperator]

}