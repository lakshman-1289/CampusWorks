FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/review-service-*.jar app.jar
EXPOSE 9008
ENTRYPOINT ["java","-jar","/app/app.jar"]

