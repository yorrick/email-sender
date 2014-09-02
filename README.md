email-sender
============

Sms <-> Email bridge application - developped with [scala](http://www.scala-lang.org/) and [play2](http://www.playframework.com/).


[![Build Status](https://travis-ci.org/yorrick/email-sender.svg?branch=27-doc)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png?branch=27-doc)](https://coveralls.io/r/yorrick/email-sender?branch=27-doc)


#### This app is based on a fully async stack
 * [play asynchronous](http://www.playframework.com/documentation/2.3.x/ScalaAsync)
 * async driver for redis [rediscala](https://github.com/etaty/rediscala) and [play2-rediscala](https://github.com/yorrick/play2-rediscala)
 * async driver for mongodb [reactivemongo](http://reactivemongo.org/)
 * [akka](http://akka.io/)

#### This application is developped for learning purposes only. It demonstrates how to
 * set up continuous integration with [travis](https://travis-ci.org/)
 * set up a local development environment with [docker](http://www.docker.com/)
 * set up a staging and production environment
 * use [google Oauth2 SSO](https://developers.google.com/accounts/docs/OAuth2) and [facebook Oauth2 SSO](https://developers.facebook.com/docs/facebook-login/v2.1) to allow users to login
 * deploy the app on [heroku](https://www.heroku.com/) using [a custom scala buildpack](https://github.com/yorrick/heroku-buildpack-scala)
 * use [newrelic](http://newrelic.com/) and [papertrail](https://papertrailapp.com/) for monitoring and logging
 * use [scaldi](http://scaldi.org/) dependency injection framework
 * unit test [actors](http://doc.akka.io/docs/akka/2.3.3/scala/testing.html), [controllers](https://www.playframework.com/documentation/2.2.0/ScalaTest) and so on, using [mockito and specs2](https://code.google.com/p/specs/wiki/UsingMockito)
 * [setup SSL](https://github.com/yorrick/email-sender/wiki/SSL-setup) for a play application on heroku
 * integrate [bootstrap](http://getbootstrap.com/) and [jQuery validation](http://jqueryvalidation.org/) with play forms
 * use [webjars](http://www.webjars.org/) with CDN and versioning
 * serve internal static resources using a CDN ([cloudfront](http://aws.amazon.com/cloudfront/)), with asset versioning (using [sbt-digest](https://github.com/sbt/sbt-digest))
 * how to integrate [prismic CMS](https://prismic.io/), to display CMS content anywhere

#### Architecture evolution
 * to be able to scale out, web UI, api endpoints and forwarding jobs should be split into different nodes
 * web, forwarding and api nodes would thus be able to scale out independently
 * api endpoints would send jobs to forwarding workers (using AMQP shared queues - rabbitMQ, since akka remote actors cannot be setup
   on an infrastructure with changing IP addresses such as Heroku's)
 * forwarding workers would still send notifications to web nodes, using redis pubsub (as it is now), or an AMQP topic

Sms and email managenement are done with [twilio](https://www.twilio.com/) and [mailgun](https://mailgun.com/).

Project documentation is [here](https://github.com/yorrick/email-sender/wiki).

