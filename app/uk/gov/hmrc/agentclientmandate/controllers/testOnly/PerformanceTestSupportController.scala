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

package uk.gov.hmrc.agentclientmandate.controllers.testOnly

import play.api.Logger
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models.Mandate
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait PerformanceTestSupportController extends FrontendController with Actions {

  def agentClientMandateConnector: AgentClientMandateConnector

  // $COVERAGE-OFF$
  def createMandate() = UnauthorisedAction.async { implicit request =>
    Logger.debug("inserting test mandate")
     agentClientMandateConnector.testOnlyCreateMandate(request.body.asJson.get.as[Mandate]).map { x =>
       Logger.debug("inserted test mandate")
       Ok
     }
  }

  def deleteMandate(mandateId: String) = UnauthorisedAction.async { implicit request =>
    Logger.debug(s"deleting mandate: $mandateId")
    agentClientMandateConnector.testOnlyDeleteMandate(mandateId).map { x =>
      Logger.debug("deleted mandate")
      Ok
    }
  }
  // $COVERAGE-ON$
}

object PerformanceTestSupportController extends PerformanceTestSupportController {
  // $COVERAGE-OFF$
  val agentClientMandateConnector = AgentClientMandateConnector
  val authConnector = FrontendAuthConnector
  // $COVERAGE-ON$
}
