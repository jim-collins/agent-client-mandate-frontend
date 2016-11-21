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
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.Future

trait AtedSubscriptionFrontendConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String
  def http: HttpGet
  val clearCacheUri = "clear-cache"

  def clearCache(service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/$clearCacheUri/$service"
    Logger.debug(s"[AtedSubscriptionFrontendConnector][clearCache] - GET - $getUrl")
    http.GET[HttpResponse](getUrl)
  }

}

object AtedSubscriptionFrontendConnector extends AtedSubscriptionFrontendConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("ated-subscription-frontend")
  val http = WSHttp
  // $COVERAGE-ON$
}
