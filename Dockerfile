FROM adoptopenjdk/openjdk16:alpine as builder

COPY . .

RUN ./gradlew --no-daemon installDist

FROM adoptopenjdk/openjdk16:alpine

WORKDIR /usr/app

COPY --from=builder build/install/mmbot ./

ENTRYPOINT ["/usr/app/bin/mmbot"]
