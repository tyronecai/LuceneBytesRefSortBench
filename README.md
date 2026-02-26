# require
jdk21

# build
mvn clean package

# run
java target/bench-1.0-SNAPSHOT-jar-with-dependencies.jar xxx.log 100 false

# run with reorder ids
java target/bench-1.0-SNAPSHOT-jar-with-dependencies.jar xxx.log 100 true
