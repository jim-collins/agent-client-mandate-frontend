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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future


object MandateDetailsController extends MandateDetailsController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val mandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}

trait MandateDetailsController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def mandateService: AgentClientMandateService

  def view(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId) flatMap {
        case Some(agentEmail) =>
          dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) map {
            case Some(x) => Ok(views.html.agent.mandateDetails(agentEmail.email, service, x.name, getBackLink(service, callingPage)))
            case _ => Redirect(routes.ClientDisplayNameController.view(service))
          }
        case _ => Future.successful(Redirect(routes.CollectAgentEmailController.view(service)))
      }
  }

  def submit(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
    for {
      mandateId <- mandateService.createMandate(service)
    } yield {
      Redirect(routes.UniqueAgentReferenceController.view(service))
    }
  }

  private def getBackLink(service: String, callingPage: String) = {
    callingPage match {
      case PaySAQuestionController.controllerId => Some(routes.PaySAQuestionController.view(service).url)
      case _ => Some(routes.OverseasClientQuestionController.view(service).url)
    }
  }
}
