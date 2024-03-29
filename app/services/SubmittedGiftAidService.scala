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

package services

import connectors.{GetAnnualIncomeSourcePeriodConnector, SubmittedGiftAidConnector}
import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils.specificTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubmittedGiftAidService @Inject()(submittedGiftAidConnector: SubmittedGiftAidConnector,
                                        getAnnualIncomeSourcePeriodConnector: GetAnnualIncomeSourcePeriodConnector ) {
  def getSubmittedGiftAid(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[SubmittedGiftAidResponse] = {
    if (taxYear >= specificTaxYear) {
      getAnnualIncomeSourcePeriodConnector.getAnnualIncomeSourcePeriod(nino, taxYear, Some(false))
    }
    else {
      submittedGiftAidConnector.getSubmittedGiftAid(nino, taxYear)
    }
  }
}
