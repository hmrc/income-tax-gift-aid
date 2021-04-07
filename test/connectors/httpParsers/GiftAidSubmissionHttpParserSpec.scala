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

package connectors.httpParsers

import utils.UnitTest
import connectors.httpParsers.GiftAidSubmissionHttpParser.CreateIncomeSourceHttpReads.read
import models.{DesErrorBodyModel, DesErrorModel, GiftAidSubmissionResponseModel}
import uk.gov.hmrc.http.HttpResponse
import play.api.http.Status._
import play.api.libs.json.Json

class GiftAidSubmissionHttpParserSpec extends UnitTest {

  def httpResponse(_status: Int, _body: String): HttpResponse = {
    class SomeResponse(
                        override val status: Int,
                        override val body: String
                      ) extends HttpResponse {
      override def allHeaders: Map[String, Seq[String]] = Map()
    }

    new SomeResponse(_status, _body)
  }

  ".read" should {

    "return a response model" when {

      "the response contains an OK and the correct body" in {
        val validBody = Json.prettyPrint(Json.obj("transactionReference" -> "some-transaction-id"))
        val result = read("POST", "/some-url", httpResponse(OK, validBody))

        result shouldBe Right(GiftAidSubmissionResponseModel("some-transaction-id"))
      }

    }

    "a DES error" when {

      "the response contains an OK, but a malformed body" in {
        val invalidBody = Json.prettyPrint(Json.obj("malformed" -> "not-what-im-looking-for"))
        val result = read("POST", "/some-url", httpResponse(OK, invalidBody))

        result shouldBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError))
      }

      "the response status is INTERNAL_SERVER_ERROR" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(INTERNAL_SERVER_ERROR, errorBody))

        result shouldBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is SERVICE_UNAVAILABLE" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(SERVICE_UNAVAILABLE, errorBody))

        result shouldBe Left(DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is BAD_REQUEST" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(BAD_REQUEST, errorBody))

        result shouldBe Left(DesErrorModel(BAD_REQUEST, DesErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is FORBIDDEN" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(FORBIDDEN, errorBody))

        result shouldBe Left(DesErrorModel(FORBIDDEN, DesErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is any other value" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "IMMA_LITTLE_TEAPOT", "reason" -> "short and stout"))
        val result = read("POST", "/some-url", httpResponse(IM_A_TEAPOT, errorBody))

        result shouldBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("IMMA_LITTLE_TEAPOT", "short and stout")))
      }

    }
  }

}
