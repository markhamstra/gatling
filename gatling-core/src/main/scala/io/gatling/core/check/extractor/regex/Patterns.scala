package io.gatling.core.check.extractor.regex

import java.util.regex.Pattern

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.ThreadSafeCache

class Patterns(implicit configuration: GatlingConfiguration) {

  private val patternCache = ThreadSafeCache[String, Pattern](configuration.core.extract.regex.cacheMaxCapacity)

  def extractAll[G: GroupExtractor](chars: CharSequence, pattern: String): Seq[G] = {

    val matcher = compilePattern(pattern).matcher(chars)
    matcher.foldLeft(List.empty[G]) { (matcher, values) =>
      matcher.value :: values
    }.reverse
  }

  def compilePattern(pattern: String): Pattern =
    if (patternCache.enabled) patternCache.getOrElsePutIfAbsent(pattern, Pattern.compile(pattern))
    else Pattern.compile(pattern)
}
