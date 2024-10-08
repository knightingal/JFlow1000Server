FROM eclipse-temurin:21
RUN mkdir /opt/app
COPY build/libs/JFlow1000Server-0.0.1-SNAPSHOT.jar /opt/app
COPY aapt2 /opt/app
CMD ["java", "-jar", "/opt/app/JFlow1000Server-0.0.1-SNAPSHOT.jar", "--spring.config.location=classpath:/application.properties, file:/opt/app/config/application.properties"]