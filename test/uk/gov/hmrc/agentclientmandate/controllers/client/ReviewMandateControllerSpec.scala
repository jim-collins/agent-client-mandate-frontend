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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, MandateReference}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ReviewMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ClientReviewAgentControllerSpec" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/review-mandate" in {
        val result = route(FakeRequest(GET, "/mandate/client/review-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

  }

  "redirect to login page for UNAUTHENTICATED client" when {

    "client requests(GET) for search mandate view" in {
      viewWithUnAuthenticatedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

  }

  "return review mandate view for AUTHORISED client" when {

    "client requests(GET) for review mandate view, and mandate has been cached on search mandate submit" in {
      val mandate = Mandate(id = "ABC123", createdBy = User("", None), agentParty = Party("ated-ref-no", "name", `type` = "Organisation", contactDetails = ContactDetails("", "")), clientParty = None, currentStatus = MandateStatus(status = Status.Pending, DateTime.now(), updatedBy = ""), statusHistory = None, Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "")))
      val returnData = ClientCache(mandateReference = Some(MandateReference("ABC123")), mandate = Some(mandate))
      viewWithAuthorisedClient(Some(returnData)) { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("Check that this is the agent that you want to appoint")
        document.getElementById("header").text() must include("Check that this is the agent that you want to appoint")
        document.getElementById("pre-heading").text() must include("Appoint an agent")
        document.getElementById("email-address").text() must be("Your email address")
        document.getElementById("agent-reference").text() must be("Agent reference")
        document.getElementById("submit").text() must be("Confirm and appoint agent")
      }
    }

  }

  "redirect to search mandate view for AUTHORISED client" when {

    "client requests(GET) for review mandate view, but mandate has not been cached on search mandate submit" in {
      viewWithAuthorisedClient() { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("/mandate/client/search-mandate"))
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestReviewMandateController extends ReviewMandateController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def viewWithUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestReviewMandateController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestReviewMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestReviewMandateController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
