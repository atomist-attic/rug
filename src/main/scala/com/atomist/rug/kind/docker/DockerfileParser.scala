package com.atomist.rug.kind.docker

import _root_.java.util
import java.io.InputStreamReader
import javax.script.{Invocable, ScriptEngineManager}

import com.atomist.util.Utils.withCloseable
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.core.io.ClassPathResource

import scala.collection.JavaConverters._

object DockerfileParser {

  val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  val consoleJs =
    """
      |console = {
      |   log: print,
      |   warn: print,
      |   error: print
      |};
    """.stripMargin
  def parse(content: String): Dockerfile = {
    val param = Option(content).getOrElse("")
    val content1 = param.replace("\r\n", "\n").replace("\r", "\n")
    withCloseable(new ClassPathResource("docker/parser.js").getInputStream)(is => {
      withCloseable(new InputStreamReader(is))(reader => {
        try {
          val engine = new ScriptEngineManager(null).getEngineByName("nashorn")
          engine.eval(consoleJs)
          engine.eval(reader)
          val invocable = engine.asInstanceOf[Invocable]
          val result = invocable.invokeFunction("parse", content1, Map("includeComments" -> "true").asJava)
          val lines = result match {
            case map: util.Map[AnyRef @unchecked, AnyRef @unchecked] =>
              map.asScala.values.map(c => mapper.convertValue(c, classOf[DockerfileLine])).toSeq
            case _ => throw new IllegalArgumentException("Failed to parse content")
          }
          new Dockerfile(lines)
        } catch {
          case e: Exception =>
            throw DockerfileException("Failed to parse Dockerfile", e)
        }
      })
    })
  }
}
