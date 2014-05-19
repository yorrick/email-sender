email-sender
============

Send emails using sms - scala / play 2 application


Heroku JAVA_OPT: 
heroku config:set java_opts="-Xms384m -Xmx384m -Xss512k -XX:+UseCompressedOops -Dnewrelic.bootstrap_classpath=true -javaagent:target/universal/stage/lib/com.newrelic.agent.java.newrelic-agent-3.6.0.jar"


TODO 
 - listing page
 - monitoring + logging (see heroku sink)
 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list
