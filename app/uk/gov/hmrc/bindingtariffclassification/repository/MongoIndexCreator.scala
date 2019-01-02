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

package uk.gov.hmrc.bindingtariffclassification.repository

import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending

trait MongoIndexCreator {

  def createSingleFieldAscendingIndex(indexFieldKey: String, isUnique: Boolean): Index = {

    createCompoundIndex(
      fieldNames = Seq(indexFieldKey),
      isUnique = isUnique
    )
  }

  def createCompoundIndex(fieldNames: Seq[String],
                          isUnique: Boolean,
                          isBackground: Boolean = false): Index = {

    Index(
      key = fieldNames.map(_ -> Ascending),
      name = Some(s"${fieldNames.mkString("-")}_Index"),
      unique = isUnique,
      background = isBackground
    )
  }

}
