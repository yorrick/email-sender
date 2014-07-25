package utils.securesocial

import securesocial.core.BasicProfile
import securesocial.core.services.UserService
import org.specs2.execute.{Result, AsResult}
import org.specs2.mock.Mockito

import play.api.test.{WithApplication, FakeApplication}
import play.api.mvc.Cookie


abstract class WithLoggedUser[Usr](override val app: FakeApplication = FakeApplication(),
                              val identity: Option[BasicProfile] = None) extends WithApplication(app) with Mockito {

  // vals must be lazy due to arround implementation
  // TODO implement default basic profile if none is given
  lazy val basicProfile = identity.get
  lazy val mockUserService = mock[UserService[Usr]]

  // TODO create a user
  //  - in AuthenticatorStore using save(authenticator: A, timeoutInSeconds: Int): Future[A]
  //  - in UserService using def save(profile: BasicProfile, mode: SaveMode): Future[U]

//  def cookie = Authenticator.create(basicProfile) match {
//    case Right(authenticator) => authenticator.toCookie
//    case _ => throw new IllegalArgumentException("Your FakeApplication _must_ configure a working AuthenticatorStore")
//  }
//
//  override def around[T: AsResult](t: => T): Result = super.around {
//    mockUserService.find(basicProfile.identityId) returns Some(basicProfile)
//    UserService.setService(mockUserService)
//    t
//  }
}

object WithLoggedUser{
  val excludedPlugins = List( "securesocial.core.DefaultAuthenticatorStore" )
  val includedPlugins = List( "securesocial.testkit.FakeAuthenticatorStore" )
  def minimalApp = FakeApplication(withoutPlugins = excludedPlugins, additionalPlugins = includedPlugins)
}


