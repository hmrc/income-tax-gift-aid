/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import models.submission._
import models.{DesErrorBodyModel, DesErrorModel, DesErrorsBodyModel, GiftAidSubmissionResponseModel}
import play.api.http.Status._
import play.api.libs.json.Json
import services.GiftAidSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class GiftAidSubmissionControllerSpec extends UnitTest {
  val service: GiftAidSubmissionService = mock[GiftAidSubmissionService]

  val controller = new GiftAidSubmissionController(
    service,
    authorisedAction,
    mockControllerComponents
  )

  val nino: String = "AA123456A"
  val taxYear: Int = 2022
  val monetaryValue: BigDecimal = 1234.56

  lazy val submissionModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    GiftAidPaymentsModel(
      Seq("Muh Charity"),
      monetaryValue,
      monetaryValue,
      monetaryValue,
      monetaryValue,
      monetaryValue
    ),
    GiftsModel(
      Seq("Muh other charity"),
      monetaryValue,
      monetaryValue,
      monetaryValue
    )
  )

  ".submit" should {

    "return a NoContent (204)" when {

      "the service returns a successful submission model" in {
        val result = {
          mockAuth()
          (service.submit(_: String, _: Int, _: GiftAidSubmissionModel)(_: HeaderCarrier))
            .expects(*, *, *, *)
            .returning(Future.successful(Right(GiftAidSubmissionResponseModel("someTransactionId"))))

          controller.submit(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(submissionModel)))
        }

        status(result) shouldBe NO_CONTENT
      }

    }

    "return a BadRequest (400)" when {

      "the request body cannot be parsed as a submission" in {
        val result = {
          mockAuth()
          controller.submit(nino, taxYear)(fakeRequest)
        }

        status(result) shouldBe BAD_REQUEST
      }

    }

    "return with the status and message of the DesError" when {

      "the submission service returns a single des error" which {
        lazy val result = {
          mockAuth()
          (service.submit(_: String, _: Int, _: GiftAidSubmissionModel)(_: HeaderCarrier))
            .expects(*, *, *, *)
            .returning(Future.successful(Left(DesErrorModel(IM_A_TEAPOT, DesErrorBodyModel("SUMET_WENT_RONG", "Dude can't spell")))))

          controller.submit(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(submissionModel)))
        }

        "has a status of IM_A_TEAPOT" in {
          status(result) shouldBe IM_A_TEAPOT
        }

        "has the correct error body" in {
          val expectedResult = Json.obj("code" -> "SUMET_WENT_RONG", "reason" -> "Dude can't spell")
          jsonBodyOf(result) shouldBe expectedResult
        }
      }

      "the submission service returns multiple des errors" which {
        lazy val result = {
          mockAuth()
          (service.submit(_: String, _: Int, _: GiftAidSubmissionModel)(_: HeaderCarrier))
            .expects(*, *, *, *)
            .returning(Future.successful(Left(DesErrorModel(IM_A_TEAPOT, DesErrorsBodyModel(
              Seq(
                DesErrorBodyModel("NOOOOOOOO", "He was my father"),
                DesErrorBodyModel("IM_UP_HERE", "I have the high ground")
              )
            )))))

          controller.submit(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(submissionModel)))
        }

        "has a status of IM_A_TEAPOT" in {
          status(result) shouldBe IM_A_TEAPOT
        }

        "has the correct error body" in {
          val expectedResult = Json.obj(
            "failures" -> Json.arr(
              Json.obj(
                "code" -> "NOOOOOOOO",
                "reason" -> "He was my father"
              ),
              Json.obj(
                "code" -> "IM_UP_HERE",
                "reason" -> "I have the high ground"
              )
            )
          )

          jsonBodyOf(result) shouldBe expectedResult
        }
      }

    }

  }

}
