package ems.controllers.utils

import play.api.mvc.{Results, Result, RequestHeader, Filter}
import scaldi.{Injectable, Injector}

import scala.concurrent.Future


/**
 * A filter that redirects all non secured requests to their secured counterpart,
 * if http header is present (eg request has been served by nginx)
 */
class HttpsOnlyFilter(implicit inj: Injector) extends Filter with Injectable with Results {

  val enabled = inject[Boolean] (identified by "ems.controllers.utils.HttpsOnlyFilter.enabled")
  val httpHeader = inject[String] (identified by "ems.controllers.utils.HttpsOnlyFilter.httpHeader")

  def apply(action: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    def doNothing = action(requestHeader)

    if (enabled) {
      requestHeader.headers.get(httpHeader) match {
        case Some(header) => {
          if ("https" == header) {
            doNothing
          } else {
            // redirect to secured url
            val newUrl = "https://" + requestHeader.host + requestHeader.uri
            Future.successful(MovedPermanently(newUrl))
          }
        }
        case None => doNothing
      }
    } else {
      doNothing
    }
  }

}
