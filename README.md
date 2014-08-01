email-sender
============

Sms to email bridge application - developped with [scala](http://www.scala-lang.org/) and [play2](http://www.playframework.com/).


[![Build Status](https://travis-ci.org/yorrick/email-sender.svg?branch=user-sms-link)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png?branch=user-sms-link)](https://coveralls.io/r/yorrick/email-sender?branch=user-sms-link)


This app is based on a fully async stack
 * [play asynchronous](http://www.playframework.com/documentation/2.3.x/ScalaAsync)
 * async driver for redis [rediscala](https://github.com/etaty/rediscala) and [play2-rediscala](https://github.com/yorrick/play2-rediscala)
 * async driver for mongodb [reactivemongo](http://reactivemongo.org/)
 * [akka](http://akka.io/)

This application is developped for learning purposes only. It demonstrates how to
 * set up continuous integration with [travis](https://travis-ci.org/)
 * set up a local development environment with [docker](http://www.docker.com/)
 * set up a staging and production environment
 * deploy the app on [heroku](https://www.heroku.com/) using [a custom scala buildpack](https://github.com/yorrick/heroku-buildpack-scala)
 * use [newrelic](http://newrelic.com/) and [papertrail](https://papertrailapp.com/) for monitoring and logging

Sms and email managenement are done with [twilio](https://www.twilio.com/) and [mailgun](https://mailgun.com/).

Project documentation is [here](https://github.com/yorrick/email-sender/wiki).


