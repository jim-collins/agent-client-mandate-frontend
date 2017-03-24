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
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views.html.partials.client_banner
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.BadRequestException
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait ClientBannerPartialController extends FrontendController with Actions {

  def mandateService: AgentClientMandateService

  def getBanner(clientId: String, service: String, returnUrl: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request => {

      // $COVERAGE-OFF$
      if (Option(returnUrl).isEmpty) throw new BadRequestException("The return url is a mandatory parameter")
      // $COVERAGE-ON$

      mandateService.fetchClientMandateByClient(clientId, service).map { x =>
        x match {
          case Some(mandate) => mandate.currentStatus.status match {
            case uk.gov.hmrc.agentclientmandate.models.Status.Active => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(service, mandate.id, returnUrl).url, "attorneyBanner--client-request-accepted", "active", "approved_active"))
            case uk.gov.hmrc.agentclientmandate.models.Status.Approved => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(service, mandate.id, returnUrl).url, "attorneyBanner--client-request-requested", "approved", "approved_active"))
            case uk.gov.hmrc.agentclientmandate.models.Status.Rejected => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.CollectEmailController.view(service).url, "attorneyBanner--client-request-rejected", "rejected", "cancelled_rejected"))
            case uk.gov.hmrc.agentclientmandate.models.Status.Cancelled => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.CollectEmailController.view(service).url, "attorneyBanner--client-request-rejected", "cancelled", "cancelled_rejected"))
          }
          case None => NotFound
        }
      }
    }
  }
}

object ClientBannerPartialController extends ClientBannerPartialController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val mandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}
