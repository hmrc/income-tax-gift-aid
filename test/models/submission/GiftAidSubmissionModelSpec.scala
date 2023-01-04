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

package models.submission

import play.api.libs.json.Json
import utils.UnitTest

class GiftAidSubmissionModelSpec extends UnitTest {

  implicit def nonOptionalToOptional[T](nonOptionalValue: T): Option[T] = Some(nonOptionalValue)

  val exampleJsonMax: String =
    """{
      |   "giftAidPayments": {
      |      "nonUkCharitiesCharityNames": [
      |         "abcdefghijklmnopqr"
      |      ],
      |      "currentYear": 23426505146.99,
      |      "oneOffCurrentYear": 80331713889.99,
      |      "currentYearTreatedAsPreviousYear": 44753493320.99,
      |      "nextYearTreatedAsCurrentYear": 88970014371.99,
      |      "nonUkCharities": 77143081269.00
      |   },
      |   "gifts": {
      |      "investmentsNonUkCharitiesCharityNames": [
      |         "abcdefghijklmnopqr"
      |      ],
      |      "landAndBuildings": 11200049718.00,
      |      "sharesOrSecurities": 82198960626.00,
      |      "investmentsNonUkCharities": 24966390172.00
      |   }
      |}""".stripMargin

  val modelMax: GiftAidSubmissionModel = GiftAidSubmissionModel(
    GiftAidPaymentsModel(
      Seq("abcdefghijklmnopqr"),
      Some(23426505146.99),
      Some(80331713889.99),
      Some(44753493320.99),
      Some(88970014371.99),
      Some(77143081269.00)
    ),
    GiftsModel(
      Seq("abcdefghijklmnopqr"),
      Some(11200049718.00),
      Some(82198960626.00),
      Some(24966390172.00)
    )
  )

  val modelMin: GiftAidSubmissionModel = GiftAidSubmissionModel(None, None)

  "GiftAidSubmissionModel" should {

    "correctly parse the max model to json" in {
      Json.toJson(modelMax) shouldBe Json.parse(exampleJsonMax)
    }

    "correctly parse the max model from json" in {
      Json.parse(exampleJsonMax).as[GiftAidSubmissionModel] shouldBe modelMax
    }

    "correctly parse the min model to json" in {
      Json.toJson(modelMin) shouldBe Json.obj()
    }

    "correctly parse the min model from json" in {
      Json.parse("{}").as[GiftAidSubmissionModel] shouldBe modelMin
    }

  }

}
