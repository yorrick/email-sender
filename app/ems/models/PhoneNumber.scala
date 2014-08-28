package ems.models


/**
 * Represents a north american phone number
 */

/**
 *
 * @param value The full prefixed value, with no spaces
 */
class PhoneNumber(val value: String) {

  /**
   * The full formatted value, like +1 514 123 4567
   */
  val formattedPrefixValue =
    s"${value.slice(0, 2)} ${value.slice(2, 5)} ${value.slice(5, 8)} ${value.slice(8, 12)}"

  /**
   * The formatted value with no prefix, like 514 123 4567
   */
  val formattedNoPrefixValue = formattedPrefixValue.drop(3)

}


object PhoneNumber {

  /**
   * North american phone prefix
   */
  val prefix = "+1"

  /**
   * Regex for north american phone number
   */
  val noPrefixRegex = """^([0-9][\s-]*){10}$""".r

  /**
   * Regex for prefixed north american phone number with no spaces
   */
  val prefixRegex = """^\+1([0-9]){10}$""".r

  /**
   * Builds a PhoneNumber object from a no prefix value
   * @param rawNumber
   * @return
   */
  def fromNoPrefixValue(rawNumber: String) = {
    if (isNoPrefixValid(rawNumber))
      new PhoneNumber(s"$prefix${removeSpaces(rawNumber)}")
    else
      throw new IllegalArgumentException(s"$rawNumber is not a valid phone number")
  }

  /**
   * Builds a PhoneNumber object with no checks at all
   * @param value
   * @return
   */
  def fromCheckedValue(value: String) = {
    if (isPrefixValid(value))
      new PhoneNumber(value)
    else
      throw new IllegalArgumentException(s"$value is not a valid phone number")
  }

  def removeSpaces(string: String) = string.replaceAll("\\s", "").replaceAll("-", "")

  def isNoPrefixValid(string: String) = noPrefixRegex.unapplySeq(string).map(_ => true).getOrElse(false)

  def isPrefixValid(string: String) = prefixRegex.unapplySeq(string).map(_ => true).getOrElse(false)
}
