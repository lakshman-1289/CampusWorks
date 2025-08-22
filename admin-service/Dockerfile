FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/admin-service-*.jar app.jar
EXPOSE 9009
ENTRYPOINT ["java","-jar","/app/app.jar"]

