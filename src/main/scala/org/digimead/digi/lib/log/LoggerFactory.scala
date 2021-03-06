/**
 * Digi-Lib-SLF4J - SLF4J binding for Digi components
 *
 * Copyright (c) 2012-2014 Alexey Aksenov ezh@ezh.msk.ru
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

import com.escalatesoft.subcut.inject.{ BindingModule, Injectable ⇒ SubCutInjectable }
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import org.digimead.digi.lib.api.XDependencyInjection
import org.digimead.digi.lib.log.api.XLevel
import org.digimead.digi.lib.log.logger.BaseLogger
import org.slf4j.ILoggerFactory
import scala.annotation.tailrec
import scala.util.control.Breaks.{ break, breakable }

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
        try {
          logging.flushQueue(flushLimit, 100)
        } catch {
          case e: Throwable ⇒
            // This is the best of the worst: log thread is alive
            e.printStackTrace()
        }
        Thread.sleep(50)
      } else
        logging.bufferedQueue.synchronized { logging.bufferedQueue.wait }
      while (lock.get == Some(false))
        lock.synchronized { lock.wait() }
      if (lock.get.nonEmpty)
        run
    }
    def threadSuspend() = lock.synchronized {
      if (lock.get.isEmpty)
        throw new IllegalStateException("Lock disabled, BufferedLogThread is deinitialized.")
      lock.set(Some(false))
      lock.notifyAll()
    }
    def threadResume() = lock.synchronized {
      if (lock.get.isEmpty)
        throw new IllegalStateException("Lock disabled, BufferedLogThread is deinitialized.")
      lock.set(Some(true))
      lock.notifyAll()
    }
    def deinit() = lock.synchronized {
      if (lock.get.isEmpty)
        throw new IllegalStateException("Lock disabled, BufferedLogThread is deinitialized.")
      lock.set(None)
      Logging.inner.bufferedQueue.synchronized { Logging.inner.bufferedQueue.notifyAll }
      lock.notifyAll()
    }
  }
  protected[log] def shutdownHook() {
    Logging.addToLog(new Date(), Thread.currentThread.getId, XLevel.Debug, Logging.inner.commonLogger.getName,
      Logging.inner.getClass(), "Buffered logging is preparing for shutdown.")
    def isQueueEmpty(): Boolean = {
      if (!Logging.inner.bufferedQueue.isEmpty)
        return false
      Thread.sleep(500)
      Logging.inner.bufferedQueue.isEmpty
    }
    // wait for log messages 10min before termination
    breakable {
      for (i ← 0 to 1200)
        if (isQueueEmpty())
          break
        else
          Logging.inner.flush(0)
    }
    Logging.addToLog(new Date(), Thread.currentThread.getId, XLevel.Debug, Logging.inner.commonLogger.getName,
      Logging.inner.getClass(), "No more log messages, shutdown.")
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
  private object DI extends XDependencyInjection.PersistentInjectable {
    /** Logging configuration DI cache */
    val configuration = inject[Configuration]
  }
}
