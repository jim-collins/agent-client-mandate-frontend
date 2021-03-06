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

package uk.gov.hmrc.agentclientmandate.config

import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

object FrontendAuditConnector extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with AppName

object WSHttp extends WSHttp {
  override val hooks = NoneRequired
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp
}

object AgentClientMandateSessionCache extends SessionCache with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("session-cache")
  override lazy val domain = getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))
}

object FrontendDelegationConnector extends DelegationConnector with ServicesConfig {
  val serviceUrl = baseUrl("delegation")
  lazy val http = WSHttp
}
