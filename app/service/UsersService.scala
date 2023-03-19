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

import model._
import repository._

import javax.inject._
import scala.concurrent.Future

@Singleton
class UsersService @Inject() (
  usersRepository: UsersRepository
) {

  def getUserById(id: String): Future[Option[Operator]] =
    usersRepository.getById(id)

  def insertUser(user: Operator): Future[Operator] =
    usersRepository.insert(user)

  def updateUser(user: Operator, upsert: Boolean): Future[Option[Operator]] =
    usersRepository.update(user, upsert)

  def search(search: UserSearch, pagination: Pagination): Future[Paged[Operator]] =
    usersRepository.search(search, pagination)

}