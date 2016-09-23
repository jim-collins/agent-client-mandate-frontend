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

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class UniqueAgentReferenceControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "UniqueAgentReferenceController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/unique-agent-reference/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/unique-agent-reference/$service")).get
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
        viewWithAuthorisedAgent(Some(mandateId)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Your unique agent reference for {0} is {1}")
          document.getElementById("banner-text").text() must include("Your unique agent reference for [client name] is ABC123")
          document.getElementById("what-happens-next").text must be("What happens next?")
          document.getElementById("authorise-instruction").text must be("You need to give this agent reference to your client so they can authorise you.")
          document.getElementById("client-instruction").text must be("Your client will then need to:")
          document.getElementById("client-instruction-1").text must be("Register their company to use the new ATED service, they may need to create a new organisational Government Gateway account.")
          document.getElementById("client-instruction-2").text must be("Enter the agent reference you gave them.")
          document.getElementById("email-instruction").text must be("Once they have done this you will receive an email notification.")
          document.getElementById("request-expire").text must be("You have 28 days to sign in and accept their request.")
          document.getElementById("admin-instruction").text must be("If you have a number of agents working on ATED within your organisation you may want to filter your clients. To do this you need to add administrators to your account in Government Gateway.")
          document.getElementById("submit").attr("href") must be("#")
          document.getElementById("ated-service").attr("href") must be("#")
          document.getElementById("admin").attr("href") must be("#")

        }
      }

    }

    "redirect agent to select service page" when {
      "mandate ID is not found in cache" in {
        viewWithAuthorisedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/select-service"))
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val service = "ated".toUpperCase
  val mandateId = "ABC123"


  object TestUniqueAgentReferenceController extends UniqueAgentReferenceController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
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

  def viewWithAuthorisedAgent(mandateId: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestUniqueAgentReferenceController.agentRefCacheId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandateId))

    val result = TestUniqueAgentReferenceController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
