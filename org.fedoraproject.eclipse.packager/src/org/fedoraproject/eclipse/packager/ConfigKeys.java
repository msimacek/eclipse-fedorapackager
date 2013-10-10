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
 * The possible properties that can be found in a configuration file.
 *
 * @since 0.5
 */
public interface ConfigKeys {
	/****/ String LOOKASIDE 		= "lookaside"; 		//$NON-NLS-1$
	/****/ String LOOKASIDE_HASH 	= "lookasidehash"; 	//$NON-NLS-1$
	/****/ String LOOKASIDE_CGI 	= "lookaside_cgi"; 	//$NON-NLS-1$
	/****/ String GIT_BASE_URL 		= "gitbaseurl"; 	//$NON-NLS-1$
	/****/ String ANON_GIT_URL 		= "anongiturl"; 	//$NON-NLS-1$
	/****/ String TRAC_BASE_URL 	= "tracbaseurl"; 	//$NON-NLS-1$
	/****/ String BRANCH_RE 		= "branchre"; 		//$NON-NLS-1$
	/****/ String KOJI_CONFIG 		= "kojiconfig"; 	//$NON-NLS-1$
	/****/ String BUILD_CLIENT 		= "build_client";	//$NON-NLS-1$
}
