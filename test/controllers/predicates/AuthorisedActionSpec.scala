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

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import models.User
import org.scalamock.handlers.{CallHandler0, CallHandler4}
import play.api.http.{HeaderNames, Status => TestStatus}
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, ResultExtractors}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, _}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.UnitTest

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends UnitTest {

  val auth: AuthorisedAction = authorisedAction

  trait AgentTest extends ResultExtractors
    with HeaderNames
    with TestStatus
    with DefaultAwaitTimeout {

    val nino = "AA111111A"
    val mtdItId: String = "1234567890"
    val arn: String = "0987654321"

    val validHeaderCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))

    val testBlock: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

    val mockAppConfig: AppConfig = mock[AppConfig]

    def primaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth")

    def secondaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT-SUPP")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth-supp")

    def mockMultipleAgentsSwitch(bool: Boolean): CallHandler0[Boolean] =
      (mockAppConfig.emaSupportingAgentsEnabled _: () => Boolean)
        .expects()
        .returning(bool)
        .anyNumberOfTimes()

    val primaryAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    val supportingAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.SupportingAgent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    def mockAuthReturnException(exception: Exception,
                                predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.failed(exception))

    def mockAuthReturn(enrolments: Enrolments, predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.successful(enrolments))

    def testAuth: AuthorisedAction = new AuthorisedAction()(
      authConnector = mockAuthConnector,
      appConfig = mockAppConfig,
      defaultActionBuilder = defaultActionBuilder,
      cc = mockControllerComponents
    )

    lazy val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
      "mtditid" -> mtdItId
    )
  }

  ".enrolmentGetIdentifierValue" should {
    "return the value for a given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) shouldBe Some(returnValue)
      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) shouldBe Some(returnValueAgent)
    }
    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        auth.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        auth.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }

    }
  }

  ".individualAuthentication" should {

    "perform the block action" when {

      "the correct enrolment exist" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an OK status" in {
          status(result) shouldBe OK
        }

        "returns a body of the mtditid" in {
          bodyOf(result) shouldBe mtditid
        }
      }

    }

    "return a UNAUTHORIZED" when {

      "the mtditid passed into the function does not match that in the enrolments" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication(block, "someAlternateId")(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED" in {
          status(result) shouldBe UNAUTHORIZED
        }

      }

      "the correct enrolment is missing" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns a UNAUTHORIZED" in {
          status(result) shouldBe UNAUTHORIZED
        }
      }

      "the confidence level is too low" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns a UNAUTHORIZED" in {
          status(result) shouldBe UNAUTHORIZED
        }
      }

      "the user has a nino but no enrolment" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "AA123456A")), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns a UNAUTHORIZED" in {
          status(result) shouldBe UNAUTHORIZED
        }
      }
    }
  }

  ".agentAuthentication" when {
    "a valid request is made" which {
      "results in a NoActiveSession error to be returned from Auth" should {
        "return an Unauthorised response" in new AgentTest {
          object AuthException extends NoActiveSession("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }
      }

      "[EMA disabled] results in an AuthorisationException error being returned from Auth" should {
        "return an Unauthorised response" in new AgentTest {
          mockMultipleAgentsSwitch(false)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }
      }

      "[EMA disabled] results in an unexpected error being returned from Auth" should {
        "return an Unauthorised response" in new AgentTest {
          mockMultipleAgentsSwitch(false)

          object AuthException extends IndexOutOfBoundsException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result) shouldBe ""
        }
      }

      "[EMA enabled] results in an AuthorisationException error being returned from Auth" should {
        "return an Unauthorised response when secondary agent auth call returns an expected error" in new AgentTest {
          mockMultipleAgentsSwitch(true)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturnException(AuthException, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }

        "return an internal server error response when secondary agent auth call returns an unexpected error" in new AgentTest {
          mockMultipleAgentsSwitch(true)

          object AuthException extends AuthorisationException("Some reason")
          object OtherException extends IndexOutOfBoundsException("Some reason")

          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturnException(OtherException, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result) shouldBe ""
        }

        "handle appropriately when a supporting agent is properly authorised" in new AgentTest {
          mockMultipleAgentsSwitch(true)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturn(supportingAgentEnrolment, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe s"$mtdItId $arn"
        }
      }

      "results in successful authorisation for a primary agent" should {
        "return an Unauthorised response when an ARN cannot be found" in new AgentTest {
          val primaryAgentEnrolmentNoArn: Enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(EnrolmentKeys.Agent, Seq.empty, "Activated")
          ))

          mockAuthReturn(primaryAgentEnrolmentNoArn, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }

        "invoke block when the user is properly authenticated" in new AgentTest {
          mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe s"$mtdItId $arn"
        }
      }
    }
  }

  ".async" should {
    lazy val block: User[AnyContent] => Future[Result] = user =>
      Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {

      "the user is successfully verified as an agent" which {

        lazy val result = {
          mockAuthAsAgent()
          auth.async(block)(fakeRequest)
        }

        "should return an OK(200) status" in {

          status(result) shouldBe OK
          bodyOf(result) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {

        lazy val result = {
          mockAuth()
          auth.async(block)(fakeRequest)
        }

        status(result) shouldBe OK
        bodyOf(result) shouldBe "mtditid: 1234567890"
      }
    }

    "return an Unauthorised" when {
      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          auth.async(block)
        }
        status(result(fakeRequest)) shouldBe UNAUTHORIZED
      }

      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          auth.async(block)
        }

        status(result(fakeRequest)) shouldBe UNAUTHORIZED
      }

      "the mtditid is not in the header" in {
        lazy val result = auth.async(block)(FakeRequest())
        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return an InternalServerError" when {
      "the authorisation service returns an unexpected error" in {
        object SomeException extends IndexOutOfBoundsException("Some reason")

        lazy val result = {
          mockAuthReturnException(SomeException)
          auth.async(block)
        }
        status(result(fakeRequest)) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
