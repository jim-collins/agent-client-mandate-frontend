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

package uk.gov.hmrc.agentclientmandate.controllers.auth

import play.api.Play.{configuration, current}

object ExternalUrls {
  val companyAuthHost = s"${configuration.getString(s"microservice.services.auth.company-auth.host").getOrElse("")}"
  val loginPath = s"${configuration.getString(s"microservice.services.auth.login-path").getOrElse("gg/sign-in")}"
  val loginCallbackAgent = s"${configuration.getString(s"microservice.services.auth.login-callback-agent.url").
    getOrElse("/agent-client-mandate/home")}"
  val loginCallbackClient = s"${configuration.getString(s"microservice.services.auth.login-callback-client.url").
    getOrElse("/agent-client-mandate/client-search-mandate")}"
}
