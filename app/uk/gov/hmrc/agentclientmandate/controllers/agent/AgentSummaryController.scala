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

import uk.gov.hmrc.agentclientmandate.config.{FrontendAuthConnector, FrontendDelegationConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, Delegator}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object AgentSummaryController extends AgentSummaryController {
  val authConnector = FrontendAuthConnector
  val agentClientMandateService = AgentClientMandateService
  val delegationConnector: DelegationConnector = FrontendDelegationConnector
}

trait AgentSummaryController extends FrontendController with Actions with Delegator {

  def agentClientMandateService: AgentClientMandateService

  def view(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      val arn = AuthUtils.getArn
      for {
        mandates <- agentClientMandateService.fetchAllClientMandates(arn, service)
        agentDetails <- agentClientMandateService.fetchAgentDetails()
      } yield {
        Ok(views.html.agent.agentSummary(service, mandates, agentDetails))
      }
  }

  def doDelegation(service: String, serviceId: String, clientName: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      startDelegationAndRedirect(createDelegationContext(service, serviceId, clientName), getDelegatedServiceRedirectUrl(service))
  }

}
