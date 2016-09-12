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

package uk.gov.hmrc.agentclientmandate.viewModelsAndForms

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json.Json

case class SearchClientMandate(id: String)

object SearchClientMandate {
  implicit val formats = Json.format[SearchClientMandate]
}

object SearchClientMandateForm {
  val searchClientMandateForm =
    Form(
      mapping(
        "id" -> text.verifying(Messages("client.search-mandate.error.id"), id => id.nonEmpty)
      )(SearchClientMandate.apply)(SearchClientMandate.unapply)
    )
}

case class ApproveClientMandate(approved: Option[Boolean] = None)

object ApproveClientMandate {
  implicit val formats = Json.format[ApproveClientMandate]
}

object ApproveClientMandateForm {
  val approveClientMandateForm =
    Form(
      mapping(
        "approved" -> optional(boolean).verifying(Messages("client.approve-mandate.error.approved"),x => x.isDefined)
      )(ApproveClientMandate.apply)(ApproveClientMandate.unapply)
    )
}