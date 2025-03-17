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

package controllers.predicates

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys}
import models.User
import models.logging.CorrelationIdMdc.withEnrichedCorrelationId
import play.api.Logger
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()()(implicit val authConnector: AuthConnector,
                                   defaultActionBuilder: DefaultActionBuilder,
                                   val cc: ControllerComponents) extends AuthorisedFunctions {

  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val executionContext: ExecutionContext = cc.executionContext

  private val unauthorized: Future[Result] = Future(Unauthorized)
  private val minimumConfidenceLevel: Int = ConfidenceLevel.L250.level

  def async(block: User[AnyContent] => Future[Result]): Action[AnyContent] = defaultActionBuilder.async { implicit request =>
    withEnrichedCorrelationId(request) { request =>
      implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      request.headers.get("mtditid").fold {
        logger.warn("[AuthorisedAction][async] - No MTDITID in the header. Returning unauthorised.")
        unauthorized
      }(
        mtdItId =>
          authorised().retrieve(affinityGroup) {
            case Some(AffinityGroup.Agent) => agentAuthentication(block, mtdItId)(request, headerCarrier)
            case _ => individualAuthentication(block, mtdItId)(request, headerCarrier)
          } recover {
            case _: NoActiveSession =>
              logger.info(s"[AuthorisedAction][async] - No active session.")
              Unauthorized
            case _: AuthorisationException =>
              logger.info(s"[AuthorisedAction][async] - User failed to authenticate")
              Unauthorized
            case e =>
              logger.error(s"[AuthorisedAction][async] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
              InternalServerError
          }
      )
    }
  }

  private[predicates] def individualAuthentication[A](block: User[A] => Future[Result], requestMtdItId: String)
                                                     (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(allEnrolments and confidenceLevel) {
      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        val optionalMtdItId: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments)
        val optionalNino: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

        (optionalMtdItId, optionalNino) match {
          case (Some(authMTDITID), Some(_)) =>
            enrolments.enrolments.collectFirst {
              case Enrolment(EnrolmentKeys.Individual, enrolmentIdentifiers, _, _)
                if enrolmentIdentifiers.exists(identifier => identifier.key == EnrolmentIdentifiers.individualId && identifier.value == requestMtdItId) =>
                block(User(requestMtdItId, None))
            } getOrElse {
              logger.warn(s"[AuthorisedAction][individualAuthentication] Non-agent with an invalid MTDITID. " +
                s"MTDITID in auth matches MTDITID in request: ${authMTDITID == requestMtdItId}")
              unauthorized
            }
          case (_, None) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no nino.")
            unauthorized
          case (None, _) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment.")
            unauthorized
        }
      case _ =>
        logger.warn("[AuthorisedAction][individualAuthentication] User has confidence level below 250.")
        unauthorized
    }
  }

  private val agentAuthLogString: String = "[AuthorisedAction][agentAuthentication]"

  private def agentAuthPredicate(mtdId: String): Predicate =
    Enrolment(EnrolmentKeys.Individual)
      .withIdentifier(EnrolmentIdentifiers.individualId, mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.agentDelegatedAuthRule)

  private def agentRecovery(): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      logger.warn(s"$agentAuthLogString - No active session.")
      unauthorized
    case _: AuthorisationException =>
      logger.warn(s"$agentAuthLogString - Agent does not have delegated authority for Client.")
      unauthorized
    case e =>
      logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      Future.successful(InternalServerError)
  }

  private def handleForValidAgent[A](block: User[A] => Future[Result],
                                     mtdItId: String,
                                     enrolments: Enrolments)
                                    (implicit request: Request[A]): Future[Result] = {
    enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
      case Some(arn) => block(User(mtdItId, Some(arn)))
      case None =>
        val logMessage = s"$agentAuthLogString - Agent with no HMRC-AS-AGENT enrolment."
        logger.warn(logMessage)
        unauthorized
    }
  }

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result], mtdItId: String)
                                             (implicit request: Request[A], hc: HeaderCarrier): Future[Result] =
    authorised(agentAuthPredicate(mtdItId))
      .retrieve(allEnrolments)(enrolments => handleForValidAgent(block, mtdItId, enrolments))
      .recoverWith(agentRecovery())

  private[predicates] def enrolmentGetIdentifierValue(checkedKey: String,
                                                      checkedIdentifier: String,
                                                      enrolments: Enrolments): Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) => enrolmentIdentifiers.collectFirst {
      case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) => identifierValue
    }
  }.flatten

}
