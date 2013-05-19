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

import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import scala.Option.option2Iterable
import scala.annotation.tailrec
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.log.logger.BaseLogger
import org.digimead.digi.lib.log.logger.RichLogger.rich2slf4j
import org.slf4j.ILoggerFactory

import com.escalatesoft.subcut.inject.BindingModule
import com.escalatesoft.subcut.inject.{ Injectable => SubCutInjectable }

object LoggerFactory extends ILoggerFactory {

  def getLogger(name: String): org.slf4j.Logger = {
    new BaseLogger(name,
      LoggerFactory.configuration.isTraceEnabled,
      LoggerFactory.configuration.isDebugEnabled,
      LoggerFactory.configuration.isInfoEnabled,
      LoggerFactory.configuration.isWarnEnabled,
      LoggerFactory.configuration.isErrorEnabled)
  }
  /*
   * dependency injection
   */
  def configuration() = DI.configuration

  class BufferedLogThread extends Logging.BufferedLogThread() {
    lazy val flushLimit = Logging.inner.bufferedFlushLimit
    this.setDaemon(true)
    val lock = new AtomicReference[Option[Boolean]](Some(false))

    def init() = {
      start()
      threadResume()
    }
    @tailrec
    final override def run() = {
      val logging = Logging.inner
      if (!logging.bufferedQueue.isEmpty) {
        logging.flushQueue(flushLimit, 100)
        Thread.sleep(50)
      } else
        logging.bufferedQueue.synchronized { logging.bufferedQueue.wait }
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
      Logging.inner.bufferedQueue.synchronized { Logging.inner.bufferedQueue.notifyAll }
      lock.notifyAll()
    }
  }
  protected[log] def shutdownHook() {
    Logging.addToLog(new Date(), Thread.currentThread.getId, Record.Level.Debug, Logging.inner.commonLogger.getName, "buffered logging is preparing for shutdown")
    def isQueueEmpty(): Boolean = {
      if (!Logging.inner.bufferedQueue.isEmpty)
        return false
      Thread.sleep(500)
      Logging.inner.bufferedQueue.isEmpty
    }
    // wait for log messages 10min before termination
    breakable {
      for (i <- 0 to 1200)
        if (isQueueEmpty())
          break
        else
          Logging.inner.flush(0)
    }
    Logging.addToLog(new Date(), Thread.currentThread.getId, Record.Level.Debug, Logging.inner.commonLogger.getName, "no more log messages, shutdown")
    Logging.inner.deinit()
  }

  /**
   * Configuration use in LoggerFactory for newly create loggers
   */
  class Configuration(implicit val bindingModule: BindingModule) extends SubCutInjectable {
    val isTraceEnabled = injectOptional[Boolean]("Log.TraceEnabled") getOrElse true
    val isDebugEnabled = injectOptional[Boolean]("Log.DebugEnabled") getOrElse true
    val isInfoEnabled = injectOptional[Boolean]("Log.InfoEnabled") getOrElse true
    val isWarnEnabled = injectOptional[Boolean]("Log.WarnEnabled") getOrElse true
    val isErrorEnabled = injectOptional[Boolean]("Log.ErrorEnabled") getOrElse true
  }
  /**
   * Dependency injection routines
   */
  private object DI extends DependencyInjection.PersistentInjectable {
    implicit def bindingModule = DependencyInjection()
    /** Logging configuration DI cache */
    @volatile var configuration = inject[Configuration]

    override def injectionAfter(newModule: BindingModule) {
      configuration = inject[Configuration]
    }
  }
}
