FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/notification-service-*.jar app.jar
EXPOSE 9007
ENTRYPOINT ["java","-jar","/app/app.jar"]

