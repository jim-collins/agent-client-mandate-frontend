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

package uk.gov.hmrc.agentclientmandate.services

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.AuthBuilder
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class AgentClientMandateServiceSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier()


  def await[A](future: Future[A]): A = Await.result(future, 5 seconds)

  "AgentClientMandateService" should {

    "not create a mandate" when {

      "no agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(validFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

        val response = TestAgentClientMandateService.createMandate(service)
        await(response) must be(None)

      }

      "there is a problem while creating the mandate" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(validFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.createMandate(service)
        await(response) must be(None)

      }
    }

    "create a mandate" when {

      "agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        val mandate = createClientMandate("123456789")
        val respJson = Json.toJson(mandate)

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(validFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))

        val response = TestAgentClientMandateService.createMandate(service)
        await(response) must be(Some(mandate))

      }
    }

    "not fetch any mandate" when {

      "incorrect mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(None)
      }

      "arn is not found for the user" in {
        implicit val user = AuthBuilder.createNonRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        val mandate = createClientMandate("123456789")
        val respJson = Json.toJson(mandate)

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(validFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))

        val response = TestAgentClientMandateService.createMandate(mandateId)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must be("No valid agent business UTR found!")

        verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](Matchers.eq(validFormId))(Matchers.any(), Matchers.any())
        verify(mockAgentClientMandateConnector, times(0)).createMandate(Matchers.any())(Matchers.any())
      }

    }

    "fetch correct mandate" when {

      "correct mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val mandate = createClientMandate("123456789")
        val respJson = Json.toJson(mandate)
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(None)
      }

    }
  }

  def createClientMandate(id: String): CreateMandateResponse =
    CreateMandateResponse(mandateId = id)

  val mandateDto: MandateDto =
    MandateDto(
      PartyDto("JARN123456", "Joe Bloggs", "Organisation"),
      ContactDetailsDto("test@test.com", "0123456789"),
      ServiceDto(None, "ATED")
    )

  val mockAgentClientMandateConnector = mock[AgentClientMandateConnector]
  val mockDataCacheService = mock[DataCacheService]

  val validFormId: String = "some-from-id"
  val service = "ATED"
  val mandateId = "12345678"

  object TestAgentClientMandateService extends AgentClientMandateService {
    override val dataCacheService = mockDataCacheService
    override val agentClientMandateConnector = mockAgentClientMandateConnector
    override val formId: String = validFormId
  }

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
  }

}
