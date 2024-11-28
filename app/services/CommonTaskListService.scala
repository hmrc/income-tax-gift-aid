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

import config.AppConfig
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import models.mongo.JourneyAnswers
import models.tasklist.TaskStatus.{Completed, InProgress, NotStarted}
import models.tasklist._
import play.api.Logging
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      giftAidService: SubmittedGiftAidService,
                                      repository: JourneyAnswersRepository) extends Logging {

  def get(taxYear: Int, nino: String, mtdItId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaskListSection] = {

    val giftAid: Future[SubmittedGiftAidModel] = giftAidService.getSubmittedGiftAid(nino, taxYear).map {
      case Left(_) => SubmittedGiftAidModel(None, None)
      case Right(value) => value
    }

    val allGifts = for {
      giftAidModel <- giftAid
    } yield {
      SubmittedGiftAidModel(
        Some(giftAidModel.giftAidPayments.getOrElse(GiftAidPaymentsModel(None, None, None, None, None, None))),
        Some(giftAidModel.gifts.getOrElse(GiftsModel(None, None, None, None)))
      )
    }

    allGifts.flatMap { submittedGiftAidModel =>
      getTaskItems(mtdItId, taxYear, "gift-aid", submittedGiftAidModel).map(items =>
        TaskListSection(SectionTitle.CharitableDonationsTitle, items)
      )
    }
  }

  private def hasValue(values: Seq[Option[BigDecimal]]): Boolean = {
    values.exists(v => v.isDefined)
  }

  private def getTasksBasedOnLinearJourney(g: SubmittedGiftAidModel, taxYear: Int): Option[Seq[TaskListSectionItem]] = {
    val url: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/charity/check-donations-to-charity"
    val checkForAny = if (hasValue(Seq(
      g.giftAidPayments.flatMap(_.currentYear),
      g.giftAidPayments.flatMap(_.oneOffCurrentYear),
      g.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear),
      g.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear),
      g.gifts.flatMap(_.landAndBuildings),
      g.giftAidPayments.flatMap(_.nonUkCharities),
      g.gifts.flatMap(_.investmentsNonUkCharities),
      g.gifts.flatMap(_.sharesOrSecurities))
    )) {
      val giftAid = Some(TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, TaskStatus.Completed, Some(url)))
      val sharesOrSecurities = Some(TaskListSectionItem(TaskTitle.GiftsOfShares, TaskStatus.Completed, Some(url)))
      val landOrProperty = Some(TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, TaskStatus.Completed, Some(url)))
      val overseas = Some(TaskListSectionItem(TaskTitle.GiftsToOverseas, TaskStatus.Completed, Some(url)))
      Some(Seq[Option[TaskListSectionItem]](giftAid, sharesOrSecurities, landOrProperty, overseas).flatten)
    } else {
      None
    }

    checkForAny
  }

  private def getStoredStatus(answers: JourneyAnswers, taxYear: Int): Option[Seq[TaskListSectionItem]] = {

    val url: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/charity/check-donations-to-charity"

    val status: TaskStatus = answers.data.value("status").validate[TaskStatus].asOpt match {
      case Some(TaskStatus.Completed) => Completed
      case Some(TaskStatus.InProgress) => InProgress
      case _ =>
        logger.info("[CommonTaskListService][getStatus] status stored in an invalid format, setting as 'Not yet started'.")
        NotStarted
    }

    val statusCheck = if(appConfig.sectionCompletedQuestionEnabled) InProgress else Completed

    Some(Seq[TaskListSectionItem](
      TaskListSectionItem(TaskTitle.DonationsUsingGiftAid, statusCheck, Some(url)),
      TaskListSectionItem(TaskTitle.GiftsOfShares, statusCheck, Some(url)),
      TaskListSectionItem(TaskTitle.GiftsOfLandOrProperty, statusCheck, Some(url)),
      TaskListSectionItem(TaskTitle.GiftsToOverseas, statusCheck, Some(url))
    ))
  }

  private def getTaskItems(mtdItId: String, taxYear: Int, journey: String, giftAidModel: SubmittedGiftAidModel)
                          (implicit ec: ExecutionContext): Future[Option[Seq[TaskListSectionItem]]] =

    repository.get(mtdItId, taxYear, journey).map(answers =>
      if (answers.isDefined) {
        getStoredStatus(answers.get, taxYear)
      } else {
        getTasksBasedOnLinearJourney(giftAidModel, taxYear)
      }
    )
}
