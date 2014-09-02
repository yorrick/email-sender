package ems.controllers


import ems.backend.email.MailgunService
import ems.backend.persistence.{UserInfoStoreException, UserInfoStore}
import ems.backend.sms.TwilioService
import ems.controllers.utils.{Context, ContextRequest, ContextAction}
import ems.views.utils.FormInfo
import play.api.data.validation.{ValidationError, Invalid, Valid, Constraint}
import play.api.mvc._
import scaldi.{Injectable, Injector}
import securesocial.core.java.SecuredAction
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.data.Form
import play.api.data.Forms._
import play.api.Logger
import ems.models.{PhoneNumber, User}
import play.api.libs.json._



trait ControllerUtils extends SecureSocial[User] {

  implicit val injector: Injector

  /**
   * Type for the callback controller function
   */
  type controllerFunction = User => Context => Request[_] => Future[Result]

  /**
   * Builds an action using a simple function that takes user, context and request as parameters
   * @param tags
   * @param block
   * @return
   */
  def securedContextAction(tags: String*)(block: controllerFunction): Action[AnyContent] =
    ContextAction("footer") {
      SecuredAction.async { r: SecuredRequest[_] => r match {
        case SecuredRequest(user, authenticator, ContextRequest(ctx, originalRequest)) =>
          block(user)(ctx)(originalRequest)
      }
      }
    }


}


/**
 * Handles authentication custom views
 */
class AccountController(implicit val injector: Injector) extends ControllerUtils with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
  implicit val executionContext = inject[ExecutionContext]
  val mailgun = inject[MailgunService]
  val userInfoStore = inject[UserInfoStore]
  val twilioService = inject[TwilioService]

  val emptyPhoneMessage = "Enter your phone number"
  val wrongPhoneMessage = s"Enter valid phone number, like ${PhoneNumber.fromNoPrefixValue("514 123 4567").formattedNoPrefixValue}"

  val frontendValidationParams = Json.obj(
    "rules" -> Json.obj(
      "phoneNumber" -> Json.obj(
        "required" -> true,
        "phoneUS" -> true)),
    "messages" -> Json.obj(
      "phoneNumber" -> Json.obj(
        "required" -> emptyPhoneMessage,
        "phoneUS" -> wrongPhoneMessage))
  )

  def formInfo(form: Form[PhoneNumber]) = {
    FormInfo(form, ems.controllers.routes.AccountController.accountUpdate(), frontendValidationParams)
  }

  val form = Form(
    mapping("phoneNumber" -> (nonEmptyText verifying phoneNumberConstraint))
    (rawNumber => PhoneNumber.fromNoPrefixValue(rawNumber))
    (numberObject => Some(numberObject.formattedNoPrefixValue))
  )

  def phoneNumberConstraint = Constraint[String] { value: String =>
    if (value.isEmpty) {
      Invalid(emptyPhoneMessage)
    } else if (PhoneNumber.isNoPrefixValid(value)) {
      Valid
    } else {
      Invalid(ValidationError(wrongPhoneMessage, PhoneNumber.noPrefixRegex))
    }
  }

  /**
   * Generates the common display response
   * @param formInfo
   * @return
   */
  def displayResponse(formInfo: FormInfo[PhoneNumber])(implicit user: User, flash: Flash, env: RuntimeEnvironment[User], ctx: Context) = {
    val twilioNumber = PhoneNumber.fromCheckedValue(twilioService.apiMainNumber)
    ems.views.html.auth.account(formInfo, twilioNumber, mailgun.emailSource(twilioService.apiMainNumber))
  }

  /**
   * Generates the common redirect response
   * @param message
   * @param user
   * @param env
   * @return
   */
  def redirectResponse(message: String)(implicit user: User, flash: Flash, env: RuntimeEnvironment[User]) =
    Redirect(ems.controllers.routes.AccountController.account).flashing("success" -> message)

  /**
   * Account view
   * @return
   */
  def account = securedContextAction("footer") { implicit user => implicit ctx => implicit request =>
    userInfoStore.findUserInfoByUserId(user.id) map { userInfo =>
      form.fill(userInfo.phoneNumber map { number => PhoneNumber.fromCheckedValue(number)} getOrElse (PhoneNumber.empty))
    } map { form =>
      Ok(displayResponse(formInfo(form)))
    }
  }

  def accountUpdate = securedContextAction("footer") { implicit user => implicit ctx => implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(displayResponse(formInfo(formWithErrors))))
      },
      handleFormValidated
    )
  }

  private def handleFormValidated(phoneNumber: PhoneNumber)(implicit user: User, flash: Flash, ctx: Context) = {
    userInfoStore.findUserInfoByUserId(user.id) flatMap { userInfo =>
      Logger.debug(s"Existing user info $userInfo, phoneNumberToSave ${phoneNumber.value}")

      if (phoneNumber.value != userInfo.phoneNumber.getOrElse("")) {
        // update the phone number in mongo
        userInfoStore.savePhoneNumber(user.id, phoneNumber.value) map { userInfo =>
          // send a confirmation to the given phone number, but do not wait for the reply
          twilioService.sendConfirmationSms(phoneNumber.value)
          redirectResponse("Phone number saved!")
        } recover recoverFromError(phoneNumber)

      } else {
          Future {redirectResponse("Phone has not changed") }
      }
    }
  }

  private def recoverFromError(phoneNumber: PhoneNumber)(implicit user: User, flash: Flash, ctx: Context): PartialFunction[Throwable, Result] = {
    case UserInfoStoreException(message) =>
      val filledForm = form.fill(phoneNumber).withGlobalError(message)
      BadRequest(displayResponse(formInfo(filledForm)))
    case t @ _ =>
      val filledForm = form.fill(phoneNumber).withGlobalError("An unexpected error occured, could not save phone number")
      BadRequest(displayResponse(formInfo(filledForm)))
  }

}
