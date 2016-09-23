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

package uk.gov.hmrc.agentclientmandate.connectors

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.CreateMandateDto
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait AgentClientMandateConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  val agentMandateUrl = "agent"
  val mandate = "mandate"

  def http: HttpGet with HttpPost with HttpDelete

  def createMandate(mandateDto: CreateMandateDto)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val agentLink = ac.principal.accounts.agent.map(_.link).getOrElse("") // TODO: Change here
    val postUrl = s"$serviceUrl$agentLink/$mandate"
    val jsonData = Json.toJson(mandateDto)
    Logger.info(s"[AgentClientMandateConnector][createMandate] - POST - $postUrl and JSON Data - $jsonData")
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandate/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][fetchMandate] - GET - $getUrl")
    http.GET[HttpResponse](getUrl)
  }
}

object AgentClientMandateConnector extends AgentClientMandateConnector {
  val serviceUrl = baseUrl("agent-client-mandate")
  val http = WSHttp
}
