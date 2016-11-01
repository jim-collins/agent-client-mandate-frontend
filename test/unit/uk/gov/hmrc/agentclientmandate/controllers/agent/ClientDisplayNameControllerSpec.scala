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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

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
import uk.gov.hmrc.agentclientmandate.controllers.agent.ClientDisplayNameController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.play.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class ClientDisplayNameControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ClientDisplayNameController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/client-display-name/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/email/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/client-display-name/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/email/$service")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewClientDisplayNameUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewClientDisplayNameUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return view for AUTHORISED agent" when {

      "agent requests(GET) view and the data hasn't been cached" in {
        viewClientDisplayNameAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for the client?")
          document.getElementById("header").text() must include("What display name do you want to use for the client?")
        }
      }

      "agent requests(GET) view pre-populated and the data has been cached" in {
        viewClientDisplayNameAuthorisedAgent(Some(ClientDisplayName("client display name"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for the client?")
          document.getElementById("clientDisplayName").`val`() must be("client display name")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "redirect to 'mandate details' Page" when {
      "valid form is submitted with valid data" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/overseas-client-question/ATED"))
          verify(mockDataCacheService, times(1)).cacheFormData[ClientDisplayName](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question.")
          document.getElementsByClass("error-notification").text() must include("You must answer client display name question.")
        }
      }

      "clientDisplayName field value is too long" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question.")
          document.getElementsByClass("error-notification").text() must include("A client display name cannot be more than 99 characters.")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  val service = "ated".toUpperCase
  val clientDisplayName = ClientDisplayName("client display name")

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewClientDisplayNameUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientDisplayNameController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewClientDisplayNameUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestClientDisplayNameController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewClientDisplayNameAuthorisedAgent(cachedData:  Option[ClientDisplayName] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestClientDisplayNameController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitClientDisplayNameAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[ClientDisplayName](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(clientDisplayName))
    val result = TestClientDisplayNameController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  object TestClientDisplayNameController extends ClientDisplayNameController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }
}
