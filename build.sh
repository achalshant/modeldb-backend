mvn clean
mvn package -Dmaven.test.skip=true
docker build -t modeldb-backend:latest -f dockerfile --rm .
docker build -t mongo-backend:latest -f mongo-dockerfile --rm .
