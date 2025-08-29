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

package connectors

import config.AppConfig
import connectors.httpParsers.GiftAidSubmissionHttpParser.{GiftAidSubmissionResponse, _}
import models.submission.GiftAidSubmissionModel
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSubmissionConnector @Inject()(val appConfig: AppConfig,
                                           http: HttpClientV2
                                          )(implicit executionContext: ExecutionContext) extends DesConnector with Logging {
  //API#1390
  def submit(nino: String, taxYear: Int, submissionModel: GiftAidSubmissionModel
            )(implicit hc: HeaderCarrier): Future[GiftAidSubmissionResponse] = {

    val giftAidSubmissionUri: String = appConfig.desBaseUrl + s"/income-tax/nino/$nino/income-source/charity/" +
      s"annual/$taxYear"

    def desCall(implicit hc: HeaderCarrier): Future[GiftAidSubmissionResponse] = {
      logger.info(s"[GiftAidSubmissionConnector] post call to DES")
//      http.POST[GiftAidSubmissionModel, GiftAidSubmissionResponse](giftAidSubmissionUri, submissionModel)
      http.post(url"$giftAidSubmissionUri").withBody(Json.toJson(submissionModel)).execute[GiftAidSubmissionResponse]
    }

    desCall(desHeaderCarrier(giftAidSubmissionUri))
  }

}
