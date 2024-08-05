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

package models.taskList

import models.tasklist.TaskTitle
import models.tasklist.TaskTitle._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}

class TaskTitleSpec extends AnyFreeSpec with Matchers {

  "TaskTitle" - {

    "must contain the correct values" in {
      TaskTitle.values mustEqual Seq[TaskTitle](
        DonationsUsingGiftAid,
        GiftsOfLandOrProperty,
        GiftsOfShares,
        GiftsToOverseas
      )
    }

    "must parse each element to jsValue successfully" in {
      val underTest: Seq[JsValue] = TaskTitle.values.map(x => Json.toJson(x))
      underTest.isInstanceOf[Seq[JsValue]] mustBe true
    }
  }

  "DonationsUsingGiftAid" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(DonationsUsingGiftAid)

      underTest.toString() mustBe s"\"${DonationsUsingGiftAid.toString}\""
      underTest.validate[TaskTitle] mustBe JsSuccess(DonationsUsingGiftAid, JsPath())
    }
  }

  "GiftsOfShares" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(GiftsOfShares)

      underTest.toString() mustBe s"\"${GiftsOfShares.toString}\""
      underTest.validate[TaskTitle] mustBe JsSuccess(GiftsOfShares, JsPath())
    }
  }

  "GiftsOfLandOrProperty" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(GiftsOfLandOrProperty)

      underTest.toString() mustBe s"\"${GiftsOfLandOrProperty.toString}\""
      underTest.validate[TaskTitle] mustBe JsSuccess(GiftsOfLandOrProperty, JsPath())
    }
  }

  "GiftsToOverseas" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(GiftsToOverseas)

      underTest.toString() mustBe s"\"${GiftsToOverseas.toString}\""
      underTest.validate[TaskTitle] mustBe JsSuccess(GiftsToOverseas, JsPath())
    }
  }

}
