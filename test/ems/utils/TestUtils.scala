package ems.utils

import ems.utils.securesocial.AuthenticationUtils


/**
 * Contains mocks that are used in the tests
 */
trait TestUtils extends MockUtils with AppUtils with AuthenticationUtils { self: WithTestData =>

}
