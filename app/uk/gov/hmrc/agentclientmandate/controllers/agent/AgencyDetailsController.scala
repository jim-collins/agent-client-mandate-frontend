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
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.agentclientmandate.models.AgentDetails

trait AgencyDetailsController extends FrontendController with Actions with MandateConstants {

  def agentClientMandateService: AgentClientMandateService
  def dataCacheService: DataCacheService

  def view(service: String) = AuthorisedFor(AgentRegime(), GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          agentDetails <- agentClientMandateService.fetchAgentDetails
          _ <- dataCacheService.cacheFormData[AgentDetails](agentDetailsFormId, agentDetails)

        } yield {
          Ok(views.html.agent.agentDetails(agentDetails, service,
            Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view(Some(service)).url)))
        }
  }


}

object AgencyDetailsController extends AgencyDetailsController {
  // $COVERAGE-OFF$
  val dataCacheService: DataCacheService = DataCacheService
  val authConnector: AuthConnector = FrontendAuthConnector
  val agentClientMandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}
