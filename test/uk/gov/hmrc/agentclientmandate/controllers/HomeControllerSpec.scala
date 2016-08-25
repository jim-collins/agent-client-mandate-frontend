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

package uk.gov.hmrc.agentclientmandate.controllers

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import play.api.mvc.Result
import uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "HomeController" must {

    "not return NOT_FOUND at route " when {

      "GET /agent-client-mandate/home" in {
        val result = route(FakeRequest(GET, "/agent-client-mandate/home")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect UNAUTHENTICATED users to go to login page" when {

      "users tries to access secured page - home page" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
        val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSessionNoUser)
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("http://localhost:9025/gg/sign-in")
      }

    }

    "redirect UNAUTHORISED users to unauthorised page" when {

      "users log in with correct credentials but wrong authorisations - status is 303" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockUnAuthorisedClient(userId, mockAuthConnector)
        val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }

    }

    "login authorised users with Org account to go to client home page" when {

      "client users log in with correct credentials and authorisations" in {
        homeWithAuthorisedUser { result =>
          redirectLocation(result) must be(None)
          status(result) must be(OK)
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]

  override def beforeEach() {
    reset(mockAuthConnector)
  }

  object TestHomeController extends HomeController {
    override val authConnector = mockAuthConnector
  }

  def homeWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestHomeController.home().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
