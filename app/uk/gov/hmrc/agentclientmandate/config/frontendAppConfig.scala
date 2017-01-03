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

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val logoutUrl: String
  val mandateFrontendHost: String

  def serviceSignOutUrl(service: Option[String]): String
  def nonUkUri(service: String): String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = configuration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "agent-client-mandate-frontend"

  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val logoutUrl = s"""${configuration.getString("microservice.logout.url").getOrElse("/gg/sign-out")}"""

  override def nonUkUri(service: String): String = s"""${configuration.getString("microservice.services.business-customer-frontend.nonUK-uri").
    getOrElse("")}/${service.toLowerCase}"""

  override def serviceSignOutUrl(service: Option[String]): String = {
    service match {
      case Some(delegatedService) if (!delegatedService.isEmpty()) => configuration.getString(s"microservice.delegated-service-sign-out-url.${delegatedService.toLowerCase}").getOrElse(logoutUrl)
      case _ => logoutUrl
    }
  }

  override lazy val mandateFrontendHost = configuration.getString(s"microservice.services.agent-client-mandate-frontend.host").getOrElse("")
}
