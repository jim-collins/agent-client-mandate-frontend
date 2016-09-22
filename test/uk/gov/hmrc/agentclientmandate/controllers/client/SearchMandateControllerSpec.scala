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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail, MandateReference}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class SearchMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "SearchMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/client/search-mandate" in {
        val result = route(FakeRequest(GET, "/mandate/client/search-mandate")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

  }

  "redirect to login page for UNAUTHENTICATED client" when {

    "client requests(GET) for search mandate view" in {
      viewUnAuthenticatedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

  }

  "redirect to unauthorised page for UNAUTHORISED client" when {

    "client requests for search mandate view" in {
      viewUnAuthenticatedClient { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }

  }

  "return search mandate view for AUTHORISED client" when {

    "client requests(GET) for search mandate view" in {
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("What is the agent's reference?")
        document.getElementById("header").text() must include("What is the agent's reference?")
        document.getElementById("mandateRef").`val`() must be("")
        document.getElementById("submit").text() must be("Continue")
      }
    }

  }

  "redirect to respective page " when {

    "valid form is submitted, while updating existing client cache object" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "ABC123")
      val cachedData = ClientCache(email = Some(ClientEmail("aa@aa.com", "aa@aa.com")))
      val mandate = Mandate(id = "ABC123", createdBy = User("credid", "Joe Bloggs", None), agentParty = Party("ated-ref-no", "name", `type` = "Organisation", contactDetails = ContactDetails("", "")), clientParty = None, currentStatus = MandateStatus(status = Status.Pending, DateTime.now(), updatedBy = ""), statusHistory = None, Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "")))
      val returnData = ClientCache(email = Some(ClientEmail("aa@aa.com", "aa@aa.com")), mandate = Some(mandate))
      submitWithAuthorisedClient(fakeRequest, Some(cachedData), Some(mandate), returnData) { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("/mandate/client/review-mandate"))
        verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "valid form is submitted, but the cache doesn't exist - this redirects to enter your email page" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "ABC123")
      val mandate = Mandate(id = "ABC123", createdBy = User("", None), agentParty = Party("ated-ref-no", "name", `type` = "Organisation", contactDetails = ContactDetails("", "")), clientParty = None, currentStatus = MandateStatus(status = Status.Pending, DateTime.now(), updatedBy = ""), statusHistory = None, Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "")))
      val returnData = ClientCache(mandateReference = Some(MandateReference("ABC123")), mandate = Some(mandate))
      submitWithAuthorisedClient(fakeRequest, None, Some(mandate), returnData) { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("/mandate/client/review-mandate"))
        verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

  }

  "returns BAD_REQUEST" when {
    "empty form is submitted" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "")
      submitWithAuthorisedClient(fakeRequest) { result =>
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-list").text() must include("There is a problem with the agent reference question")
        document.getElementsByClass("error-notification").text() must include("You must answer agent reference question")
        verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "mandateRef field has more than expected length" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "a" * 11)
      submitWithAuthorisedClient(fakeRequest) { result =>
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-list").text() must include("There is a problem with the agent reference question")
        document.getElementsByClass("error-notification").text() must include("Agent reference cannot be more than 10 characters.")
        verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "invalid agent reference is passed" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "invalidId")
      submitWithAuthorisedClient(fakeRequest) { result =>
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-list").text() must include("The agent reference you entered was not found. Please check and try again.")
        verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestSearchMandateController extends SearchMandateController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val mandateService = mockMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockMandateService)
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestSearchMandateController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestSearchMandateController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 cachedData: Option[ClientCache] = None,
                                 mandate: Option[Mandate] = None,
                                 returnCache: ClientCache = ClientCache())(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandate))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId), Matchers.eq(returnCache))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnCache))
    val result = TestSearchMandateController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
