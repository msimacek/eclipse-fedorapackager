/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.internal.config;

import java.util.HashMap;

/**
 * A custom map to keep track of the properties of a
 * configuration file.
 *
 */
public class ConfigMap extends HashMap<String, String>{

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private static final String EMPTY = ""; //$NON-NLS-1$

	/**
	 * Default constructor to initialize the map to empty values.
	 *
	 * @param configDefinitions The only definitions found in a config file.
	 */
	public ConfigMap (String[] configDefinitions) {
		for (String key : configDefinitions) {
			super.put(key, EMPTY);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String put(String key, String value) {
		// only allow the keys declared in configDefinitions
		if (super.containsKey(key)) {
			return super.put(key, value);
		}
		return EMPTY;
	}
}
