FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/task-service-*.jar app.jar
EXPOSE 9002
ENTRYPOINT ["java","-jar","/app/app.jar"]

