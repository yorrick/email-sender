package ems.backend.utils

import play.api.{LoggerLike, Logger}

import scala.util.{Failure, Success, Try}

/**
 * Contains utility functions to log futures results
 */
trait LogUtils {

  /**
   * Allows for future result logging
   * @param msg
   * @return
   */
  def logResult(msg: String, logger: LoggerLike = Logger): PartialFunction[Try[_], Unit] = {
    case Success(result) => logger.debug(s"$msg: SUCCESS $result")
    case Failure(e) => logger.debug(s"$msg: ERROR $e")
  }

}
