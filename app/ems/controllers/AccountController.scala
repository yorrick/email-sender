package ems.controllers


import ems.backend.email.MailgunService
import ems.backend.persistence.{UserInfoStoreException, UserInfoStore}
import ems.backend.sms.TwilioService
import play.api.mvc.{ RequestHeader}
import scaldi.{Injectable, Injector}
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.Logger
import ems.models.{PhoneNumber, User}


/**
 * Handles authentication custom views
 */
class AccountController(implicit inj: Injector) extends SecureSocial[User] with Injectable {
  // regex for north american phone number
  val phoneRegex = """[0-9.+]{10}""".r
  // north american phone prefix
  val phonePrefix = "+1"

  override implicit val env = inject [RuntimeEnvironment[User]]
  implicit val executionContext = inject[ExecutionContext]
  val mailgun = inject[MailgunService]
  val userInfoStore = inject[UserInfoStore]
  val twilioService = inject[TwilioService]

  val form = Form(mapping(
    "phoneNumber" -> (text verifying pattern(phoneRegex, "10 digits", "The phone number must have 10 digits"))
  )(PhoneNumber.apply)(PhoneNumber.unapply))

  /**
   * Generates the common display response
   * @param form
   * @return
   */
  def displayResponse(form: Form[PhoneNumber])(implicit user: User, request: RequestHeader, env: RuntimeEnvironment[User]) =
    ems.views.html.auth.account(form, twilioService.apiMainNumber, mailgun.emailSource(twilioService.apiMainNumber))

  /**
   * Generates the common redirect response
   * @param message
   * @param user
   * @param request
   * @param env
   * @return
   */
  def redirectResponse(message: String)(implicit user: User, request: RequestHeader, env: RuntimeEnvironment[User]) =
    Redirect(ems.controllers.routes.AccountController.account).flashing("success" -> message)

  /**
   * Account view
   * @return
   */
  def account = SecuredAction.async { implicit request =>
    implicit val user = request.user

    userForm map { form =>
      Ok(displayResponse(form))
    }
  }

  def accountUpdate = SecuredAction.async { implicit request =>
    implicit val user = request.user

    form.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(displayResponse(formWithErrors))),
      phoneNumber => {
        val phoneNumberToSave = s"$phonePrefix${phoneNumber.value}"

        userInfoStore.findUserInfoByUserId(user.id) flatMap { userInfo =>
          Logger.debug(s"Existing user info $userInfo, phoneNumberToSave $phoneNumberToSave")

          if (phoneNumberToSave != userInfo.phoneNumber.getOrElse("")) {
            // update the phone number in mongo
            userInfoStore.savePhoneNumber(user.id, phoneNumberToSave) map { userInfo =>

              // send a confirmation to the given phone number, but do not wait for the reply
              twilioService.sendConfirmationSms(phoneNumberToSave)

              redirectResponse("Phone number saved!")
            } recover {
              case UserInfoStoreException(message) =>
                BadRequest(displayResponse(form.fill(phoneNumber).withGlobalError(message)))
            }

          } else {
              Future {redirectResponse("Phone has not changed") }
          }

        }

      }
    )
  }

  def userForm(implicit user: User): Future[Form[PhoneNumber]] = {
    userInfoStore.findUserInfoByUserId(user.id) map { userInfo =>
      val phoneNumber = userInfo.phoneNumber map { _.stripPrefix(phonePrefix)}
      form.fill(PhoneNumber(phoneNumber.getOrElse("")))
    }
  }
}
