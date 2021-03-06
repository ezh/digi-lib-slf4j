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

package org.slf4j.impl;

import org.slf4j.spi.MDCAdapter;

public class StaticMDCBinder {
	/**
	 * The unique instance of this class.
	 */
	public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

	private StaticMDCBinder() {
	}

	/**
	 * Currently this method always returns an instance of
	 * {@link StaticMDCBinder}.
	 */
	public MDCAdapter getMDCA() {
		return new org.digimead.digi.lib.log.MDC();
	}

	public String getMDCAdapterClassStr() {
		return org.digimead.digi.lib.log.MDC.class.getName();
	}
}
