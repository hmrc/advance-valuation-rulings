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

package uk.gov.hmrc.bindingtariffclassification.model

import java.net.URLDecoder

import uk.gov.hmrc.play.test.UnitSpec

class UserSearchTest extends UnitSpec {

  private val search =
    UserSearch(role = Some(Role.CLASSIFICATION_MANAGER), team = Some("ACT"))

  private val params: Map[String, Seq[String]] = Map(
    "role" -> Seq("Classification manager"),
    "member_of_teams" -> Seq("ACT", "GATEWAY")
  )

  private val emptyParams: Map[String, Seq[String]] =
    params.mapValues(_.map(_ => ""))

  "Search Binder" should {

    "Unbind Unpopulated Search to Query String" in {
      UserSearch.bindable.unbind("", UserSearch()) shouldBe ""
    }

    //TODO: Fix below test, look into UserSearch for logic to retrieve users by Team

    /*    "Unbind Populated Search to Query String" in {
          val populatedQueryParam: String =
            "role=Classification manager" +
              "&member_of_teams=ACT" +
              "&member_of_teams=GATEWAY"
          URLDecoder.decode(UserSearch.bindable.unbind("", search), "UTF-8") shouldBe populatedQueryParam
        }*/

    "Bind empty query string" in {
      UserSearch.bindable.bind("", Map()) shouldBe Some(Right(UserSearch()))
    }

    "Bind query string with empty values" in {
      UserSearch.bindable.bind("", emptyParams) shouldBe Some(
        Right(UserSearch())
      )
    }

    "Bind populated query string" in {
      UserSearch.bindable.bind("", params) shouldBe Some(Right(search))
    }
  }

}
