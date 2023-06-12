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

import com.codahale.metrics.SharedMetricRegistries
import connectors.{GetAnnualIncomeSourcePeriodConnector, SubmittedGiftAidConnector}
import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, UnitTest}

import scala.concurrent.Future

class SubmittedGiftAidServiceSpec extends UnitTest {
  SharedMetricRegistries.clear()

  val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  val specificTaxYearPlusOne: Int = specificTaxYear + 1

  val connector: SubmittedGiftAidConnector = mock[SubmittedGiftAidConnector]
  val ifConnector: GetAnnualIncomeSourcePeriodConnector = mock[GetAnnualIncomeSourcePeriodConnector]
  val service: SubmittedGiftAidService = new SubmittedGiftAidService(connector, ifConnector)

  val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(
    Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67)
  )
  val gifts: GiftsModel = GiftsModel(Some(List("")), Some(12345.67), Some(12345.67) ,Some(12345.67))

  ".getSubmittedGiftAid" should {

    "return the connector response" in {

      val expectedResult: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

      (connector.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedGiftAid("12345678", 1234))

      result shouldBe expectedResult

    }

    "return the IfConnector response for specific tax year" in {

      val expectedResult: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

      (ifConnector.getAnnualIncomeSourcePeriod(_: String, _: Int, _: Option[Boolean])(_: HeaderCarrier))
        .expects("12345678", specificTaxYear, Some(false), *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedGiftAid("12345678", specificTaxYear))

      result shouldBe expectedResult

    }

    "return the IfConnector response for specific tax year plus one" in {

      val expectedResult: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

      (ifConnector.getAnnualIncomeSourcePeriod(_: String, _: Int, _: Option[Boolean])(_: HeaderCarrier))
        .expects("12345678", specificTaxYearPlusOne, Some(false), *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedGiftAid("12345678", specificTaxYearPlusOne))

      result shouldBe expectedResult

    }
  }
}
