/**
 * Digi-Lib-SLF4J - SLF4J binding for Digi components
 *
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class LoggingTestSimpleInit extends FunSuite with BeforeAndAfter with ShouldMatchers {

  test("logging initialization via Record & Logging") {
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
  }
}

object A {
  class A extends Logging with java.io.Serializable
  val a = new A
  val bos = new ByteArrayOutputStream()
  val out = new ObjectOutputStream(bos)
  out.writeObject(a)
  val saved = bos.toByteArray()
  val bis = new ByteArrayInputStream(saved)
  val in = new ObjectInputStream(bis)
  val b = in.readObject()
  val nameA = a.log.getName
  val nameB = b.asInstanceOf[A].log.getName
}
