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
package org.fedoraproject.eclipse.packager;

/**
 * The possible properties that can be found in a koji configuration file.
 *
 * @since 0.5
 */
public interface KojiConfigKeys {
	/****/ String SERVER 		= "server"; 	//$NON-NLS-1$
	/****/ String AUTH_TYPE 	= "authtype"; 	//$NON-NLS-1$
	/****/ String TOP_DIR 		= "topdir"; 	//$NON-NLS-1$
	/****/ String WEB_URL 		= "weburl"; 	//$NON-NLS-1$
	/****/ String TOP_URL 		= "topurl"; 	//$NON-NLS-1$
	/****/ String CERT 			= "cert"; 		//$NON-NLS-1$
	/****/ String CA 			= "ca"; 		//$NON-NLS-1$
	/****/ String SERVER_CA 	= "serverca"; 	//$NON-NLS-1$
	/****/ String KRB_SERVICE 	= "krbservice";	//$NON-NLS-1$
}
