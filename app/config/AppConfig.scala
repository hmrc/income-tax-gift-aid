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

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.duration.Duration

trait AppConfig {

  val authBaseUrl: String

  val auditingEnabled: Boolean
  val graphiteHost: String
  val desBaseUrl: String

  val desEnvironment: String
  val authorisationToken: String
  val ifAuthorisationToken: String
  val authorisationTokenKey: String
  val ifBaseUrl: String
  val ifEnvironment: String

  val personalFrontendBaseUrl: String

  def authorisationTokenFor(apiVersion: String): String

  def timeToLive: Long
  def replaceIndexes: Boolean

  val sectionCompletedQuestionEnabled: Boolean
}

class BackendAppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {
  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")
  val desBaseUrl: String = servicesConfig.baseUrl("des")

  val desEnvironment: String = config.get[String]("microservice.services.des.environment")
  val authorisationToken: String = config.get[String]("microservice.services.des.authorisation-token")

  lazy val ifBaseUrl: String = servicesConfig.baseUrl(serviceName = "integration-framework")
  lazy val ifAuthorisationToken: String = config.get[String]("microservice.services.integration-framework.authorisation-token")
  lazy val authorisationTokenKey: String = "microservice.services.integration-framework.authorisation-token"
  lazy val ifEnvironment: String = servicesConfig.getString(key = "microservice.services.integration-framework.environment")

  val personalFrontendBaseUrl: String =
    s"${config.get[String]("microservice.services.personal-income-tax-submission-frontend.url")}/update-and-submit-income-tax-return/personal-income"

  def authorisationTokenFor(api: String): String = config.get[String](s"microservice.services.integration-framework.authorisation-token.$api")

  def timeToLive: Long = Duration(config.get[String]("mongodb.timeToLive")).toDays.toInt
  def replaceIndexes: Boolean = config.get[Boolean]("mongodb.replaceIndexes")

  lazy val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.sectionCompletedQuestionEnabled")
}
