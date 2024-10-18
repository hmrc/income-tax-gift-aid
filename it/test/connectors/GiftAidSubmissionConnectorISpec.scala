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

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import models.{ErrorBodyModel, ErrorModel, ErrorsBodyModel, GiftAidSubmissionResponseModel}
import models.submission.GiftAidSubmissionModel
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.IntegrationTest

class GiftAidSubmissionConnectorISpec extends IntegrationTest  {

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  lazy val connector: GiftAidSubmissionConnector = new GiftAidSubmissionConnector(appConfig("localhost"), httpClient)

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino: String = "AA123456A"
  val taxYear: Int = 2022

  val url= s"/income-tax/nino/$nino/income-source/charity/annual/$taxYear"

  val submissionModel: GiftAidSubmissionModel = GiftAidSubmissionModel(None, None)

  ".submit" should {

    "include internal headers" when {
      val requestBody = Json.toJson(submissionModel).toString()
      val responseBody: String = Json.stringify(Json.toJson(GiftAidSubmissionResponseModel("im-an-id-yay")))

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.submit(nino, taxYear, submissionModel)(hc))

        result mustBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new GiftAidSubmissionConnector(appConfig(externalHost), httpClient)


        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.submit(nino, taxYear,submissionModel)(hc))

        result mustBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }
    }

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
        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, taxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("oh-noes", "somethin' sank the ship")))
    }

    "return multiple errors when des does so" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = """{ "failures": [{ "code": "oh-noes", "reason": "somethin' sank the ship" }, { "code": "wut", "reason": "but how?!" }]}"""

      val result = {
        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, taxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorsBodyModel(Seq(
        ErrorBodyModel("oh-noes", "somethin' sank the ship"),
        ErrorBodyModel("wut", "but how?!")
      ))))
    }

  }

}
