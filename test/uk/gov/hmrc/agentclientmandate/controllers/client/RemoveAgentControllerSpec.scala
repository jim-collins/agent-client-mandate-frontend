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

package uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class RemoveAgentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestRemoveAgentController extends RemoveAgentController {
    override val authConnector = mockAuthConnector
    override val acmService = mockAgentClientMandateService
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach = {
    reset(mockAgentClientMandateService)
  }

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestRemoveAgentController.view("1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedClient(userId, mockAuthConnector)
    val result = TestRemoveAgentController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(request: FakeRequest[AnyContentAsJson], test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestRemoveAgentController.view("1").apply(SessionBuilder.updateRequestWithSession(request, userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)

    val result = TestRemoveAgentController.confirm("1", "agent ltd").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def returnToServiceWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestRemoveAgentController.returnToService().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def showConfirmationWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestRemoveAgentController.showConfirmation("ACME Ltd").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

//  def showConfirmationWithAuthorisedAgent(test: Future[Result] => Any) {
//    val userId = s"user-${UUID.randomUUID}"
//    implicit val hc: HeaderCarrier = HeaderCarrier()
//    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
//    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
//    val result = TestRemoveAgentController.showConfirmation("Acme Ltd").apply(SessionBuilder.buildRequestWithSession(userId))
//    test(result)
//  }
//
//  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
//    val userId = s"user-${UUID.randomUUID}"
//    implicit val hc: HeaderCarrier = HeaderCarrier()
//    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
//    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
//    val result = TestRemoveAgentController.confirm("1", "Acme Ltd").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
//    test(result)
//  }


  "RemoveAgentController" must {
    "not return NOT_FOUND at route " when {
      "GET /mandate/client/remove-agent/1" in {
        val result = route(FakeRequest(GET, "/mandate/client/remove-agent/1")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/client/remove-agent/1" in {
        val result = route(FakeRequest(POST, s"/mandate/client/remove-agent/1/Acme")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for agent removal view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED client" when {
      "client requests(GET) for agent removal view" in {
        viewUnAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }


    "return 'remove agent question' view for AUTHORISED agent" when {
      "client requests(GET) for 'remove agent question' view" in {

        val hc = new HeaderCarrier()
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))
        when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("AS12345678"))
        val request = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        viewAuthorisedClient(request, { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Confirm Agent Removal")
          document.getElementById("header").text() must include("Are you sure you want to cancel the authority for Agent Ltd to act on your behalf for ATED?")
          document.getElementById("pre-heading").text() must be("Manage your ATED service")
          document.getElementById("yesNo_legend").text() must be("Are you sure you want to cancel the authority for Agent Ltd to act on your behalf for ATED?")
          document.getElementById("submit").text() must be("Confirm")
        })
      }

      "no query parameter, throw exception" in {

        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
        val request = FakeRequest(GET, "/client/remove-agent/1").withJsonBody(Json.toJson("""{}"""))
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.view("1").apply(SessionBuilder.updateRequestWithSession(request, userId)))

        thrown.getMessage must be("No returnUrl specified")
      }

      "can't find mandate, throw exception" in {

        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
        when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("AS12345678"))
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
        val request = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.view("1").apply(SessionBuilder.updateRequestWithSession(request, userId)))

        thrown.getMessage must be("No Mandate returned")
      }
    }

    "submitting form" when {
      "invalid form is submitted" in {
        val hc = new HeaderCarrier()
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the question")
          document.getElementsByClass("error-notification").text() must include("You must answer question")
        }
      }

      "submitted with true will redirect to change agent" in {
        when(mockAgentClientMandateService.removeAgent(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(true)
        val hc = new HeaderCarrier()
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/change-agent")
        }
      }

      "submitted with true but agent removal fails" in {

        when(mockAgentClientMandateService.removeAgent(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(false)
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.confirm("1", "ACME").apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Agent Removal Failed")
      }

      "submitted with false will redirect to cached return url" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("/api/anywhere")))
        val hc = new HeaderCarrier()
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "submitted with false but retreival of returnUrl from cache fails" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.confirm("1", "ACME").apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Cache Retrieval Failed")
      }
    }

    "returnToService" when {
      "redirects to cached service" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("/api/anywhere")))
        val hc = new HeaderCarrier()
        returnToServiceWithAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "fails when cache fails" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.returnToService().apply(SessionBuilder.buildRequestWithSession(userId)))

        thrown.getMessage must be("Cache Retrieval Failed")
      }
    }

    "showConfirmation" when {
      "agent has been removed show confirmation page" in {
        val hc = new HeaderCarrier()
        showConfirmationWithAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What happens next?")
          document.getElementById("banner-text").text() must include("You have removed ACME Ltd as your agent")
          document.getElementById("notification").text() must be("Your agent will receive an email notification.")
          document.getElementById("heading").text() must be("What happens next?")
          document.getElementById("finish_btn").text() must be("Finish and sign out")
          document.getElementById("return_to_service_button").text() must be("Your ATED online service")
        }
      }
    }

  }

}
