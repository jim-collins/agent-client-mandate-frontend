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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, Mandates}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AcceptClientControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach{


  "AcceptClientController" must {

    "not return NOT_FOUND at route " when {
      "Get /mandate/agent/accept-client" in{
        val result = route(FakeRequest(GET, s"/mandate/agent/accept-client/$mandateId")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "return agen-client-summary page view for agent" when {

      "client requests(GET) for check client details view" in {
        viewAuthorisedAgent(true) { result =>
          status(result) must be(SEE_OTHER)
        }
      }
    }

    "throw an exception" when {
      "the activate clinet service fails and return false" in {
        viewAuthorisedAgent() { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("Failed to accept client")
        }
      }
    }


  }
  val mandateId = "AS123456"

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]

  object TestAcceptClientController extends AcceptClientController {
    override val authConnector = mockAuthConnector
    override val agentClientMandateService = mockAgentClientMandateService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
  }

  def viewAuthorisedAgent(clientAccepted: Boolean = false)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockAgentClientMandateService.acceptClient(Matchers.eq(mandateId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(clientAccepted)
    val result = TestAcceptClientController.view(mandateId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

 }
