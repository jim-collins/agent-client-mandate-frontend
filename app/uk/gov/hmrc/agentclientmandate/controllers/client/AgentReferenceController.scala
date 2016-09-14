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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientAgentReferenceForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController


object AgentReferenceController extends AgentReferenceController {
  val authConnector = FrontendAuthConnector
}

trait AgentReferenceController extends FrontendController with Actions {

  def agentReference = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Ok(views.html.client.clientAgentReference(ClientAgentReferenceForm.clientAgentRefForm))
  }

  def continue = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Redirect(routes.ClientReviewAgentController.reviewAgent())
  }


}
