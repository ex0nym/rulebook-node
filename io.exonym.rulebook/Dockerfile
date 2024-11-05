FROM tomcat:9.0.73-jdk17-temurin-focal

ENV CATALINA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED"


# COPY conf/logging.properties /usr/local/tomcat/conf/
COPY conf/tomcat-users.xml /usr/local/tomcat/conf/
COPY conf/server.xml /usr/local/tomcat/conf/
COPY conf/web.xml /usr/local/tomcat/conf/
COPY conf/context.xml /usr/local/tomcat/conf/
COPY conf/catalina.properties /usr/local/tomcat/conf/

RUN groupadd -r tomcat && useradd -r -g tomcat tomcat
RUN mkdir -p /var/www/html
RUN chown -R tomcat:tomcat /var/www/html
RUN chmod -R 750 /var/www/html

COPY target/rulebook.war /usr/local/tomcat/webapps/
