package ems.backend

import ems.backend.cms.PrismicService
import ems.utils.{AppInjector, TestUtils, WithTestData}
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, Handler}
import play.api.test._
import scaldi.{Injectable}


@RunWith(classOf[JUnitRunner])
class PrismicServiceSpec extends PlaySpecification with WithTestData with AppInjector with Injectable with TestUtils {
  sequential

  val fakePrismicDocumentResponse = """
    |{
    |    "page": 1,
    |    "results_per_page": 20,
    |    "results_size": 1,
    |    "total_results_size": 1,
    |    "total_pages": 1,
    |    "next_page": null,
    |    "prev_page": null,
    |    "results": [{
    |        "id": "VAT2YS4AAC8A4K7l",
    |        "type": "doc",
    |        "href": "http://email-sender.prismic.io/api/documents/search?ref=VAUnay4AADMA4REl&q=%5B%5B%3Ad+%3D+at%28document.id%2C+%22VAT2YS4AAC8A4K7l%22%29+%5D%5D",
    |        "tags": ["welcome"],
    |        "slugs": ["welcome", "welcome-you"],
    |        "linked_documents": [],
    |        "data": {
    |            "doc": {
    |                "title": {
    |                    "type": "StructuredText",
    |                    "value": [{
    |                        "type": "heading1",
    |                        "text": "Welcome",
    |                        "spans": []
    |                    }]
    |                },
    |                "content": {
    |                    "type": "StructuredText",
    |                    "value": [{
    |                        "type": "paragraph",
    |                        "text": "Welcome to smsemailbridge,  a bridge between sms and emails. ",
    |                        "spans": []
    |                    }, {
    |                        "type": "paragraph",
    |                        "text": "Send emails by sending sms, and send sms by sending emails!",
    |                        "spans": []
    |                    }]
    |                }
    |            }
    |        }
    |    }],
    |    "version": "bcb3796",
    |    "license": "This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/."
    |}
    """.stripMargin

  val fakePrismicApiResponse = """
    |{
    |    "refs": [{
    |        "id": "master",
    |        "ref": "VAUnay4AADMA4REl",
    |        "label": "Master",
    |        "isMasterRef": true
    |    }],
    |    "bookmarks": {},
    |    "types": {
    |        "author": "Author",
    |        "event": "Event",
    |        "article": "Article",
    |        "doc": "Documentation",
    |        "blog": "Blog post",
    |        "place": "Place",
    |        "product": "Product"
    |    },
    |    "tags": ["test", "welcome"],
    |    "forms": {
    |        "help": {
    |            "name": "Help articles",
    |            "method": "GET",
    |            "rel": "collection",
    |            "enctype": "application/x-www-form-urlencoded",
    |            "action": "http://email-sender.prismic.io/api/documents/search",
    |            "fields": {
    |                "ref": {
    |                    "type": "String",
    |                    "multiple": false
    |                },
    |                "q": {
    |                    "default": "[[:d = at(document.tags, [\"help\"])][:d = any(document.type, [\"article\"])]]",
    |                    "type": "String",
    |                    "multiple": true
    |                },
    |                "page": {
    |                    "type": "Integer",
    |                    "multiple": false,
    |                    "default": "1"
    |                },
    |                "pageSize": {
    |                    "type": "Integer",
    |                    "multiple": false,
    |                    "default": "20"
    |                },
    |                "orderings": {
    |                    "type": "String",
    |                    "multiple": false
    |                },
    |                "referer": {
    |                    "type": "String",
    |                    "multiple": false
    |                }
    |            }
    |        },
    |        "everything": {
    |            "method": "GET",
    |            "enctype": "application/x-www-form-urlencoded",
    |            "action": "http://email-sender.prismic.io/api/documents/search",
    |            "fields": {
    |                "ref": {
    |                    "type": "String",
    |                    "multiple": false
    |                },
    |                "q": {
    |                    "type": "String",
    |                    "multiple": true
    |                },
    |                "page": {
    |                    "type": "Integer",
    |                    "multiple": false,
    |                    "default": "1"
    |                },
    |                "pageSize": {
    |                    "type": "Integer",
    |                    "multiple": false,
    |                    "default": "20"
    |                },
    |                "orderings": {
    |                    "type": "String",
    |                    "multiple": false
    |                },
    |                "referer": {
    |                    "type": "String",
    |                    "multiple": false
    |                }
    |            }
    |        }
    |    },
    |    "oauth_initiate": "https://email-sender.prismic.io/auth",
    |    "oauth_token": "https://email-sender.prismic.io/auth/token",
    |    "version": "bcb3796",
    |    "license": "This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/."
    |}
    """.stripMargin

  val routes: PartialFunction[(String, String), Handler] = {
    case ("GET", path: String) =>
      if (path == "/api") {
        Action { Ok(fakePrismicApiResponse).withHeaders("Content-Type" -> "application/json; charset=utf-8") }
      } else {
        Action { Ok(fakePrismicDocumentResponse).withHeaders("Content-Type" -> "text/plain; charset=utf-8") }
      }
  }

  override val app = FakeApplication(withRoutes = routes, withoutPlugins = Seq(mongoPluginClass, redisPluginClass))

  "Prismic service" should {

    "Fetch main page doc" in new WithServer(app = app) {
      implicit val i = appInjector
      val service = inject[PrismicService]
      await(service.getMainPageDocument) must beSome.which(_.id == "VAT2YS4AAC8A4K7l")
    }

  }
}
