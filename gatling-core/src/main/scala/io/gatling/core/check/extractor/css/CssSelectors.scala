package io.gatling.core.check.extractor.css

import java.util.{ List => JList }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.ThreadSafeCache
import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.NodeSelector
import jodd.log.LoggerFactory
import jodd.log.impl.Slf4jLoggerFactory

import scala.collection._
import scala.collection.JavaConversions.asScalaBuffer

class CssSelectors(implicit configuration: GatlingConfiguration) {

  LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory)

  private val domBuilder = Jodd.newLagartoDomBuilder
  private val selectorCache = ThreadSafeCache[String, JList[JList[CssSelector]]](configuration.core.extract.css.cacheMaxCapacity)

  def parse(chars: Array[Char]) = new NodeSelector(domBuilder.parse(chars))

  def parse(string: String) = new NodeSelector(domBuilder.parse(string))

  private def parseQuery(query: String): JList[JList[CssSelector]] =
    if (selectorCache.enabled) selectorCache.getOrElsePutIfAbsent(query, CSSelly.parse(query))
    else CSSelly.parse(query)

  def extractAll(selector: NodeSelector, criterion: (String, Option[String])): Vector[String] = {

    val (query, nodeAttribute) = criterion
    val selectors = parseQuery(query)

    selector.select(selectors).flatMap { node =>
      nodeAttribute match {
        case Some(attr) => Option(node.getAttribute(attr))
        case _          => Some(node.getTextContent.trim)
      }
    }(breakOut)
  }
}
