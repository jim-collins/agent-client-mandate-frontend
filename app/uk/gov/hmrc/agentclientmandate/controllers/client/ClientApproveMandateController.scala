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

import uk.gov.hmrc.agentclientmandate._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ApproveClientMandateForm.approveClientMandateForm
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ClientApproveMandateController extends ClientApproveMandateController {
  val authConnector: AuthConnector = FrontendAuthConnector
}

trait ClientApproveMandateController extends FrontendController with Actions {

  def approve = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit user => Ok(views.html.client.approveMandate(approveClientMandateForm))
  }

  def submit = AuthorisedFor(ClientRegime, GGConfidence) {
    implicit authContext => implicit user =>
      approveClientMandateForm.bindFromRequest.fold(
        formWithError => BadRequest(views.html.client.approveMandate(formWithError)),
        data =>
          if(data.approved.getOrElse(false)) Redirect(routes.ClientConfirmMandateController.accepted())
          else Redirect(routes.ClientConfirmMandateController.rejected())
      )
  }

}
