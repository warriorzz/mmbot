FROM adoptopenjdk/openjdk15 as builder

COPY . .

RUN ./gradlew --no-daemon installDist

FROM adoptopenjdk/openjdk15

WORKDIR /user/app

COPY --from=builder build/install/mmbot ./

ENTRYPOINT ["/user/app/bin/mmbot"]