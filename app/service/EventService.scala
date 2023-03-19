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

package service

import javax.inject._
import model.{Event, EventSearch, Paged, Pagination}
import repository.EventRepository

import scala.concurrent.Future

@Singleton
class EventService @Inject() (repository: EventRepository) {

  def insert(e: Event): Future[Event] =
    repository.insert(e)

  def search(search: EventSearch, pagination: Pagination): Future[Paged[Event]] =
    repository.search(search, pagination)

  def deleteAll(): Future[Unit] =
    repository.deleteAll()

  def deleteCaseEvents(caseReference: String): Future[Unit] =
    repository.delete(EventSearch(caseReference = Some(Set(caseReference))))

}