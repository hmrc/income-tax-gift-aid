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

package support.stubs

import config.AppConfig

class AppConfigStub {

  def config(environment: String = "test"): AppConfig = new AppConfig() {
    private val wireMockPort = 11111
    private val timeToLiveValue = 28

    override val authBaseUrl: String = "/auth"

    override val auditingEnabled: Boolean = true
    override val graphiteHost: String = "/graphite"
    override val authorisationToken = ""
    override val authorisationTokenKey = ""
    override val desBaseUrl: String = s"http://localhost:$wireMockPort"
    override val desEnvironment: String = environment

    override lazy val ifAuthorisationToken: String = ""

    override lazy val ifBaseUrl: String = s"http://localhost:$wireMockPort"
    override lazy val ifEnvironment: String = environment

    override val personalFrontendBaseUrl: String = "http://localhost:9308"

    override val sectionCompletedQuestionEnabled: Boolean = true

    override def authorisationTokenFor(apiVersion: String): String = ifAuthorisationToken + s".$apiVersion"

    override def timeToLive: Long = timeToLiveValue

    override def replaceIndexes: Boolean = false
  }
}