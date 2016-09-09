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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object CollectAgentEmailController extends CollectAgentEmailController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val formId: String = "agent-email"
}

trait CollectAgentEmailController extends FrontendController with Actions {

  def dataCacheService: DataCacheService

  def formId: String

  def view(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit user => implicit request =>
      dataCacheService.fetchAndGetFormData[AgentEmail](formId) map {
        case Some(agentEmail) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(agentEmail), service))
        case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service))
      }
  }

  def submit(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      agentEmailForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.agent.agentEnterEmail(formWithError, service))),
        data => {
          dataCacheService.cacheFormData[AgentEmail](formId, data) flatMap { dataCached =>
            Future.successful(Redirect(routes.OverseasClientQuestionController.view(service)))
          }
        }
      )
  }

}
