email-sender
============

Send emails using sms - scala / play 2 application


Heroku JAVA_OPT for staging: 
heroku config:set java_opts="-Xms384m -Xmx384m -Xss512k -XX:+UseCompressedOops -Dnewrelic.bootstrap_classpath=true -javaagent:target/universal/stage/lib/com.newrelic.agent.java.newrelic-agent-3.6.0.jar -Dnewrelic.config.file=target/universal/stage/conf/newrelic/newrelic-staging.yml"


Heroku timezone:
heroku config:add TZ="Europe/Paris"


TODO 
 - listing page
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


