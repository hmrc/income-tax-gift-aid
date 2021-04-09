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

package controllers

import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import models.giftaid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import models.{DesErrorBodyModel, DesErrorModel}
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.test.FakeRequest
import services.SubmittedGiftAidService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class SubmittedGiftAidControllerSpec extends TestUtils {

  val submittedGiftAidService: SubmittedGiftAidService = mock[SubmittedGiftAidService]
  val submittedGiftAidController = new SubmittedGiftAidController(submittedGiftAidService, mockControllerComponents,authorisedAction)
  val nino :String = "123456789"
  val mtdItID :String = "1234567890"
  val taxYear: Int = 1234
  val badRequestModel: DesErrorBodyModel = DesErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: DesErrorBodyModel = DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  private val fakeGetRequest = FakeRequest("GET", "/").withHeaders("mtditid" -> mtdItID)
  private val fakeGetRequestWithDifferentMTDITID = FakeRequest("GET", "/").withHeaders("mtditid" -> "123123123")

  val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(List(""), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  val gifts: GiftsModel = GiftsModel(List(""), Some(12345.67), Some(12345.67) ,Some(12345.67))

  def mockGetSubmittedGiftAidValid(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedGiftAidResponse]] = {
    val validSubmittedGiftAid: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))
    (submittedGiftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(validSubmittedGiftAid))
  }

  def mockGetSubmittedGiftAidBadRequest(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedGiftAidResponse]] = {
    val invalidSubmittedGiftAid: SubmittedGiftAidResponse = Left(DesErrorModel(BAD_REQUEST, badRequestModel))
    (submittedGiftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(invalidSubmittedGiftAid))
  }

  def mockGetSubmittedGiftAidNotFound(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedGiftAidResponse]] = {
    val invalidSubmittedGiftAid: SubmittedGiftAidResponse = Left(DesErrorModel(NOT_FOUND, notFoundModel))
    (submittedGiftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(invalidSubmittedGiftAid))
  }

  def mockGetSubmittedGiftAidServerError(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedGiftAidResponse]] = {
    val invalidSubmittedGiftAid: SubmittedGiftAidResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (submittedGiftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(invalidSubmittedGiftAid))
  }

  def mockGetSubmittedGiftAidServiceUnavailable(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedGiftAidResponse]] = {
    val invalidSubmittedGiftAid: SubmittedGiftAidResponse = Left(DesErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    (submittedGiftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(invalidSubmittedGiftAid))
  }

  "calling .getSubmittedGiftAid" should {

    "with existing GiftAid sources" should {

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedGiftAidValid()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe OK
      }

      "return an 401 response when called as an individual" in {
        val result = {
          mockAuth()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequestWithDifferentMTDITID)
        }
        status(result) shouldBe UNAUTHORIZED
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedGiftAidValid()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe OK
      }

    }

    "without existing GiftAid sources" should {

      "return an NotFound response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedGiftAidNotFound()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe NOT_FOUND
      }

      "return an NotFound response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedGiftAidNotFound()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe NOT_FOUND
      }

    }

    "with an invalid NINO" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedGiftAidBadRequest()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe BAD_REQUEST
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedGiftAidBadRequest()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe BAD_REQUEST
      }
    }

    "with something that causes and internal server error in DES" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedGiftAidServerError()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedGiftAidServerError()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "with an unavailable service" should {

      "return an Service_Unavailable response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedGiftAidServiceUnavailable()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return an Service_Unavailable response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedGiftAidServiceUnavailable()
          submittedGiftAidController.getSubmittedGiftAid(nino, taxYear)(fakeGetRequest)
        }
        status(result) shouldBe SERVICE_UNAVAILABLE
      }
    }

  }
}
