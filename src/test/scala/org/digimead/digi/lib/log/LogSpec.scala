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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.log.api.Loggable
import org.digimead.digi.lib.log.api.Level
import org.digimead.lib.test.OSGiHelper
import org.scalatest.BeforeAndAfter
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

import com.escalatesoft.subcut.inject.NewBindingModule

class LogSpec000 extends WordSpec with OSGiHelper with BeforeAndAfter with ShouldMatchers with Loggable {
  val testBundleClass = org.digimead.digi.lib.log.slf4j.default.getClass()

  "A Log" should {
    "be persistent" in {
      val config = org.digimead.digi.lib.log.slf4j.default ~ org.digimead.digi.lib.default
      DependencyInjection(config)

      adjustOSGiBefore

      val loggerFactoryConfig = config.inject[LoggerFactory.Configuration](None)
      loggerFactoryConfig should be theSameInstanceAs (config.inject[LoggerFactory.Configuration](None))
      config.inject[Option[Logging.BufferedLogThread]](Some("Log.BufferedThread")) should not be theSameInstanceAs
      (config.inject[Option[Logging.BufferedLogThread]](Some("Log.BufferedThread")))
      loggerFactoryConfig.isTraceEnabled should be(true)
      LoggerFactory.getLogger("A").isTraceEnabled() should be(true)

      val newConfig = config ~ (NewBindingModule.newBindingModule(module => {
        module.bind[Boolean] identifiedBy "Log.TraceEnabled" toSingle { false }
      }))
      DependencyInjection(newConfig, false)
      val newLoggerFactoryConfig = newConfig.inject[LoggerFactory.Configuration](None)
      loggerFactoryConfig should not be theSameInstanceAs(newLoggerFactoryConfig)
      newLoggerFactoryConfig.isTraceEnabled should be(false)
      LoggerFactory.getLogger("B").isTraceEnabled() should be(true)

      adjustOSGiAfter
    }
    "be serializable" in {
      val a = new LogSpec.LogSerializable
      val bos = new ByteArrayOutputStream()
      val out = new ObjectOutputStream(bos)
      out.writeObject(a)
      val saved = bos.toByteArray()
      val bis = new ByteArrayInputStream(saved)
      val in = new ObjectInputStream(bis)
      val b = in.readObject()
      val nameA = a.log.getName
      val nameB = b.asInstanceOf[LogSpec.LogSerializable].log.getName
      nameA should be(nameB)
    }
    "accumulate records in bufferedQueue" in {
      val size = Logging.inner.bufferedQueue.size()
      val thread = new Thread {
        override def run {
          val testClass = new Loggable() {
            log.debug("hello")
          }
        }
      }
      thread.start
      thread.join()
      Logging.inner.bufferedQueue.size() should be(size + 1)
      val record = Logging.inner.bufferedQueue.toArray.last.asInstanceOf[Message]
      record.level should be(Level.Debug)
      record.message should be("hello")
    }
  }
  class Test extends Loggable
}

class LogSpec001 extends WordSpec with ShouldMatchers {
  "A Log" should {
    "create singeton with default parameters" in {
      val config = org.digimead.digi.lib.log.slf4j.default ~ org.digimead.digi.lib.default
      DependencyInjection(config)
      val instance = Logging.inner
      instance.record should not be (null)
      instance.builder should not be (null)
      instance.isWhereEnabled should be(false)
      instance.bufferedThread should not be ('empty)
      instance.bufferedFlushLimit should be(1000)
      instance.shutdownHook should not be ('empty)
      instance.bufferedAppender.size should be(1)
      instance.bufferedAppender.head.getClass should be(org.digimead.digi.lib.log.appender.Console.getClass)
      instance.richLogger.size should be(0)
      instance.bufferedQueue.size() should be(0)
      val testClass = new Loggable {
        log.debug("hello")
      }
      instance.bufferedQueue.size() should be(1)
      instance.richLogger.size should be(1)
      instance.commonLogger should not be (null)
    }
  }

  /*test("crazy behavior") {
    Logging.addAppender(appender.Console)
    Logging.commonLogger.debug("Scheduling a background task check for {}.{} with {}",
      Array[Object]("a", "b", "c"))
    // System.exit(0)
    Logging.delAppender(appender.Console)
  }*/
}

object LogSpec {
  class LogSerializable extends Loggable with java.io.Serializable
}
