package dev.ogai.anymind

import java.util.Properties

import scala.jdk.CollectionConverters._
import scala.util.Try

import org.apache.kafka.clients.admin.{ AdminClient, AdminClientConfig }

object Utils {
  def createTopic[V](bootstrap: String, topic: Topic[V]): Unit = {
    val props = new Properties()
    props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
    props.setProperty(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000.toString)
    props.setProperty(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000.toString)
    val adminClient = AdminClient.create(props)
    Try {
      val res = adminClient.createTopics(List(topic.asNewTopic).asJavaCollection)
      res.all().get()
    }
    ()
  }
}
