email-sender
============

Rebuild app on heroku without push any new commit
-------------------------------------------------

```
heroku plugins:install https://github.com/heroku/heroku-repo.git
heroku repo:rebuild
```


Run mongodb in local
--------------------

```
mongod --config /Volumes/data/mongodb-databases/mongod.conf
```


Test sms reception
------------------

```
curl --data "From=11111111&To=222222222&Body=hello you" http://localhost:9000/sms/
```


Execute an command on heroku for a specific app
-----------------------------------------------

```
heroku addons:add papertrail --app yorrick-email-sender
```


TODO
----

 - send email with mailgun
 - use mongodb to store sms
 - use websockets to update message list


Heroku configuration
--------------------
You can update heroku configuration by running 

```
heroku config --app yorrick-email-sender-staging > docs/heroku-config-staging && heroku config --app yorrick-email-sender > docs/heroku-config-prod
```

 - [Staging](https://raw.githubusercontent.com/yorrick/email-sender/documentation/docs/heroku-config-staging)
 - [Production](https://raw.githubusercontent.com/yorrick/email-sender/documentation/docs/heroku-config-prod)