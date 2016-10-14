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

package uk.gov.hmrc.agentclientmandate.service

import uk.gov.hmrc.agentclientmandate.connectors.GovernmentGatewayConnector
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait GovernmentGatewayService {

  def ggConnector: GovernmentGatewayConnector
  def agentClientMandateService: AgentClientMandateService

  def setupInitialMandates(service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Unit] = {
    val arn = AuthUtils.getArn
    agentClientMandateService.fetchAllClientMandates(arn, service).flatMap { mandatesOption =>
      mandatesOption match {
        case Some(mandates) => {
          ggConnector.retrieveClientList.map { clientList =>
            if (clientList.size != mandates.activeMandates.size)
            {
              
              // if clientList > 0
              //    create list of mandateDto's comparing both lists
              //    send to backend
            }
          }
        }
        case _ => ???
      }

    }
  }
}

object GovernmentGatewayService extends GovernmentGatewayService {
  val ggConnector = GovernmentGatewayConnector
  val agentClientMandateService = AgentClientMandateService
}
