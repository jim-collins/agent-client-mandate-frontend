/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future


object MandateDeclarationController extends MandateDeclarationController {
  val authConnector = FrontendAuthConnector
  val dataCacheService = DataCacheService
  val mandateService = AgentClientMandateService
}

trait MandateDeclarationController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def mandateService: AgentClientMandateService

  def view(service: String)= AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map {
        _.flatMap(_.mandate) match {
          case Some(x) => Ok(views.html.client.mandateDeclaration(x, getBackLink(service)))
          case None => Redirect(routes.ReviewMandateController.view())
        }
      }
  }

  def submit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence) async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
        _.flatMap(_.mandate) match {
          case Some(m) =>
            mandateService.approveMandate(m) flatMap {
              case Some(n) => Future.successful(Redirect(routes.MandateConfirmationController.view()))
              case None => Future.successful(Redirect(routes.ReviewMandateController.view()))
            }
          case None => Future.successful(Redirect(routes.ReviewMandateController.view()))
        }
      }
  }

  private def getBackLink(service: String) = {
    Some(routes.ReviewMandateController.view().url)
  }
}
