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

package uk.gov.hmrc.agentclientmandate.connectors


import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.models.{ClientMandateDto, ContactDetailsDto, PartyDto, ServiceDto}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost}

import scala.concurrent.Future

class AgentClientMandateConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost with WSDelete {
    override val hooks = NoneRequired
  }

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val mockWSHttp = mock[MockHttp]

  object TestAgentClientMandateConnector extends AgentClientMandateConnector {
    override def serviceUrl: String = baseUrl("agent-client-mandate")

    override def http: HttpGet with HttpPost with HttpDelete = mockWSHttp
  }

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val mandateId = "12345678"

  val mandateDto: ClientMandateDto =
    ClientMandateDto(
      PartyDto("JARN123456", "Joe Bloggs", "Organisation"),
      ContactDetailsDto("test@test.com", "0123456789"),
      ServiceDto("ATED")
    )

  "AgentClientMandateConnector" must {

    "have a service url" in {
      AgentClientMandateConnector.serviceUrl must be("http://localhost:9960")
    }

    "create a mandate" in {
      val successResponse = Json.toJson(mandateDto)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.createMandate(mandateDto)
      await(response).status must be(OK)

    }

    "fetch a valid mandate" in {
      val successResponse = Json.toJson(mandateDto)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchMandate(mandateId)
      await(response).status must be(OK)

    }

  }


}
