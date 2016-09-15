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



class CollectClientBusinessDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectClientBusinessDetailsController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/collect-client-business-details/:service" in {

        val result = route(FakeRequest(GET, s"/agent-client-mandate/collect-client-business-details/$service")).get
        status(result) mustNot be(NOT_FOUND)


      }

      "POST /agent-client-mandate/collect-client-business-details/:service" in {

        val result = route(FakeRequest(POST, s"/agent-client-mandate/collect-client-business-details/$service")).get
        status(result) mustNot be(NOT_FOUND)

      }

    }

    "return business details page" when {
      "requested GET for business details page" in {
        viewCollectAgentDetailsAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What are your clients company details?")
          document.getElementById("header").text() must include("Add a client")
          document.getElementById("continue").text() must include("Continue")

        }

      }
    }

    "stay on page when form with error" when {
      "requested POST for business details page" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("businessName" -> "", "utr" -> "")
        submitCollectAgentDetailsAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)


        }

      }
    }
    "redirect to review business details page" when {
      "requested POST for business details page" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("businessName" -> "a", "utr" -> "1111111111")
        submitCollectAgentDetailsAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(OK)
        }

      }
    }

  }


  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val service = "ated".toUpperCase
  val formId1 = "businessName"

  object TestCollectClientBusinessDetailsControllerSpec extends CollectClientBusinessDetailsController {
    val authConnector = mockAuthConnector
    val dataCacheService = mockDataCacheService
    val formId = formId1
  }

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }


  def viewCollectAgentDetailsAuthorisedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCollectClientBusinessDetailsControllerSpec.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitCollectAgentDetailsAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCollectClientBusinessDetailsControllerSpec.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)


  }
}

