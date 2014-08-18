package ems.controllers


import ems.backend.UserInfoStore.UserInfoStoreException
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{RequestHeader, Flash}

import ems.models.{PhoneNumber, User}
import ems.backend.{Mailgun, Twilio, UserInfoStore}
import ems.views.utils.Helpers._


object AccountController {
  // regex for north american phone number
  val phoneRegex = """[0-9.+]{10}""".r
  // north american phone prefix
  val phonePrefix = "+1"
}


/**
 * Handles authentication custom views
 */
class AccountController(implicit inj: Injector) extends SecureSocial[User] with Injectable {
  import AccountController._

  override implicit val env = inject [RuntimeEnvironment[User]]

  val form = Form(mapping(
    "phoneNumber" -> (text verifying pattern(phoneRegex, "10 digits", "The phone number must have 10 digits"))
  )(PhoneNumber.apply)(PhoneNumber.unapply))

  /**
   * Generates the common display response
   * @param form
   * @return
   */
  def displayResponse(form: Form[PhoneNumber])(implicit user: User, request: RequestHeader, env: RuntimeEnvironment[User]) =
    ems.views.html.auth.account(form, Twilio.apiMainNumber, Mailgun.emailSource(Twilio.apiMainNumber))

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

        UserInfoStore.findUserInfoByUserId(user.id) flatMap { userInfo =>
          if (phoneNumberToSave != userInfo.phoneNumber) {
            // update the phone number in mongo
            UserInfoStore.savePhoneNumber(user.id, phoneNumberToSave) map { userInfo =>

              // send a confirmation to the given phone number, but do not wait for the reply
              Twilio.sendConfirmationSms(phoneNumberToSave)

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
    UserInfoStore.findUserInfoByUserId(user.id) map { userInfo =>
      val phoneNumber = userInfo.phoneNumber map { _.stripPrefix(phonePrefix)}
      form.fill(PhoneNumber(phoneNumber.getOrElse("")))
    }
  }
}
