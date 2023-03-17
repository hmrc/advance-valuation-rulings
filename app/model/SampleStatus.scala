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

package model

object SampleStatus extends Enumeration {
  type SampleStatus = Value
  val NONE, AWAITING, MOVED_TO_ACT, MOVED_TO_ELM, SENT_FOR_ANALYSIS, SENT_TO_APPEALS, STORAGE, RETURNED_APPLICANT,
    RETURNED_PORT_OFFICER, RETURNED_COURIER, DESTROYED = Value
}
