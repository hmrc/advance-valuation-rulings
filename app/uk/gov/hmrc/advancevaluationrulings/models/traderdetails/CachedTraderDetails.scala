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

package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class CachedTraderDetails(index: String,
                                     data: TraderDetailsResponse,
                                     lastUpdated: Instant)

object CachedTraderDetails {

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[CachedTraderDetails] = {
    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    val encryptedReads: Reads[CachedTraderDetails] =
      (
        (__ \ "index").read[String] and // TODO: Hash this?
          (__ \ "data").read[SensitiveString] and
          (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
        )((index, value, lastUpdated) => CachedTraderDetails(index, Json.parse(value.decryptedValue).as[TraderDetailsResponse], lastUpdated))

    val encryptedWrites: OWrites[CachedTraderDetails] =
      (
        (__ \ "index").write[String] and
          (__ \ "data").write[SensitiveString] and
          (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
        )(x => (x.index, SensitiveString(Json.stringify(Json.toJsObject(x.data))), x.lastUpdated))

    OFormat(encryptedReads, encryptedWrites)
  }
}
