/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.SubmittedGiftAidConnector
import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import models.giftaid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class SubmittedGiftAidServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val connector: SubmittedGiftAidConnector = mock[SubmittedGiftAidConnector]
  val service: SubmittedGiftAidService = new SubmittedGiftAidService(connector)

  val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(List(""), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  val gifts: GiftsModel = GiftsModel(List(""), Some(12345.67), Some(12345.67) ,Some(12345.67))

  ".getSubmittedGiftAid" should {

    "return the connector response" in {

      val expectedResult: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

      (connector.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedGiftAid("12345678", 1))

      result shouldBe expectedResult

    }
  }
}
