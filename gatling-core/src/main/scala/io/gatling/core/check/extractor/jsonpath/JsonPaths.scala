package io.gatling.core.check.extractor.jsonpath

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.ThreadSafeCache
import io.gatling.jsonpath.JsonPath
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

class JsonPaths(implicit configuration: GatlingConfiguration) {

  private val jsonPathCache = ThreadSafeCache[String, Validation[JsonPath]](configuration.core.extract.jsonPath.cacheMaxCapacity)

  def extractAll[X: JsonFilter](json: Any, expression: String): Validation[Iterator[X]] =
    compileJsonPath(expression).map(_.query(json).collect(implicitly[JsonFilter[X]].filter))

  def compileJsonPath(expression: String): Validation[JsonPath] = {

      def compile(expression: String): Validation[JsonPath] = JsonPath.compile(expression) match {
        case Left(error) => error.reason.failure
        case Right(path) => path.success
      }

    if (jsonPathCache.enabled) jsonPathCache.getOrElsePutIfAbsent(expression, compile(expression))
    else compile(expression)
  }
}
