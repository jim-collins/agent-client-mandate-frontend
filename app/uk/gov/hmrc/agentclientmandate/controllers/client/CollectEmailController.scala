/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers.client

import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object CollectEmailController extends CollectEmailController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
}

trait CollectEmailController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def emailService: EmailService

  def view() = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map { a =>
        a.flatMap(_.email) match {
          case Some(x) => Ok(views.html.client.collectEmail(clientEmailForm.fill(x)))
          case None => Ok(views.html.client.collectEmail(clientEmailForm))
        }
      }
  }

  def submit() = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      validateConfirmEmail(clientEmailForm.bindFromRequest).fold(
        formWithError => Future.successful(BadRequest(views.html.client.collectEmail(formWithError))),
        data => {
          emailService.validate(data.email) flatMap { isValidEmail =>
            if (isValidEmail) {
              dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
                case Some(x) => dataCacheService.cacheFormData[ClientCache](clientFormId, x.copy(email = Some(data))) flatMap { cachedData =>
                  Future.successful(Redirect(routes.SearchMandateController.view()))
                }
                case None => dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(email = Some(data))) flatMap { cachedData =>
                  Future.successful(Redirect(routes.SearchMandateController.view()))
                }
              }
            } else {
              val errorMsg = Messages("client.collect-email.error.email.invalid-by-email-service")
              val errorForm = clientEmailForm.withError(key = "client-collect-email-form", message = errorMsg).fill(data)
              Future.successful(BadRequest(views.html.client.collectEmail(errorForm)))
            }
          }
        }
      )
  }

}

