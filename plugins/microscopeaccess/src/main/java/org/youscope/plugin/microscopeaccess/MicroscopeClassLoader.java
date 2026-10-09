/*******************************************************************************
 * Copyright © 2026 ETH Zürich
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.microscopeaccess;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author langmo
 *
 */
class MicroscopeClassLoader extends URLClassLoader
{
	MicroscopeClassLoader(URL[] urls, ClassLoader parent)
	{
		super(urls, parent);
	}

	/**
	 * This implementation prevents the parent class loaders to load any implementation
	 * class. Implementation classes are identified by the name: They have an "Impl" in
	 * it.
	 */
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		//if((name.indexOf("Impl") <= 0 || name.lastIndexOf("Impl") != name.length() - 4) && name.compareToIgnoreCase("mmcorej.CMMCore") != 0)
		if(name.indexOf(MicroscopeClassLoader.class.getPackage().getName()) != 0 && name.indexOf("mmcorej") != 0)
			return super.loadClass(name, resolve);
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		if(c == null)
		{
			c = findClass(name);
		}
		if(resolve)
		{
			resolveClass(c);
		}
		return c;
	}
}
