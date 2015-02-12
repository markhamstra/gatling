package io.gatling.core.check.extractor.xpath

import java.nio.charset.StandardCharsets._
import javax.xml.transform.sax.SAXSource

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.ThreadSafeCache
import net.sf.saxon.s9api._
import org.xml.sax.InputSource

class Saxon(implicit configuration: GatlingConfiguration) {

  val enabled = Seq(UTF_8, UTF_16, US_ASCII, ISO_8859_1).contains(configuration.core.charset)

  private val processor = new Processor(false)
  private val documentBuilder = processor.newDocumentBuilder

  private val compilerCache = ThreadSafeCache[List[(String, String)], XPathCompiler](configuration.core.extract.xpath.cacheMaxCapacity)
  private val executableCache = ThreadSafeCache[String, XPathExecutable](configuration.core.extract.xpath.cacheMaxCapacity)

  def parse(inputSource: InputSource) = {
    inputSource.setEncoding(configuration.core.encoding)
    val source = new SAXSource(inputSource)
    documentBuilder.build(source)
  }

  def evaluateXPath(criterion: String, namespaces: List[(String, String)], xdmNode: XdmNode): XdmValue = {
    val xPathSelector = compileXPath(criterion, namespaces).load
    try {
      xPathSelector.setContextItem(xdmNode)
      xPathSelector.evaluate
    } finally {
      xPathSelector.getUnderlyingXPathContext.setContextItem(null)
    }
  }

  private def compileXPath(expression: String, namespaces: List[(String, String)]): XPathExecutable = {

      def xPathCompiler(namespaces: List[(String, String)]) = {
        val compiler = processor.newXPathCompiler
        for {
          (prefix, uri) <- namespaces
        } compiler.declareNamespace(prefix, uri)
        compiler
      }

    if (executableCache.enabled)
      executableCache.getOrElsePutIfAbsent(expression, compilerCache.getOrElsePutIfAbsent(namespaces, xPathCompiler(namespaces)).compile(expression))
    else
      xPathCompiler(namespaces).compile(expression)
  }
}
