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

package connectors

import models.{DesErrorBodyModel, DesErrorModel, DesErrorsBodyModel, GiftAidSubmissionResponseModel}
import models.submission.GiftAidSubmissionModel
import play.api.http.Status._
import play.api.libs.json.Json
import utils.IntegrationTest

class GiftAidSubmissionConnectorISpec extends IntegrationTest {

  lazy val connector: GiftAidSubmissionConnector = app.injector.instanceOf[GiftAidSubmissionConnector]

  val nino: String = "AA123456A"
  val taxYear: Int = 2022

  val submissionModel: GiftAidSubmissionModel = GiftAidSubmissionModel(None, None)

  ".submit" should {

    "return a response model with the data des returns" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = Json.stringify(Json.toJson(GiftAidSubmissionResponseModel("im-an-id-yay")))

      val result = {
        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/charity/annual/$taxYear", OK, expectedBody, responseBody)
        connector.submit(nino, taxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
    }

    "return an error model when des returns an error" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = """{ "code": "oh-noes", "reason": "somethin' sank the ship" }"""

      val result = {
        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/charity/annual/$taxYear", INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, taxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("oh-noes", "somethin' sank the ship")))
    }

    "return multiple errors when des does so" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = """{ "failures": [{ "code": "oh-noes", "reason": "somethin' sank the ship" }, { "code": "wut", "reason": "but how?!" }]}"""

      val result = {
        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/charity/annual/$taxYear", INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, taxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorsBodyModel(Seq(
        DesErrorBodyModel("oh-noes", "somethin' sank the ship"),
        DesErrorBodyModel("wut", "but how?!")
      ))))
    }

  }

}
