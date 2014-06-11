email-sender
============

Send emails using sms - scala / play 2 application


[![Build Status](https://api.travis-ci.org/yorrick/email-sender.svg?branch=master)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png)](https://coveralls.io/r/yorrick/email-sender)


Heroku JAVA_OPT for staging: 
heroku config:set java_opts="-Xms384m -Xmx384m -Xss512k -XX:+UseCompressedOops -Dnewrelic.bootstrap_classpath=true -javaagent:target/universal/stage/lib/com.newrelic.agent.java.newrelic-agent-3.6.0.jar -Dnewrelic.config.file=target/universal/stage/conf/newrelic/newrelic-staging.yml"


Heroku timezone:
heroku config:add TZ="Europe/Paris"

Enable clean compiles (https://devcenter.heroku.com/articles/scala-support#clean-builds):
heroku config:set SBT_CLEAN=true


TODO 
 - listing page
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


