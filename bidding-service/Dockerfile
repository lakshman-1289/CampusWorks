FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/bidding-service-*.jar app.jar
EXPOSE 9004
ENTRYPOINT ["java","-jar","/app/app.jar"]

