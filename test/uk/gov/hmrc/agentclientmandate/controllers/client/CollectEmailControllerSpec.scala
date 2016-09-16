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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CollectEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectEmailController" must {

    "not return NOT_FOUND at route " when {
      "GET /agent-client-mandate/collect-email" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/collect-email")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        addEmailUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        clientAddEmail { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address?")
          document.getElementById("header").text() must include("What is your email address?")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("email_field").text() must be("Email address")
          document.getElementById("confirmEmail_field").text() must be("Confirm email address")
          document.getElementById("confirm_btn").text() must be("Continue")
        }
      }

    }

    "redirect to respective page " when {

      "valid form is submitted" in {
        continueWithAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/agent-client-mandate/search-mandate"))
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]

  object TestCollectEmailController extends CollectEmailController {
    override val authConnector = mockAuthConnector
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
  }

  def addEmailUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestCollectEmailController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def clientAddEmail(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestCollectEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestCollectEmailController.submit().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
