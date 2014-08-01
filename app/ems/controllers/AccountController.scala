package ems.controllers


import scala.concurrent.Future

import securesocial.core.{RuntimeEnvironment, SecureSocial}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

import ems.models.{UserInfo, PhoneNumber, User}
import ems.backend.UserInfoStore



/**
 * Handles authentication custom views
 */
class AccountController(override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] {

  val form = Form(mapping("phoneNumber" -> text(minLength = 10))(PhoneNumber.apply)(PhoneNumber.unapply))

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
        // update the phone number in mongo
        UserInfoStore.savePhoneNumber(user.id, phoneNumber.value) map { userInfo =>
          Redirect(ems.controllers.routes.AccountController.account).flashing("success" -> "Phone number saved!")
        }
      }
    )
  }

  def userForm(implicit user: User): Future[Form[PhoneNumber]] = {
    UserInfoStore.findUserInfoByUserId(user.id) map { userInfo =>
      form.fill(PhoneNumber(userInfo.phoneNumber.getOrElse("")))
    }
  }
}
