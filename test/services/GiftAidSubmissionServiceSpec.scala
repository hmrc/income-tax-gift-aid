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

import connectors.GiftAidSubmissionConnector
import models.GiftAidSubmissionResponseModel
import models.submission.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class GiftAidSubmissionServiceSpec extends UnitTest {
  val nino = "AA123456A"
  val taxYear = 2021
  val monetaryValue: BigDecimal = 123456.89

  val giftAidSubmissionModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    GiftAidPaymentsModel(Seq(), monetaryValue, monetaryValue, monetaryValue, monetaryValue, monetaryValue),
    GiftsModel(Seq(), monetaryValue, monetaryValue, monetaryValue)
  )

  val connector: GiftAidSubmissionConnector = mock[GiftAidSubmissionConnector]

  val service = new GiftAidSubmissionService(connector)

  val returnedObject = Right(GiftAidSubmissionResponseModel("some-transaction-id"))

  ".submit" should {

    "return what is returned from the connector" in {
      (connector.submit(_: String, _: Int, _: GiftAidSubmissionModel)(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(returnedObject))

      val result = await(service.submit(nino, taxYear, giftAidSubmissionModel))

      result shouldBe returnedObject
    }

  }

}
