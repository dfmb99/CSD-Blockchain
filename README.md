# CSD-Blockchain

## Installation guide

1º - Clone repository

2º -Install bft-smart library that is in bin folder
``` java
cd CSD-Blockchain
mvn install:install-file -Dfile=./bin/BFT-SMaRt.jar -DgroupId=bft-smart -DartifactId=library -Dversion=1.2 -Dpackaging=jar
```

3º - Compile maven project
``` java
mvn compile
``` 

3º - Launch servers
``` java
mvn exec:java -Dexec.mainClass=server.RESTServer -Dexec.args="0 1000 8080"
mvn exec:java -Dexec.mainClass=server.RESTServer -Dexec.args="1 1001 8081"
mvn exec:java -Dexec.mainClass=server.RESTServer -Dexec.args="2 1002 8082"
mvn exec:java -Dexec.mainClass=server.RESTServer -Dexec.args="3 1003 8083"
``` 

4º - Launch client to make requests
``` java
mvn exec:java -Dexec.mainClass=client.Client
```
## Testing results
Data of the tests is in the latency_test_data.xlsx file, every test was run 50 times, and the result is the average.
<br /> <br /> 
![alt text](latency_test.PNG)
