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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import models.submission.GiftAidSubmissionModel
import models.{ErrorBodyModel, ErrorModel, ErrorsBodyModel, GiftAidSubmissionResponseModel}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{IntegrationTest, TaxYearUtils}
import utils.TaxYearUtils.convertSpecificTaxYear

class CreateOrAmendIncomeSourcePeriodConnectorISpec extends IntegrationTest {

  lazy val connector: CreateOrAmendAnnualIncomeSourcePeriodConnector = app.injector.instanceOf[CreateOrAmendAnnualIncomeSourcePeriodConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(ifHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val nino: String = "AA123456A"
  val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  val specificTaxYearPlusOne: Int = specificTaxYear + 1
  val taxYearParameter: String = convertSpecificTaxYear(specificTaxYear)
  val taxYearParameterPlusOne: String = convertSpecificTaxYear(specificTaxYearPlusOne)
  val url = s"/income-tax/$taxYearParameter/$nino/income-source/charity/annual"
  val urlPlusOne = s"/income-tax/$taxYearParameterPlusOne/$nino/income-source/charity/annual"
  val submissionModel: GiftAidSubmissionModel = GiftAidSubmissionModel(None, None)

  ".submit" should {

    "include internal headers" when {
      val requestBody = Json.toJson(submissionModel).toString()
      val responseBody: String = Json.stringify(Json.toJson(GiftAidSubmissionResponseModel("im-an-id-yay")))

      val headersSentToIF = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for IF is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToIF)

        val result = await(connector.submit(nino, specificTaxYear, submissionModel)(hc))

        result mustBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }

      "the host for IF is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateOrAmendAnnualIncomeSourcePeriodConnector(appConfig(externalHost), httpClient)


        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToIF)

        val result = await(connector.submit(nino, specificTaxYear, submissionModel)(hc))

        result mustBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }
    }

    "return a success response" when {

      "IF returns a 200 for specific tax year" in {
        val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
        val responseBody: String = Json.stringify(Json.toJson(GiftAidSubmissionResponseModel("im-an-id-yay")))

        val result = {
          stubPostWithResponseBody(url, OK, expectedBody, responseBody)
          connector.submit(nino, specificTaxYear, submissionModel)(emptyHeaderCarrier)
        }

        await(result) shouldBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }

      "IF returns a 200 for specific tax year plus one" in {
        val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
        val responseBody: String = Json.stringify(Json.toJson(GiftAidSubmissionResponseModel("im-an-id-yay")))

        val result = {
          stubPostWithResponseBody(urlPlusOne, OK, expectedBody, responseBody)
          connector.submit(nino, specificTaxYearPlusOne, submissionModel)(emptyHeaderCarrier)
        }

        await(result) shouldBe Right(GiftAidSubmissionResponseModel("im-an-id-yay"))
      }
    }

    "return an error model when IF returns an error" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = """{ "code": "oh-noes", "reason": "somethin' sank the ship" }"""

      val result = {
        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, specificTaxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("oh-noes", "somethin' sank the ship")))
    }

    "return multiple errors when IF does so" in {
      val expectedBody: String = Json.stringify(Json.toJson(submissionModel))
      val responseBody: String = """{ "failures": [{ "code": "oh-noes", "reason": "somethin' sank the ship" }, { "code": "wut", "reason": "but how?!" }]}"""

      val result = {
        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, expectedBody, responseBody)
        connector.submit(nino, specificTaxYear, submissionModel)(emptyHeaderCarrier)
      }

      await(result) shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorsBodyModel(Seq(
        ErrorBodyModel("oh-noes", "somethin' sank the ship"),
        ErrorBodyModel("wut", "but how?!")
      ))))
    }

  }

}
