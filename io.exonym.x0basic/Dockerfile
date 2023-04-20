FROM eclipse-temurin:11

RUN mkdir /opt/app

COPY target/x0basic.jar /opt/app
COPY target/jars/ /opt/app/jars

CMD ["java", "-jar", "/opt/app/x0basic.jar"]

#"-Xshareclasses:cacheDir=/opt/shareclasses",
