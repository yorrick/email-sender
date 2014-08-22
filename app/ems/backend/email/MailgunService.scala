package ems.backend.email

import scala.concurrent.Future


trait MailgunService {
  def sendEmail(from: String, to: String, content: String): Future[String]
  def emailSource(from: String): String
}
