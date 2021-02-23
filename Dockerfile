FROM bellsoft/liberica-openjdk-alpine:11
RUN apk add tzdata
WORKDIR /usr/src/gymmanager/
COPY ./target/gymmanager.jar .
CMD java \
    -Dbot.username=${BOT_USERNAME} \
    -Dbot.token=${BOT_TOKEN} \
    -Dspring.datasource.hostname=${DB_HOST} \
    -Dspring.datasource.username=${DB_USER} \
    -Dspring.datasource.password=${DB_PASSWORD} \
    -jar gymmanager.jar
