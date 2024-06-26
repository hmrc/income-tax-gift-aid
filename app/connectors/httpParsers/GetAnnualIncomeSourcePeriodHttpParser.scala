/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.httpParsers

import models.giftAid.SubmittedGiftAidModel
import models.ErrorModel
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object GetAnnualIncomeSourcePeriodHttpParser extends APIParser with Logging {
    type GetAnnualIncomeSourcePeriod = Either[ErrorModel, SubmittedGiftAidModel]

    implicit object GetAnnualIncomeSourcePeriodReads extends HttpReads[GetAnnualIncomeSourcePeriod] {

      override def read(method: String, url: String, response: HttpResponse): GetAnnualIncomeSourcePeriod = {
        logCorrelationId(response)
        response.status match {
          case OK =>
            (response.json \ "charitableGivingAnnual").validate[SubmittedGiftAidModel].fold[GetAnnualIncomeSourcePeriod](
            jsonErrors => badSuccessJsonFromAPI, parsedModel => Right(parsedModel))
          case INTERNAL_SERVER_ERROR =>
            pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
            handleAPIError(response)
          case SERVICE_UNAVAILABLE =>
            pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
            handleAPIError(response)
          case UNPROCESSABLE_ENTITY =>
            pagerDutyLog(UNPROCESSABLE_ENTITY_FROM_API, logMessage(response))
            handleAPIError(response)
          case NOT_FOUND =>
            logger.info(logMessage(response))
            handleAPIError(response)
          case BAD_REQUEST =>
            pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
            handleAPIError(response)
          case UNAUTHORIZED =>
            pagerDutyLog(UNAUTHORIZED_RESPONSE_FROM_API, logMessage(response))
            //This scenario is missing in spec.But we do get 401 from downstream, convert to internal server error?
            handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
          case _ =>
            pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API)
            handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
        }
      }
    }
  }
