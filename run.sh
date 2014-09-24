#./sbt/sbt "run -m $master -a cf hdfs://hadoop-001:9000/user/cray/als/test.data hdfs://hadoop-001:9000/user/cray/als/output"
spark-submit  --class ScalaParseDate target/scala-2.10/scalaparsedate_2.10-0.1-SNAPSHOT.jar

