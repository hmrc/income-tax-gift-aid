/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import controllers.predicates.AuthorisedAction
import models.submission.GiftAidSubmissionModel
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.GiftAidSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSubmissionController @Inject()(
                                             service: GiftAidSubmissionService,
                                             auth: AuthorisedAction,
                                             cc: ControllerComponents
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def submit(nino: String, taxYear: Int): Action[AnyContent] = auth.async { user =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(user, user.session)
    val requestContent = user.body.asJson.flatMap(_.asOpt[GiftAidSubmissionModel])

    requestContent match {
      case Some(model) => service.submit(nino, taxYear, model).map {
        case Right(_) => NoContent
        case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
      }
      case None => Future.successful(BadRequest)
    }
  }

}
