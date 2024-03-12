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
import connectors.httpParsers.GiftAidSubmissionHttpParser._
import models.submission.GiftAidSubmissionModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSubmissionConnector @Inject()(val appConfig: AppConfig,
                                           http: HttpClient
                                          )(implicit executionContext: ExecutionContext) extends DesConnector {
  //API#1390
  def submit(nino: String, taxYear: Int, submissionModel: GiftAidSubmissionModel
            )(implicit hc: HeaderCarrier): Future[GiftAidSubmissionResponse] = {

    val giftAidSubmissionUri: String = appConfig.desBaseUrl + s"/income-tax/nino/$nino/income-source/charity/" +
      s"annual/$taxYear"

    def desCall(implicit hc: HeaderCarrier): Future[GiftAidSubmissionResponse] = {
      http.POST[GiftAidSubmissionModel, GiftAidSubmissionResponse](giftAidSubmissionUri, submissionModel)
    }

    desCall(desHeaderCarrier(giftAidSubmissionUri))
  }

}
