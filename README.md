email-sender
============

Send emails using sms - scala / play 2 application


[![Build Status](https://api.travis-ci.org/yorrick/email-sender.svg?branch=master)](https://travis-ci.org/yorrick/email-sender)
[![Coverage Status](https://coveralls.io/repos/yorrick/email-sender/badge.png)](https://coveralls.io/r/yorrick/email-sender)


To run sbt console, do not use heroku sbt script:
java -Xmx1024M -Dfile.encoding=UTF8 -Duser.home=/app/.sbt_home/  -Dsbt.log.noformat=true -Divy.fault.ivy.user.dir="/app/.sbt_home/.ivy2" -jar .sbt_home/bin/sbt-launch.jar console


TODO 
 - See why we get an error about sbt configuration (http://www.scala-sbt.org/0.13.5/docs/Launcher/GettingStarted.html#overview) when trying to run sbt on heroku
 - listing page
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


