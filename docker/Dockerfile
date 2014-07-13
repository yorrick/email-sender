FROM quintenk/supervisor
MAINTAINER Yorrick Jansen

# Import MongoDB public GPG key AND create a MongoDB list file
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list

RUN apt-get update

RUN apt-get install -y -q mongodb-org
RUN mkdir -p /data/db
EXPOSE 27017

RUN apt-get -y install redis-server
EXPOSE 6379

# add supervisor scripts
ADD mongo.conf /etc/supervisor/conf.d/
ADD redis.conf /etc/supervisor/conf.d/

# supervisor will then run all other processes
ADD supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord"]
