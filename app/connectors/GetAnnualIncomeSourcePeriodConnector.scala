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
import connectors.httpParsers.GetAnnualIncomeSourcePeriodHttpParser._
import connectors.httpParsers.GetAnnualIncomeSourcePeriodHttpParser.GetAnnualIncomeSourcePeriod
import play.api.Logging

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.TaxYearUtils.convertSpecificTaxYear

import scala.concurrent.{ExecutionContext, Future}

class GetAnnualIncomeSourcePeriodConnector @Inject()(val http: HttpClientV2,
                                                     val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector with Logging{
  val GetAnnualIncomeSourcePeriod = "1785"
  def getAnnualIncomeSourcePeriod(nino: String,
                                  taxYear: Int,
                                  deletedPeriod: Option[Boolean])(implicit hc: HeaderCarrier): Future[GetAnnualIncomeSourcePeriod] = {
    val taxYearParameter = convertSpecificTaxYear(taxYear)
    val incomeSourcesUri: String = appConfig.ifBaseUrl + s"/income-tax/$taxYearParameter/$nino/income-source/charity/annual?deleteReturnPeriod=false"
    logger.info(s"[GetAnnualIncomeSourcePeriodConnector] GET call to IF")
    http.get(url"$incomeSourcesUri")(ifHeaderCarrier(incomeSourcesUri, GetAnnualIncomeSourcePeriod)).execute[GetAnnualIncomeSourcePeriod]

  }
}

