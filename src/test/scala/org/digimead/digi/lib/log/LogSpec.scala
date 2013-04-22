/**
 * Digi-Lib-SLF4J - SLF4J binding for Digi components
 *
 * Copyright (c) 2012-2013 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.digimead.digi.lib.log

import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.log.logger.RichLogger.rich2slf4j
import org.scalatest.FunSpec
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.ShouldMatchers

import com.escalatesoft.subcut.inject.NewBindingModule

class LogSpec extends FunSpec with ShouldMatchers {
  describe("A Log") {
    it("should have proper reinitialization") {
      DependencyInjection.get.foreach(_ => DependencyInjection.clear)
      val config = org.digimead.digi.lib.log.slf4j.default ~ org.digimead.digi.lib.default
      DependencyInjection.set(config)

      val loggerFactoryConfig = config.inject[LoggerFactory.Configuration](None)
      loggerFactoryConfig should be theSameInstanceAs (config.inject[LoggerFactory.Configuration](None))
      config.inject[Option[Logging.BufferedLogThread]](Some("Log.BufferedThread")) should not be theSameInstanceAs
      (config.inject[Option[Logging.BufferedLogThread]](Some("Log.BufferedThread")))
      loggerFactoryConfig.isTraceEnabled should be(true)
      LoggerFactory.getLogger("A").isTraceEnabled() should be(true)

      val newConfig = config ~ (NewBindingModule.newBindingModule(module => {
        module.bind[Boolean] identifiedBy "Log.TraceEnabled" toSingle { false }
      }))
      DependencyInjection.reset(newConfig)
      val newLoggerFactoryConfig = newConfig.inject[LoggerFactory.Configuration](None)
      loggerFactoryConfig should not be theSameInstanceAs(newLoggerFactoryConfig)
      newLoggerFactoryConfig.isTraceEnabled should be(false)
      LoggerFactory.getLogger("B").isTraceEnabled() should be(false)
    }
    it("should create singeton with default parameters") {
      DependencyInjection.get.foreach(_ => DependencyInjection.clear)
      val config = org.digimead.digi.lib.log.slf4j.default ~ org.digimead.digi.lib.default
      DependencyInjection.set(config)
      val instance = Logging.inner
      instance.record should not be (null)
      instance.builder should not be (null)
      instance.isTraceWhereEnabled should be(false)
      instance.bufferedThread should not be ('empty)
      instance.bufferedFlushLimit should be(1000)
      instance.shutdownHook should not be ('empty)
      instance.bufferedAppender.size should be(1)
      instance.richLogger.size should be(1)
      instance.commonLogger should not be (null)

      val testClass = new Loggable {
        log.debug("hello")
      }

      /*      val thread = new Thread {
        override def run {
          val testClass = new Loggable {
            log.debug("hello")
          }
        }
      }
      thread.start
      thread.join()*/
    }
  }
  /*  test("logging initialization via Record & Logging") {
    val thread = new Thread {
      override def run {
        val testClass = new Logging() {
          log.debug("hello")
        }
      }
    }
    thread.start
    thread.join()
    Logging.resume()
    Logging.addAppender(appender.Console)
    Logging.delAppender(appender.Console)
  }

  test("serializable") {
    assert(A.nameA === A.nameB)
  }

  test("crazy behavior") {
    Logging.addAppender(appender.Console)
    Logging.commonLogger.debug("Scheduling a background task check for {}.{} with {}",
      Array[Object]("a", "b", "c"))
    // System.exit(0)
    Logging.delAppender(appender.Console)
  }*/
}

object A {
  val z = org.slf4j.LoggerFactory.getLogger(getClass)
  /*  class A extends Logging with java.io.Serializable
  val a = new A
  val bos = new ByteArrayOutputStream()
  val out = new ObjectOutputStream(bos)
  out.writeObject(a)
  val saved = bos.toByteArray()
  val bis = new ByteArrayInputStream(saved)
  val in = new ObjectInputStream(bis)
  val b = in.readObject()
  val nameA = a.log.getName
  val nameB = b.asInstanceOf[A].log.getName*/
}
