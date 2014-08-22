package ems.backend.auth

import akka.util.ByteString
import ems.models.User
import org.joda.time.DateTime
import play.api.libs.json.{Format, JsResult, Json}
import redis.ByteStringFormatter
import securesocial.core.authenticator.{AuthenticatorStore, CookieAuthenticator}

/**
 * ByteStringFormatter that allows to serialize and deserialize CookieAuthenticator[User] objects
 * @param store
 */
class CookieAuthenticatorFormatter(val store: AuthenticatorStore[CookieAuthenticator[User]])
    extends ByteStringFormatter[CookieAuthenticator[User]] {

  val encoding = "UTF-8"

  /**
   * Work around to avoid serializing parameterized type with macros
   * See http://stackoverflow.com/questions/20134595/automatic-serialization-deserialization-of-generic-case-classes-to-from-json-in
   * if someday we want to make it work
   */
  case class SerializableCookieAuthenticator(id: String, user: User, expirationDate: DateTime,
                                             lastUsed: DateTime, creationDate: DateTime)

  object SerializableCookieAuthenticator {
    def fromCookieAuthenticator(cookieAuth: CookieAuthenticator[User]) = SerializableCookieAuthenticator(
      cookieAuth.id, cookieAuth.user, cookieAuth.expirationDate, cookieAuth.lastUsed, cookieAuth.creationDate
    )

    def toCookieAuthenticator(scAuth: SerializableCookieAuthenticator) = new CookieAuthenticator(
      scAuth.id, scAuth.user, scAuth.expirationDate, scAuth.lastUsed, scAuth.creationDate, store
    )
  }
  implicit val format: Format[SerializableCookieAuthenticator] = Json.format[SerializableCookieAuthenticator]

  def serialize(cAuth: CookieAuthenticator[User]): ByteString = {
    val serializableCookieAuthenticator = SerializableCookieAuthenticator.fromCookieAuthenticator(cAuth)
    val stringAuth = Json.stringify(format.writes(serializableCookieAuthenticator))
    ByteString(stringAuth.getBytes(encoding))
  }

  def deserialize(bs: ByteString): CookieAuthenticator[User] = {
    val byteArray: Array[Byte] = bs.toArray
    val stringAuth = new String(byteArray, encoding)

    val jsResult: JsResult[CookieAuthenticator[User]] = Json.parse(stringAuth).validate[SerializableCookieAuthenticator] map {
      SerializableCookieAuthenticator.toCookieAuthenticator(_)
    }

    jsResult.get
  }
}
