/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.model

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.bindingtariffclassification.sort.CaseSortField.CaseSortField
import uk.gov.hmrc.bindingtariffclassification.sort.SortDirection.SortDirection
import uk.gov.hmrc.bindingtariffclassification.sort.{CaseSortField, SortDirection}

case class CaseSort
(
  field: CaseSortField,
  direction: SortDirection = SortDirection.ASCENDING
)

object CaseSort {

  private val sortByKey = "sort_by"
  private val sortDirectionKey = "sort_direction"

  private def bindSortField(key: String): Option[CaseSortField] = {
    CaseSortField.values.find(_.toString == key)
  }

  private def bindSortDirection(key: String): Option[SortDirection] = {
    SortDirection.values.find(_.toString == key)
  }

  implicit def bindable(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[CaseSort] = new QueryStringBindable[CaseSort] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, CaseSort]] = {

      def param(name: String): Option[String] = stringBinder.bind(name, params).filter(_.isRight).map(_.right.get)

      val field: Option[CaseSortField] = param(sortByKey).flatMap(bindSortField)
      val direction: Option[SortDirection] = param(sortDirectionKey).flatMap(bindSortDirection)

      (field, direction) match {
        case (Some(f), Some(d)) => Some(Right(CaseSort(field = f, direction = d)))
        case (Some(f), _) => Some(Right(CaseSort(field = f)))
        case _ => None
      }
    }

    override def unbind(key: String, query: CaseSort): String = {
      Seq[String](
        stringBinder.unbind(sortByKey, query.field.toString),
        stringBinder.unbind(sortDirectionKey, query.direction.toString)
      ).mkString("&")

    }

  }

}