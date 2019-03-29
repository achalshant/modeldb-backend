FROM openjdk:latest

RUN mkdir -p modeldb-project-work-dir
WORKDIR /modeldb-project-work-dir

COPY target/modeldb-1.0-SNAPSHOT-client-build.jar modeldb-1.0-SNAPSHOT-client-build.jar

COPY ./wait-for-it.sh wait-for-it.sh
RUN chmod +x ./wait-for-it.sh

# Define environment variable
ENV NAME modeldb-backend
ENV VERTA_MODELDB_CONFIG /config/config.yaml

ENTRYPOINT ["bash", "-c"]
CMD ["./wait-for-it.sh mysql-backend:3306 --timeout=30 && java -jar modeldb-1.0-SNAPSHOT-client-build.jar"]

# ./wait-for-it.sh artifact-store-backend:8086 --timeout=30 && 