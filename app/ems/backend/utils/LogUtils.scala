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
  def logResult[T](msg: String, logger: LoggerLike = Logger, extractor: T => String = {value: T => value.toString}): PartialFunction[Try[T], Unit] = {
    case Success(result) =>
      val valueToLog = Try {
        extractor(result)
      }.getOrElse("COULD NOT EXTRACT VALUE TO LOG")

      logger.debug(s"$msg: SUCCESS $valueToLog")
    case Failure(e) =>
      logger.debug(s"$msg: ERROR $e")
  }

}
