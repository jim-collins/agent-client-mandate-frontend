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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class CollectAgentEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectAgentEmailController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/email/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/email/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/email/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/email/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewEmailUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewEmailUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'what is your email address' for AUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view and the data hasn't been cached" in {
        viewEmailAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address?")
          document.getElementById("header").text() must include("What is your email address?")
          document.getElementById("pre-header").text() must be("Add a client")
          document.getElementById("info").text() must be(s"We need this to send you notifications relating to this client's activity within the $service online service.")
          document.getElementById("email_field").text() must be("Email address")
          document.getElementById("email").`val`() must be("")
          document.getElementById("confirmEmail").`val`() must be("")
          document.getElementById("confirmEmail_field").text() must be("Confirm email address")
          document.getElementById("submit").text() must be("Submit")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "agent requests(GET) for 'what is your email address' view pre-populated and the data has been cached" in {
        viewEmailAuthorisedAgent(Some(agentEmail)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address?")
          document.getElementById("email").`val`() must be("aa@aa.com")
          document.getElementById("confirmEmail").`val`() must be("aa@aa.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }

    "redirect to 'overseas client question' Page" when {
      "valid form is submitted with valid email" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com", "confirmEmail" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/overseas-client-question/ATED"))
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "", "confirmEmail" -> "")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-list").text() must include("There is a problem with the confirm email address question")
          document.getElementsByClass("error-notification").text() must include("You must answer email address question")
          document.getElementsByClass("error-notification").text() must include("You must answer confirm email address question")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "confirmEmail field value is not equal to email field value" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com", "confirmEmail" -> "aa@bb.com")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the confirm email address question")
          document.getElementsByClass("error-notification").text() must include("You must enter same email address in confirm email address")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com", "confirmEmail" -> "aa@invalid.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("This email is invalid")
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockEmailService: EmailService = mock[EmailService]

  val service = "ated".toUpperCase
  val formId1 = "agent-email"
  val agentEmail = AgentEmail("aa@aa.com", "aa@aa.com")

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockEmailService)
    reset(mockAuthConnector)
  }

  def viewEmailUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestCollectAgentEmailController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewEmailUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCollectAgentEmailController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewEmailAuthorisedAgent(cachedData: Option[AgentEmail] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(formId1))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectAgentEmailController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitEmailAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean = false)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockEmailService.validate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(isValidEmail))
    when(mockDataCacheService.cacheFormData[AgentEmail](Matchers.eq(formId1), Matchers.eq(agentEmail))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentEmail))
    val result = TestCollectAgentEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  object TestCollectAgentEmailController extends CollectAgentEmailController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val emailService = mockEmailService
  }

}
