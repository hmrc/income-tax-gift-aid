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

import config.MockAppConfig
import connectors.httpParsers.SubmittedGiftAidHttpParser.SubmittedGiftAidResponse
import models._
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import models.mongo.JourneyAnswers
import models.tasklist.TaskStatus.{Completed, InProgress, NotStarted}
import models.tasklist._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.Configuration
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsObject, Json}
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import utils.UnitTest

import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.Base64
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class CommonTaskListServiceSpec extends UnitTest {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  protected def mongoUri: String            = s"mongodb://localhost:27017/income-tax-gift-aid"
  protected def initTimeout: FiniteDuration = 5.seconds

  protected lazy val mongoComponent: MongoComponent = MongoComponent(mongoUri, initTimeout)
  private val configuration = Configuration("crypto.key" -> aesKey)

  private implicit val crypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  protected val repository = new JourneyAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  val giftAidService: SubmittedGiftAidService = mock[SubmittedGiftAidService]

  val service: CommonTaskListService = new CommonTaskListService(mockAppConfig, giftAidService, repository)

  val nino: String = "12345678"
  val taxYear: Int = 1234
  val mtdItId: String = "1234567890"

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

  val inProgressTaskSection: TaskListSection =
    TaskListSection(SectionTitle.CharitableDonationsTitle,
      Some(List(
        TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.InProgress,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.InProgress,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.InProgress,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
        TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.InProgress,
          Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
      ))
    )

  "CommonTaskListService.get" should {

    "return a full task list section model" in {

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullGiftAidResult))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe inProgressTaskSection
    }

    "return a minimal task list section model" in {

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe inProgressTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return a task list section model with 'In Progress' status" in {

      repository.clear(mtdItId, taxYear,"gift-aid")
      repository.set(JourneyAnswers(mtdItId, taxYear, "gift-aid", JsObject(Seq("status" -> Json.toJson(InProgress.entryName))), Instant.now()))

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return a task list section model with 'In Progress' status from 'Have you finished..' stored response" in {

      repository.clear(mtdItId, taxYear,"gift-aid")
      repository.set(JourneyAnswers(mtdItId, taxYear, "gift-aid", JsObject(Seq("status" -> Json.toJson(InProgress.entryName))), Instant.now()))

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe inProgressTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return a task list section model with 'Completed' status from 'Have you finished..' stored response and Section Completed Question Enabled is false" in {

      val sCQDisabledAppConfig: MockAppConfig = new MockAppConfig{
        override val sectionCompletedQuestionEnabled: Boolean = false
      }

      val service: CommonTaskListService = new CommonTaskListService(
        appConfig = sCQDisabledAppConfig,
        giftAidService = giftAidService,
        repository = repository
      )

      repository.clear(mtdItId, taxYear,"gift-aid")
      repository.set(JourneyAnswers(mtdItId, taxYear, "gift-aid", JsObject(Seq("status" -> Json.toJson(Completed.entryName))), Instant.now()))

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe inProgressTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return a task list section model with 'Not Started' status if stored status is invalid" in {

      repository.clear(mtdItId, taxYear,"gift-aid")
      repository.set(JourneyAnswers(mtdItId, taxYear, "gift-aid", JsObject(Seq("status" -> Json.toJson("not a valid status"))), Instant.now()))

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, NotStarted,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, NotStarted,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, NotStarted,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, NotStarted,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return 'Completed' status when Journey Answers are not defined and Section Completed Question Enabled is false" in  {

      val sCQDisabledAppConfig: MockAppConfig = new MockAppConfig{
        override val sectionCompletedQuestionEnabled: Boolean = false
      }

      val service: CommonTaskListService = new CommonTaskListService(
        appConfig = sCQDisabledAppConfig,
        giftAidService = giftAidService,
        repository = repository
      )

      repository.clear(mtdItId, taxYear,"gift-aid")

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))


      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, Completed,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return 'In Progress' status when Journey Answers are not defined and Section Completed Question Enabled is true" in  {

      val sCQDisabledAppConfig: MockAppConfig = new MockAppConfig{
        override val sectionCompletedQuestionEnabled: Boolean = true
      }

      val service: CommonTaskListService = new CommonTaskListService(
        appConfig = sCQDisabledAppConfig,
        giftAidService = giftAidService,
        repository = repository
      )

      repository.clear(mtdItId, taxYear,"gift-aid")

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(
          SubmittedGiftAidModel(Some(GiftAidPaymentsModel(None, Some(123.45), None, None, None, None)), None)
        )))


      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe inProgressTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfShares, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity")),
          TaskListSectionItem(TaskTitle.GiftsToOverseas, InProgress,
            Some("http://localhost:9308/1234/charity/check-donations-to-charity"))
        ))
      )
    }

    "return an empty task list section model" in {

      repository.clear(mtdItId, taxYear, "gift-aid")

      (giftAidService.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyGiftAidResult))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe TaskListSection(SectionTitle.CharitableDonationsTitle, None)
    }
  }
}
