/**
 * Digi-Lib-SLF4J - SLF4J binding for Digi components
 *
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
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

import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import scala.Option.option2Iterable
import scala.annotation.tailrec
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.DependencyInjection.PersistentInjectable
import org.digimead.digi.lib.log.Logging.instance2Logging
import org.digimead.digi.lib.log.logger.BaseLogger
import org.digimead.digi.lib.log.logger.RichLogger.rich2slf4j
import org.scala_tools.subcut.inject.BindingModule
import org.scala_tools.subcut.inject.{ Injectable => SubCutInjectable }
import org.slf4j.ILoggerFactory

object LoggerFactory extends PersistentInjectable with ILoggerFactory {
  implicit def bindingModule = DependencyInjection()
  @volatile private var instance = inject[Configuration]

  def getLogger(name: String): org.slf4j.Logger = {
    new BaseLogger(name,
      Logging.isTraceWhereEnabled,
      LoggerFactory.instance.isTraceEnabled,
      LoggerFactory.instance.isDebugEnabled,
      LoggerFactory.instance.isInfoEnabled,
      LoggerFactory.instance.isWarnEnabled,
      LoggerFactory.instance.isErrorEnabled,
      Logging.record.pid,
      Logging.record.builder,
      Logging.offer)
  }
  def reloadInjection() = synchronized {
    instance = inject[Configuration]
  }
  class BufferedLogThread extends Logging.BufferedLogThread() {
    lazy val flushLimit = Logging.bufferedFlushLimit
    this.setDaemon(true)
    val lock = new AtomicReference[Option[Boolean]](Some(false))

    def init() = {
      start()
      threadResume()
    }
    @tailrec
    final override def run() = {
      if (!Logging.bufferedQueue.isEmpty) {
        Logging.flushQueue(flushLimit, 100)
        Thread.sleep(50)
      } else
        Logging.bufferedQueue.synchronized { Logging.bufferedQueue.wait }
      while (lock.get == Some(false))
        lock.synchronized { lock.wait() }
      if (lock.get.nonEmpty)
        run
    }
    def threadSuspend() = lock.synchronized {
      assert(lock.get.nonEmpty, "lock disabled, BufferedLogThread deinitialized")
      lock.set(Some(false))
      lock.notifyAll()
    }
    def threadResume() = lock.synchronized {
      assert(lock.get.nonEmpty, "lock disabled, BufferedLogThread deinitialized")
      lock.set(Some(true))
      lock.notifyAll()
    }
    def deinit() = lock.synchronized {
      assert(lock.get.nonEmpty, "lock disabled, BufferedLogThread deinitialized")
      lock.set(None)
      Logging.bufferedQueue.synchronized { Logging.bufferedQueue.notifyAll }
      lock.notifyAll()
    }
  }
  protected[log] def shutdownHook() {
    Logging.addToLog(new Date(), Thread.currentThread.getId, Record.Level.Debug, Logging.commonLogger.getName, "buffered logging is preparing for shutdown")
    def isQueueEmpty(): Boolean = {
      if (!Logging.bufferedQueue.isEmpty)
        return false
      Thread.sleep(500)
      Logging.bufferedQueue.isEmpty
    }
    // wait for log messages 10min before termination
    breakable {
      for (i <- 0 to 1200)
        if (isQueueEmpty())
          break
        else
          Logging.flush(0)
    }
    Logging.addToLog(new Date(), Thread.currentThread.getId, Record.Level.Debug, Logging.commonLogger.getName, "no more log messages, shutdown")
    Logging.deinit()
  }

  class Configuration(implicit val bindingModule: BindingModule) extends SubCutInjectable {
    val isTraceEnabled = injectOptional[Boolean]("Log.TraceEnabled") getOrElse true
    val isDebugEnabled = injectOptional[Boolean]("Log.DebugEnabled") getOrElse true
    val isInfoEnabled = injectOptional[Boolean]("Log.InfoEnabled") getOrElse true
    val isWarnEnabled = injectOptional[Boolean]("Log.WarnEnabled") getOrElse true
    val isErrorEnabled = injectOptional[Boolean]("Log.ErrorEnabled") getOrElse true
  }
}
