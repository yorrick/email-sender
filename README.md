email-sender
============

Send emails using sms - scala / play 2 application


[![Build Status](https://api.travis-ci.org/yorrick/email-sender.svg?branch=master)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png)](https://coveralls.io/r/yorrick/email-sender)

This app is deployed on heroku using a custom scala buildpack https://github.com/yorrick/heroku-buildpack-scala.

Rebuild app on heroku without push any new commit
heroku plugins:install https://github.com/heroku/heroku-repo.git
heroku repo:rebuild


TODO 
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


