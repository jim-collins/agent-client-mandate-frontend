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
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController


object AgentSummaryController extends AgentSummaryController {
  val authConnector = FrontendAuthConnector
  val agentClientMandateService = AgentClientMandateService
}

trait AgentSummaryController extends FrontendController with Actions {

  def agentClientMandateService: AgentClientMandateService

  def view(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      val arn = AuthUtils.getArn
      for {
        mandates <- agentClientMandateService.fetchAllClientMandates(arn, "ATED")
        agentDetails <- agentClientMandateService.fetchAgentDetails()
      } yield {
        Ok(views.html.agent.agentSummary(service, mandates, agentDetails))
      }
  }

}