FROM quintenk/supervisor
MAINTAINER Yorrick Jansen

# Import MongoDB public GPG key AND create a MongoDB list file
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list

RUN apt-get update

RUN apt-get install -y -q mongodb-org
RUN mkdir -p /data/db
EXPOSE 27017

# redis version on heroku is 2.8.11
# for now we do not set any version for dev redis (otherwise we would have do build ot from source)
# 2 instances are started, one for dev and one to run test locally
RUN apt-get -y install redis-server

ADD etc/redis-dev.conf /etc/redis/
ADD etc/redis-test.conf /etc/redis/

RUN mkdir /var/lib/redis-dev
RUN mkdir /var/lib/redis-test

EXPOSE 6379
EXPOSE 6380

# add supervisor scripts
ADD supervisor/mongo.conf /etc/supervisor/conf.d/
ADD supervisor/redis-dev.conf /etc/supervisor/conf.d/
ADD supervisor/redis-test.conf /etc/supervisor/conf.d/

# supervisor will then run all other processes
ADD supervisor/supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord"]
