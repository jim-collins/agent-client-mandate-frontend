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
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object RemoveAgentController extends RemoveAgentController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val dataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait RemoveAgentController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def dataCacheService: DataCacheService

  def view(mandateId: String, service: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      request.getQueryString("returnUrl") match {
        case Some(returnUrl) =>
          dataCacheService.cacheFormData[String]("RETURN_URL", returnUrl).flatMap { cache =>
            acmService.fetchClientMandate(mandateId).map {
              case Some(mandate) => Ok(views.html.client.removeAgent(new YesNoQuestionForm("client.remove-agent.error").yesNoQuestionForm, mandate.agentParty.name, mandateId, service))
              case _ => throw new RuntimeException("No Mandate returned")
            }
          }
        case _ => throw new RuntimeException("No returnUrl specified")
      }
  }

  def submit(mandateId: String, agentName: String, service: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      val form = new YesNoQuestionForm("client.remove-agent.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.client.removeAgent(formWithError, agentName, mandateId, service))),
        data => {
          val removeAgent = data.yesNo.getOrElse(false)
          if (removeAgent) {
            acmService.removeAgent(mandateId).map { removedAgent =>
              if (removedAgent) Redirect(routes.ChangeAgentController.view(agentName, service))
              else throw new RuntimeException("Agent Removal Failed")
            }
          }
          else {
            dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map {
              case Some(x) => Redirect(x)
              case _ => throw new RuntimeException("Cache Retrieval Failed")
            }
          }
        }
      )
  }

  def confirmation(agentName: String) = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Ok(views.html.client.removeAgentConfirmation(agentName, "ATED"))
  }

  def returnToService = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map {
        case Some(x) => Redirect(x)
        case _ => throw new RuntimeException("Cache Retrieval Failed")
      }
  }

}
