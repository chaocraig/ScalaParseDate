package com.vpon.examples


import java.util.{Date, Locale}
import java.text.SimpleDateFormat
import java.io._
import java.io.IOException;
import java.text.ParseException;

import java.net.DatagramSocket
import java.net.ServerSocket
import java.util.Random
import java.util.regex._

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.{RDD => SparkRDD}


object ParseDate {
  private val MINPORT     = 10000
  private val MAXPORT     = 49151
  private val MAXTRY      = 100
  private val DEBUG_LEVEL = 3
  private val DATE_ERR    = "2000-00-00"

  private val SRC_FILEPATH="hdfs://hadoop-001:9000/user/cray/app_dect/imei_all.txt"
  private val DST_FILEPATH="hdfs://hadoop-001:9000/user/cray/app_dect/imei_all_dir"

  // val SRC_FILEPATH="/Users/cray/Downloads/scala/imei_all_sample.txt"
  // val DST_FILEPATH="/Users/cray/Downloads/scala/imei_all_sample-cvt.txt"


    
  def ConvertDateHour(stringSrcDate:String) : String = {
    
      val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat("dd/MMM/yyyy:h:mm:ss", Locale.US)

      val df = new SimpleDateFormat("HH\tyyyy-MM-dd")

      
      try {
    	  val timeNew = simpleDateFormat.parse(stringSrcDate)
    	  val stringNewDateHour=df.format(timeNew)
        
    	  stringNewDateHour
      } catch {
        case _: Exception => {
        	if (DEBUG_LEVEL > 1) println ("===> ParseDate Err")
        	if (DEBUG_LEVEL > 2) println ("===> " + stringSrcDate)
        }
          DATE_ERR
      }
  }
  

  def ReadAllFromFile(sc:SparkContext, filename:String) : SparkRDD[String] =  {
     // val listLines = scala.io.Source.fromFile(filename, "utf-8").getLines.toList
    val listLines = sc.textFile(filename)
    
    if (DEBUG_LEVEL > 1) println ("---> ReadAllFromFile() finished.")
    if (DEBUG_LEVEL > 2) println ("---> ReadAllFromFile() Count: " + listLines.count + "\n" + listLines)

    listLines
  }
  
 

  def DoConversions(listLines:SparkRDD[String]) : SparkRDD[String] = {   
    
    
    val listListLines = listLines.map(_.split('\t') match {
      
       case Array(date_time, imei, pkg, phone, os) => imei.trim+"\t"+pkg.trim+"\t"+phone.trim+"\t"+os.trim+"\t"+
                                                       ConvertDateHour(date_time.trim)
       case _ => "imei\tpkg\tphone\tos\ttime\tdate"  
    })

    if (DEBUG_LEVEL > 1) println ("---> DoConversions() finished.")
    if (DEBUG_LEVEL > 2) println ("---> DoConversions() Count: " + listListLines.count + "\n" + listLines)
    if (DEBUG_LEVEL > 5) listListLines.foreach(println (_))

    listListLines
  }
  
  
  def WriteAlltoFile(listLines:SparkRDD[String], stringPath:String)  {
      

      try {
    	  listLines.saveAsTextFile(stringPath)
      } catch  {
          case ex: IOException => {
            println("IO Exception")
         }
      } finally {
    	  if (DEBUG_LEVEL > 1) println ("---> WriteAlltoFile() finished.")
    	  if (DEBUG_LEVEL > 2) println ("---> WriteAlltoFile() Count: " + listLines.count + "\n" + listLines)
    	  if (DEBUG_LEVEL > 5) listLines.foreach(println (_)) 
      }
      
  }
  
  
    def findAvailablePort: Int = {
    var ss: ServerSocket = null
    var ds: DatagramSocket = null
    var port: Int = 0
    val rand = new Random()

    1 to MAXTRY find {i =>
      try {
        val tryport = MINPORT + rand.nextInt(MAXPORT-MINPORT)
        ss = new ServerSocket(tryport)
        ss.setReuseAddress(true)

        ds = new DatagramSocket(tryport)
        ds.setReuseAddress(true)

        port = tryport
        true
      } catch {
        case e: Exception => false
      } finally {
        if (ss != null) ss.close
        if (ds != null) ds.close
      }
    }

    port
  }


  def findJars: Seq[String] = {
    val jarOfClass = SparkContext.jarOfClass(this.getClass)
    val version = scala.util.Properties.versionNumberString
      .split('.').init.mkString(".")
    // no jar found, assume running from sbt, try to add jars
    if (jarOfClass.length == 0) {
      new java.io.File("target/scala-" + version)
        .listFiles.map(_.getPath).filter(_.endsWith(".jar"))
    } else
      jarOfClass
  }


  
  def setSparkEnv(master:String) : SparkContext = {

    val conf = new SparkConf()
       .setMaster(master)
       .setAppName("mllib tutorial")
       // runtime Spark Home, set by env SPARK_HOME or explicitly as below
       //.setSparkHome("/opt/spark")

       // be nice or nasty to others (per node)
       .set("spark.executor.memory", "4g")
       //.set("spark.core.max", "8")

       // find a random port for driver application web-ui
       .set("spark.ui.port", findAvailablePort.toString)
       .setJars(findJars)
       
       // The coarse-grained mode will instead launch only one long-running Spark task on each Mesos machine, 
       // and dynamically schedule its own “mini-tasks” within it. The benefit is much lower startup overhead, 
       // but at the cost of reserving the Mesos resources for the complete duration of the application.
       // .set("spark.mesos.coarse", "true")

    // for debug purpose
    println("sparkconf: " + conf.toDebugString)

    val sc = new SparkContext(conf)
    sc
  }


  def main(args:Array[String]){
    
    val sc = setSparkEnv( args(0) )
    
    WriteAlltoFile(DoConversions(ReadAllFromFile(sc, SRC_FILEPATH)), DST_FILEPATH)
  }
  
}
  
