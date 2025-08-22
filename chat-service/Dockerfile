FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/chat-service-*.jar app.jar
EXPOSE 9005
ENTRYPOINT ["java","-jar","/app/app.jar"]

