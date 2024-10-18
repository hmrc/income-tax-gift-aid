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
import models.tasklist._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      giftAidService: SubmittedGiftAidService) {

  def get(taxYear: Int, nino: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaskListSection] = {

    val giftAid: Future[SubmittedGiftAidModel] = giftAidService.getSubmittedGiftAid(nino, taxYear).map {
      case Left(_) => SubmittedGiftAidModel(None, None)
      case Right(value) => value
    }

    val allGifts = for {
      ga <- giftAid
    } yield {
      SubmittedGiftAidModel(
        Some(ga.giftAidPayments.getOrElse(GiftAidPaymentsModel(None, None, None, None, None, None))),
        Some(ga.gifts.getOrElse(GiftsModel(None, None, None, None)))
      )
    }

    allGifts.map { g =>

      val tasks: Option[Seq[TaskListSectionItem]] = {

        val optionalTasks: Seq[TaskListSectionItem] = getTasksBasedOnLinearJourney(g, taxYear)

        if (optionalTasks.nonEmpty) {
          Some(optionalTasks)
        } else {
          None
        }
      }

      TaskListSection(SectionTitle.CharitableDonationsTitle, tasks)
    }
  }

  private def hasValue(values: Seq[Option[BigDecimal]]): Boolean = {
    values.exists(v => v.isDefined)
  }

  /**
   * TODO : once the journeys are split into mini journeys.
   *  Below function `getTasksBasedOnLinearJourney` can be deleted and `getTasksBasedOnMiniJourney` can be used
   */

  private def getTasksBasedOnLinearJourney(g: SubmittedGiftAidModel, taxYear: Int): Seq[TaskListSectionItem] = {
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
      Seq[Option[TaskListSectionItem]](giftAid, sharesOrSecurities, landOrProperty, overseas).flatten
    } else {
      Seq.empty
    }

    checkForAny
  }
}
