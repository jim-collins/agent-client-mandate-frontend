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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.GovernmentGatewayConnector
import uk.gov.hmrc.agentclientmandate.models.{IdentifierForDisplay, RetrieveClientAllocation}
import uk.gov.hmrc.agentclientmandate.utils.GovernmentGatewayConstants
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

import scala.concurrent.Future

class GovernmentGatewayConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost {
    override val hooks = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestGovernmentGatewayConnector extends GovernmentGatewayConnector {
    override val serviceUrl = baseUrl("government-gateway")
    val http: HttpGet with HttpPost = mockWSHttp
  }

  "GovernmentGatewayConnector" must {
    import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder._

    "retrieve client list" must {
      "work for an agent" in {
        val clientList = {
          val client1 = RetrieveClientAllocation(friendlyName = "Joe Bloggs", identifiersForDisplay =
            List(IdentifierForDisplay(`type` = GovernmentGatewayConstants.AtedReferenceNoType,
              value = "ATEDREF-123"),
              IdentifierForDisplay(`type` = "OTHER_REF", value = "OTHERREF-123")))
          val client2 = RetrieveClientAllocation(friendlyName = "Becky Smith", identifiersForDisplay =
            List(IdentifierForDisplay(`type` = "OTHER_REF", value = "OTHERREF-999")))
          val client3 = RetrieveClientAllocation(friendlyName = "Jane Smith", identifiersForDisplay =
            List(IdentifierForDisplay(`type` = GovernmentGatewayConstants.AtedReferenceNoType,
              value = "ATEDREF-999"),
              IdentifierForDisplay(`type` = "OTHER_REF", value = "OTHERREF-999")))

          List(client1, client2, client3)
        }
        val successResponse = Json.toJson(clientList)

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val ac = createRegisteredAgentAuthContext("User-Id", "username")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestGovernmentGatewayConnector.retrieveClientList
        await(result) must be(clientList)
      }

      "work for an agent with no data" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val ac = createRegisteredAgentAuthContext("User-Id", "username")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT, None)))

        val result = TestGovernmentGatewayConnector.retrieveClientList
        await(result).size must be(0)
      }

      "fail for an agent with a Bad request" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val ac = createRegisteredAgentAuthContext("User-Id", "username")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

        val result = TestGovernmentGatewayConnector.retrieveClientList
        val thrown = the[BadRequestException] thrownBy await(result)
        thrown.getMessage must include("Bad Request")
      }

      "fail for an agent with an INTERNAL_SERVER_ERROR" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val ac = createRegisteredAgentAuthContext("User-Id", "username")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))

        val result = TestGovernmentGatewayConnector.retrieveClientList
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Internal Server error")
      }
    }


  }
}
