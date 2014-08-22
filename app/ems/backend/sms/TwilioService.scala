package ems.backend.sms

import scala.concurrent.Future


trait TwilioService {
  def sendConfirmationSms(to: String): Future[String]
  def sendSms(to: String, content: String): Future[String]

  def apiMainNumber: String
}
