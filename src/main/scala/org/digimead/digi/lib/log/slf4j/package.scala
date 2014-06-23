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

import scala.collection.immutable.HashSet

import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.log.api.XAppender
import org.digimead.digi.lib.log.appender.Console

import com.escalatesoft.subcut.inject.NewBindingModule

package object slf4j {
  lazy val default = new NewBindingModule(module => {
    module.bind[() => Any] identifiedBy "Log.ShutdownHook" toSingle { () => LoggerFactory.shutdownHook }
    module.bind[Option[Logging.BufferedLogThread]] identifiedBy "Log.BufferedThread" toProvider { Some(new LoggerFactory.BufferedLogThread) }
    module.bind[HashSet[XAppender]] identifiedBy "Log.BufferedAppenders" toSingle { HashSet[XAppender](Console) }
    module.bind[LoggerFactory.Configuration] toModuleSingle { implicit module => new LoggerFactory.Configuration }
  })
  DependencyInjection.setPersistentInjectable("org.digimead.digi.lib.log.LoggerFactory$DI$")
}
