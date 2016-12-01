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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views.html.partials.client_banner
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait ClientBannerPartialController extends FrontendController with Actions {

  def mandateService: AgentClientMandateService

  def getBanner(clientId: String, service: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request => {
      mandateService.fetchClientMandateByClient(clientId, service).map { x =>
        x match {
          case Some(mandate) => Ok(client_banner(mandate.agentParty.name, routes.RemoveAgentController.view(mandate.id).url))
          case None => NotFound
        }
      }
    }
  }
}

object ClientBannerPartialController extends ClientBannerPartialController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val mandateService: AgentClientMandateService = AgentClientMandateService
}
