email-sender
============

Send emails using sms - scala / play 2 application

                 
[![Build Status](https://travis-ci.org/yorrick/email-sender.svg?branch=master)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png)](https://coveralls.io/r/yorrick/email-sender)

This app is deployed on heroku using a custom scala buildpack https://github.com/yorrick/heroku-buildpack-scala.

Rebuild app on heroku without push any new commit
heroku plugins:install https://github.com/heroku/heroku-repo.git
heroku repo:rebuild


Run mongodb in local
mongod --config /Volumes/data/mongodb-databases/mongod.conf


Test sms reception
curl --data "From=11111111&To=222222222&Body=hello you" http://localhost:9000/sms/


TODO
 - use papertrail for logging
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


