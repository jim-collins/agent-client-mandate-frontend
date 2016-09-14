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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.models.{Service, _}
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class UniqueAgentReferenceControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "UniqueAgentReferenceController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/unique-agent-reference/:service" in {
        val result = route(FakeRequest(GET, s"/agent-client-mandate/unique-agent-reference/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'what is your email address' for AUTHORISED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in {
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Your unique agent reference for {0} is {1}")
          document.getElementById("header").text() must include("Your unique agent reference for {0} is {1}")

        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val service = "ated".toUpperCase


  object TestUniqueAgentReferenceController extends UniqueAgentReferenceController {
    override val authConnector = mockAuthConnector
    override val agentClientMandateService = mockAgentClientMandateService
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestUniqueAgentReferenceController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestUniqueAgentReferenceController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val clientMandate = ClientMandate("12345", hc.gaUserId.getOrElse("credid"),
      Party("JARN123456", "Joe Bloggs", "Organisation", ContactDetails("test@test.com", "0123456789")),
      MandateStatus(Status.Pending, DateTime.now(), "credid"), None, Service(None, "ATED"))

    when(mockAgentClientMandateService.createMandate(Matchers.eq(service))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(clientMandate)))

    val result = TestUniqueAgentReferenceController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
