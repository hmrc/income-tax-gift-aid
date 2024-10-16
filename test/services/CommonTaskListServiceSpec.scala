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

package services

import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import models._
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import models.tasklist.{SectionTitle, TaskListSection, TaskListSectionItem, TaskStatus, TaskTitle}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class CommonTaskListServiceSpec extends UnitTest {

  val giftAidService: SubmittedGiftAidService = mock[SubmittedGiftAidService]

  val service: CommonTaskListService = new CommonTaskListService(mockAppConfig, giftAidService)

  val nino: String = "12345678"
  val taxYear: Int = 1234

  val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(
    Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67)
  )
  val gifts: GiftsModel = GiftsModel(Some(List("")), Some(12345.67), Some(12345.67) , Some(12345.67))

  val fullGiftAidResult: SubmittedGiftAidResponse = Right(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

  val emptyGiftAidResult: SubmittedGiftAidResponse = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

  // These urls will all direct to individual CYA pages rather than the existing CYA page once the journey has been split up.
  val fullTaskSection: TaskListSection =
    TaskListSection(SectionTitle.CharitableDonationsTitle,
      Some(List(
        TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.Completed,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.Completed,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.Completed,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.Completed,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
      ))
    )

  "CommonTaskListService.get" should {

    "return a full task list section model" in {

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullGiftAidResult))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe fullTaskSection
    }

    "return a minimal task list section model" in {

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return an empty task list section model" in {

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyGiftAidResult))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe TaskListSection(SectionTitle.CharitableDonationsTitle, None)
    }
  }
}
