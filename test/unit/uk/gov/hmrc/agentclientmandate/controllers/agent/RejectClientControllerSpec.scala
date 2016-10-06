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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class RejectClientControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "RejectClientController" must {
    "not return NOT_FOUND at route " when {
      "GET /mandate/agent/reject/:service/:id" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/reject/$service/$mandateId")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/reject/:service/:id" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/reject/$service/$mandateId/$agentName")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'reject client question' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'reject client question' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'reject client question' view for AUTHORISED agent" when {

      "agent requests(GET) for 'reject client question' view" in {
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Are you sure you want to reject the request from ACME Limited?")
          document.getElementById("header").text() must include("Are you sure you want to reject the request from ACME Limited?")
          document.getElementById("pre-heading").text() must be("Manage your ATED service")
          document.getElementById("yesNo_legend").text() must be("Are you sure you want to reject the request from ACME Limited?")
          document.getElementById("submit").text() must be("Confirm")
        }
      }

      "service can't find mandate throw exception" in {
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
        val thrown = the[RuntimeException] thrownBy await(TestRejectClientController.view(service, "1").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("No Mandate returned")
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the question")
          document.getElementsByClass("error-notification").text() must include("You must answer question")
        }
      }
    }

    "submitting form " when {
      "submitted with false will redirect to agent summary" in {
        val hc = new HeaderCarrier()
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/agent/summary/$service")
        }
      }

      "submitted with true will redirect to confirmation" in {
        when(mockAgentClientMandateService.rejectClient(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(true)
        val hc = new HeaderCarrier()
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/agent/reject/confirmation")
        }
      }

      "submitted with true throws exception" in {
        when(mockAgentClientMandateService.rejectClient(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(false)
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
        val thrown = the[RuntimeException] thrownBy await(TestRejectClientController.submit(service, "ABC123", "Acme Ltd").apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must include("Client Rejection Failed")
      }
    }

    "return 'client rejection confirmation' view for AUTHORISED agent" when {

      "agent requests(GET) for 'client rejection confirmation' view" in {

        val hc = new HeaderCarrier()

        confirmationWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Client Rejection Confirmed")
        }
      }
    }

  }

  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector = mock[AuthConnector]
  val service = "ATED"
  val mandateId = "1"
  val agentName = "Acme"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))

  object TestRejectClientController extends RejectClientController {
    override val authConnector = mockAuthConnector
    override val acmService = mockAgentClientMandateService
  }

  override def beforeEach = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestRejectClientController.view(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestRejectClientController.view(service, "1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestRejectClientController.view(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestRejectClientController.confirmation(service, "Acme Ltd").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestRejectClientController.submit(service, "1", "Acme Ltd").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
