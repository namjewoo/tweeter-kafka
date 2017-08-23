package com.knoldus.kafka.producer

import java.util.Date
import java.util.concurrent.LinkedBlockingQueue

import com.knoldus.util.{JsonHelper, KafkaCassandraConfigUtil}
import org.slf4j.LoggerFactory
import twitter4j._
import twitter4j.conf.ConfigurationBuilder
case class Tweet(date:Date,text:String)
object TwitterStreamApp extends App with JsonHelper{

  val logger = LoggerFactory.getLogger(this.getClass)

  val queue = new LinkedBlockingQueue[Status](1000)

  val boundingBox: Array[Double] = Array(-180.0,-90.0)
  val boundingBox1: Array[Double] = Array(180.0,90.0)

  val keyWords = "narendramodi"

  val cb = new ConfigurationBuilder()
  cb.setDebugEnabled(true)
    .setOAuthConsumerKey(KafkaCassandraConfigUtil.consumerKey)
    .setOAuthConsumerSecret(KafkaCassandraConfigUtil.consumerSecret)
    .setOAuthAccessToken(KafkaCassandraConfigUtil.accessToken)
    .setOAuthAccessTokenSecret(KafkaCassandraConfigUtil.accessTokenSecret)

  val twitterStream = new TwitterStreamFactory(cb.build()).getInstance()
  var counter = 0
  val listener = new StatusListener() {

    override def onStatus(status: Status) {

      queue.offer(status)
      counter = counter + 1
      val user = status.getUser
      val tweetId = status.getId
      val userName = user.getName.replaceAll("'","''")

      //println(s"data start----->")
      val message = write(Tweet(status.getCreatedAt,status.getText))
      //println(s"data end------->")
      KafkaTwitterProducer.send(KafkaCassandraConfigUtil.kafka_topic,message)
      println(s"tweet text is ::::   ${message}  counter ::: ${counter}")

    }

    override def onDeletionNotice(statusDeletion_Notice: StatusDeletionNotice) = {

    }

    override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
      // System.out.println("Got track limitation notice:" +num-berOfLimitedStatuses);
    }

    override def onScrubGeo(userId: Long, upToStatusId: Long) {
      // System.out.println("Got scrub_geo event userId:" + userId +"upToStatusId:" + upToStatusId);
    }

    override def onStallWarning(warning: StallWarning) {
      // System.out.println("Got stall warning:" + warning);
    }

    override def onException(ex: Exception) {
      ex.printStackTrace()
    }
  }
  twitterStream.addListener(listener)

  val query = new FilterQuery().track(keyWords)
  //query.
  query.locations(boundingBox,boundingBox1)
  twitterStream.filter(query)


  Thread.sleep(100000000)


}
