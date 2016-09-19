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

case class ClientEmail(email: String, confirmEmail: String)

object ClientEmail {
  implicit val formats = Json.format[ClientEmail]
}

object ClientEmailForm {
  val emailLength = 241
  val clientEmailForm =
    Form(
      mapping(
        "email" -> text
          .verifying(Messages("ated.contact-details-email.length"), x => x.isEmpty || (x.nonEmpty && x.length <= emailLength)),
        "confirmEmail" -> text
          .verifying(Messages("ated.contact-details-email.length"), x => x.isEmpty || (x.nonEmpty && x.length <= emailLength))
      )
      (ClientEmail.apply)(ClientEmail.unapply)
    )
}

case class MandateReference(mandateRef: String)

object MandateReference {
  implicit val formats = Json.format[MandateReference]
}

object MandateReferenceForm {
  val mandateRefLength = 35

  val mandateRefForm =
    Form(
      mapping(
        "mandateRef" -> text
          .verifying(Messages("ated.contact-details-email.length"), x => x.isEmpty || (x.nonEmpty && x.length <= mandateRefLength))
      )
      (MandateReference.apply)(MandateReference.unapply)
    )
}

case class ClientCache(email: Option[ClientEmail] = None, mandate: Option[MandateReference] = None)

object ClientCache {
  implicit val formats = Json.format[ClientCache]
}
