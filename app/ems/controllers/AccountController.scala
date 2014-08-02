package ems.controllers


import scala.concurrent.Future

import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.concurrent.Execution.Implicits._

import ems.models.{PhoneNumber, User}
import ems.backend.UserInfoStore
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
class AccountController(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {
  import AccountController._

  val form = Form(mapping(
    "phoneNumber" -> (text verifying pattern(phoneRegex, "10 digits", "The phone number must have 10 digits"))
  )(PhoneNumber.apply)(PhoneNumber.unapply))

  /**
   * Account view
   * @return
   */
  def account = SecuredAction.async { implicit request =>
    implicit val user = request.user

    userForm map { form =>
      Ok(ems.views.html.auth.account(form))
    }
  }

  def accountUpdate = SecuredAction.async { implicit request =>
    implicit val user = request.user

    form.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(ems.views.html.auth.account(formWithErrors))),
      phoneNumber => {
        val phoneNumberToSave = s"$phonePrefix${phoneNumber.value}"

        // update the phone number in mongo
        UserInfoStore.savePhoneNumber(user.id, phoneNumberToSave) map { userInfo =>
          Redirect(ems.controllers.routes.AccountController.account).flashing("success" -> "Phone number saved!")
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
