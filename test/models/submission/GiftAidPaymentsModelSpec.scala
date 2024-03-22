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

package models.submission

import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class GiftAidPaymentsModelSpec extends UnitTest {
  val monetaryValue: BigDecimal = 1234.56

  lazy val modelMax: GiftAidPaymentsModel = GiftAidPaymentsModel(
    Seq("string1", "string2"),
    monetaryValue, monetaryValue, monetaryValue, monetaryValue, monetaryValue
  )

  lazy val modelMin: GiftAidPaymentsModel = GiftAidPaymentsModel(None, None, None, None, None, None)

  lazy val jsonMax: JsObject = Json.obj(
    "nonUkCharitiesCharityNames" -> Json.arr("string1", "string2"),
    "currentYear" -> 1234.56,
    "oneOffCurrentYear" -> 1234.56,
    "currentYearTreatedAsPreviousYear" -> 1234.56,
    "nextYearTreatedAsCurrentYear" -> 1234.56,
    "nonUkCharities" -> 1234.56
  )

  "GiftAidPaymentsModel" should {

    "parse the max model to json" in {
      Json.toJson(modelMax) shouldBe jsonMax
    }

    "parse the max model from json" in {
      jsonMax.as[GiftAidPaymentsModel] shouldBe modelMax
    }

    "parse the min model to json" in {
      Json.toJson(modelMin) shouldBe Json.obj()
    }

    "parse the min model from json" in {
      Json.obj().as[GiftAidPaymentsModel] shouldBe modelMin
    }

  }

}
