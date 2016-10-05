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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object ChangeAgentController extends ChangeAgentController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val dataCacheService = DataCacheService
  // $COVERAGE-ON$
}


trait ChangeAgentController extends FrontendController with Actions{

  def acmService: AgentClientMandateService
  def dataCacheService: DataCacheService

  def view(agentName: String) = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Ok(views.html.client.changeAgent(yesNoQuestionForm, agentName))
  }

  def confirm(agentName: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      yesNoQuestionForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.client.changeAgent(formWithError, agentName))),
        data => {
          val changeAgent = data.yesNo.getOrElse(false)
          if (changeAgent) {
            Future.successful(Redirect(routes.CollectEmailController.view()))
          }
          else {
            Future.successful(Redirect(routes.RemoveAgentController.showConfirmation(agentName)))
          }
        }
      )
  }
}
