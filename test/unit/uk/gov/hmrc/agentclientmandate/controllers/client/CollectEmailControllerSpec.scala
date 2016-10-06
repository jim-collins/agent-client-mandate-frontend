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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CollectEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectEmailController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/email" in {
        val result = route(FakeRequest(GET, "/mandate/client/email")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for collect email view" in {
        viewWithUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in {
        viewWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address?")
          document.getElementById("header").text() must include("What is your email address?")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("email_field").text() must be("Email address")
          document.getElementById("confirmEmail_field").text() must be("Confirm email address")
          document.getElementById("email").`val`() must be("")
          document.getElementById("confirmEmail").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data has been cached" in {
        val cached = ClientCache(email = Some(ClientEmail("aa@mail.com", "aa@mail.com")))
        viewWithAuthorisedClient(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address?")
          document.getElementById("email").`val`() must be("aa@mail.com")
          document.getElementById("confirmEmail").`val`() must be("aa@mail.com")
        }
      }

    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com", "confirmEmail" -> "aa@aa.com")
        val cachedData = ClientCache()
        val returnData = ClientCache(email = Some(ClientEmail("aa@aa.com", "aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = Some(cachedData), returnCache = returnData) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "valid form is submitted, while creating new client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com", "confirmEmail" -> "aa@aa.com")
        val returnData = ClientCache(email = Some(ClientEmail("aa@aa.com", "aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = None, returnCache = returnData) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "", "confirmEmail" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-list").text() must include("There is a problem with the confirm email address question")
          document.getElementsByClass("error-notification").text() must include("You must answer email address question")
          document.getElementsByClass("error-notification").text() must include("You must answer confirm email address question")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "email field and confirmEmail field has more than expected length" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "a" * 242, "confirmEmail" -> "a" * 242)
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-list").text() must include("There is a problem with the confirm email address question")
          document.getElementsByClass("error-notification").text() must include("Email address cannot be more than 241 characters.")
          document.getElementsByClass("error-notification").text() must include("Confirm email address cannot be more than 241 characters.")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "confirmEmail field value is not equal to email field value" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com", "confirmEmail" -> "aa@bb.com")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the confirm email address question")
          document.getElementsByClass("error-notification").text() must include("You must enter same email address in confirm email address")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com", "confirmEmail" -> "aa@invalid.com")
        submitWithAuthorisedClient(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("This email is invalid")
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockEmailService: EmailService = mock[EmailService]

  object TestCollectEmailController extends CollectEmailController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val emailService = mockEmailService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockEmailService)
  }

  def viewWithUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestCollectEmailController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded], cachedData: Option[ClientCache] = None, isValidEmail: Boolean = false, returnCache: ClientCache = ClientCache())(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockEmailService.validate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(isValidEmail))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId), Matchers.eq(returnCache))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnCache))
    val result = TestCollectEmailController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }


}
