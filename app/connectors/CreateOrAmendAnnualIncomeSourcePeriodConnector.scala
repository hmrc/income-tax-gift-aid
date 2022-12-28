/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpParsers.CreateOrAmendAnnualIncomeSourcePeriodHttpParser.CreateOrAmendAnnualIncomeSourcePeriodResponse
import connectors.httpParsers.GiftAidSubmissionHttpParser._
import models.submission.GiftAidSubmissionModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendAnnualIncomeSourcePeriodConnector @Inject()(val appConfig: AppConfig,
                                                               http: HttpClient
                                                              )(implicit executionContext: ExecutionContext) extends IFConnector {
  val createOrAmendAnnualIncomeSourcePeriod = "1784"

  def submit(nino: String, taxYear: Int, submissionModel: GiftAidSubmissionModel
            )(implicit hc: HeaderCarrier): Future[CreateOrAmendAnnualIncomeSourcePeriodResponse] = {

    val taxYearParameter = convertSpecificTaxYear(taxYear)
    val giftAidSubmissionUri: String = appConfig.ifBaseUrl + s"/income-tax/$taxYearParameter/$nino/income-source/charity/annual"

    def IFCall(implicit hc: HeaderCarrier): Future[CreateOrAmendAnnualIncomeSourcePeriodResponse] = {
      http.POST[GiftAidSubmissionModel, CreateOrAmendAnnualIncomeSourcePeriodResponse](giftAidSubmissionUri, submissionModel)
    }

    IFCall(ifHeaderCarrier(giftAidSubmissionUri, createOrAmendAnnualIncomeSourcePeriod))
  }

}
