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

import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermissionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ClientPermissionController extends ClientPermissionController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  // $COVERAGE-ON$
}

trait ClientPermissionController extends FrontendController with Actions {

  def view(service: String) = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit user => implicit request =>
      Ok(views.html.agent.clientPermission(clientPermissionForm, service))
  }


  def submit(service: String) = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit user => implicit request =>
      clientPermissionForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.agent.clientPermission(formWithErrors, service)),
        data => {
          if (data.hasPermission.getOrElse(false)) Redirect(nonUkUri(service))
          else Redirect(routes.AgentSummaryController.view(service))
        }
      )
  }

}