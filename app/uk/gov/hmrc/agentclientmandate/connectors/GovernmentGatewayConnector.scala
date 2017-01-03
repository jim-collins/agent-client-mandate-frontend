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

package uk.gov.hmrc.agentclientmandate.connectors

import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.RetrieveClientAllocation
import uk.gov.hmrc.agentclientmandate.utils.GovernmentGatewayConstants
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GovernmentGatewayConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String
  def http: HttpGet

  private def getClientList(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val agentLink = ac.principal.accounts.agent.map(_.link).getOrElse("")
    val atedService = GovernmentGatewayConstants.ClientListServiceName
    val atedAllocatedTo = GovernmentGatewayConstants.ClientListAllocatedToGroup
    val postUrl = s"""$serviceUrl${agentLink}/client-list/$atedService/$atedAllocatedTo"""
    http.GET[HttpResponse](postUrl)
  }

  def retrieveClientList(implicit hc: HeaderCarrier, ac: AuthContext): Future[List[RetrieveClientAllocation]] = {
    getClientList map { response =>
      response.status match {
        case OK =>
          response.json.as[List[RetrieveClientAllocation]]
        case NOT_FOUND | NO_CONTENT =>
          List.empty[RetrieveClientAllocation]
        case BAD_REQUEST =>
          Logger.warn(s"[GovernmentGatewayConnector] [retrieveClientList] BadRequestException body ${response.body}")
          throw new BadRequestException(s"[GovernmentGatewayConnector] [retrievePendingClients] " +
            s"Bad Request: Failed to retrieve ClientList, body ${response.body}")
        case status =>
          Logger.warn(s"[GovernmentGatewayConnector] [retrieveClientList] [status] = $status, [body] = ${response.body}")
          throw new InternalServerException(s"[GovernmentGatewayConnector] [retrievePendingClients]" +
            s"Internal Server error: Failed to retrieve ClientList[status] = $status, [body] = ${response.body}")
      }
    }
  }

}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("government-gateway")
  val http = WSHttp
  // $COVERAGE-ON$
}
