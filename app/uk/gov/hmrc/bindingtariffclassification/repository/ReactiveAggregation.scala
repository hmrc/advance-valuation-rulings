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

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.libs.json.{Format, Json}
import reactivemongo.api.DB
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONAggregationFramework.{Match, PipelineOperator}

abstract class ReactiveAggregation[A: Format](
  collectionName: String,
  mongo: () => DB
)(implicit mat: Materializer) {

  protected def pipeline: List[PipelineOperator]

  def runAggregation: Source[A, NotUsed] =
    mongo()
      .collection[JSONCollection](collectionName)
      .aggregatorContext[A](pipeline.headOption.getOrElse(Match(Json.obj())), pipeline.tail)
      .prepared[AkkaStreamCursor.WithOps]
      .cursor
      .documentSource()
      .mapMaterializedValue(_ => NotUsed)
}
