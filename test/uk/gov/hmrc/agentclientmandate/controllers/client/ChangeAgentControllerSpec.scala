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

class ChangeAgentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestChangeAgentController extends ChangeAgentController {
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
    val result = TestChangeAgentController.view.apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedClient(userId, mockAuthConnector)
    val result = TestChangeAgentController.view.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(request: FakeRequest[AnyContentAsJson], test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestChangeAgentController.view.apply(SessionBuilder.updateRequestWithSession(request, userId))
    test(result)
  }

//  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
//    val userId = s"user-${UUID.randomUUID}"
//    implicit val hc: HeaderCarrier = HeaderCarrier()
//    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
//    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
//
//    val result = TestChangeAgentController.confirm("1", "agent ltd").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
//    test(result)
//  }

  "ChangeAgentController" must {
    "not return NOT_FOUND at route " when {
      "GET /mandate/client/change-agent" in {
        val result = route(FakeRequest(GET, "/mandate/client/change-agent")).get
        status(result) mustNot be(NOT_FOUND)
      }

//      "POST /mandate/agent/remove-agent/1" in {
//        val result = route(FakeRequest(POST, s"/mandate/client/change-agent")).get
//        status(result) mustNot be(NOT_FOUND)
//      }
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

    "return 'change agent question' view for AUTHORISED agent" when {
      "client requests(GET) for 'change agent question' view" in {

        val hc = new HeaderCarrier()
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")))
        val request = FakeRequest(GET, "/client/change-agent").withJsonBody(Json.toJson("""{}"""))
        viewAuthorisedClient(request, { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you want to appoint another agent to act on your behalf?")
          document.getElementById("header").text() must include("Do you want to appoint another agent to act on your behalf?")
          document.getElementById("pre-heading").text() must be("Manage your ATED service")
          document.getElementById("yesNo_legend").text() must be("Do you want to appoint another agent to act on your behalf?")
          document.getElementById("submit").text() must be("Confirm")
        })
      }
    }
  }
}
