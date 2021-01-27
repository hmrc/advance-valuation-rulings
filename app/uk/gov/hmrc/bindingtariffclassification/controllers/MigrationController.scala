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

package uk.gov.hmrc.bindingtariffclassification.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.bindingtariffclassification.migrations.{AmendDateOfExtractMigrationJob, MigrationRunner}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MigrationController @Inject() (
  migrationRunner: MigrationRunner,
  mcc: MessagesControllerComponents
) extends CommonController(mcc) {

  def amendDateOfExtract(): Action[AnyContent] =
    Action.async { implicit request =>
      migrationRunner.trigger(classOf[AmendDateOfExtractMigrationJob]) map (_ => NoContent) recover recovery
    }
}