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

package services

import connectors.{CreateOrAmendAnnualIncomeSourcePeriodConnector, GiftAidSubmissionConnector}
import connectors.httpParsers.GiftAidSubmissionHttpParser.GiftAidSubmissionResponse
import models.submission.GiftAidSubmissionModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils.specificTaxYear

import javax.inject.Inject
import scala.concurrent.Future

class GiftAidSubmissionService @Inject()(connector: GiftAidSubmissionConnector,
                                         ifConnector:CreateOrAmendAnnualIncomeSourcePeriodConnector) {

  def submit(nino: String, taxYear: Int, giftAidSubmissionModel: GiftAidSubmissionModel)(implicit hc: HeaderCarrier): Future[GiftAidSubmissionResponse] = {
    if(taxYear >= specificTaxYear)
      {
        ifConnector.submit(nino, taxYear, giftAidSubmissionModel)
      }
    else
      {
        connector.submit(nino, taxYear, giftAidSubmissionModel)(hc)
      }

  }

}
