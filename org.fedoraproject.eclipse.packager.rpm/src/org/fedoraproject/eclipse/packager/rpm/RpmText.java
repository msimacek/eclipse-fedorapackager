/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.rpm;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * Utility class for String externalization.
 *
 */
public class RpmText extends NLS {
	
	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 * 
	 * This is the path to the file containing externalized strings.
	 */
	private static final String BUNDLE_NAME = "org.fedoraproject.eclipse.packager.rpm.rpmtext"; //$NON-NLS-1$


	// LocalBuildHandler Strings
	/****/ public static String LocalBuildHandler_buildForLocalArch;
	/****/ public static String LocalBuildHandler_jobName;
	// MockBuildHandler Strings
	/****/ public static String MockBuildHandler_jobName;
	/****/ public static String MockBuildHandler_testLocalBuildWithMock;
	/****/ public static String MockBuildHandler_callMockMsg;
	/****/ public static String MockBuildHandler_mockNotInstalled;
	// PrepHandler Strings
	/****/ public static String PrepHandler_attemptApplyPatchMsg;
	/****/ public static String PrepHandler_jobName;
	// RPMHandler Strings
	/****/ public static String RpmBuildHandler_consoleName;
	/****/ public static String RpmBuildHandler_runShellCmds;
	/****/ public static String RpmBuildHandler_scriptCancelled;
	/****/ public static String RpmBuildHandler_userWarningMsg;
	/****/ public static String RpmBuildHandler_terminationMsg;
	// SRPMHandler Strings
	/****/ public static String CreateSRPMHandler_jobName;
	/****/ public static String CreateSRPMHandler_buildSrpm;
	// RpmBuildCommand
	/****/ public static String RpmBuildCommand_distDefinesNullError;
	/****/ public static String RpmBuildCommand_flagsNullError;
	/****/ public static String RpmBuildCommand_commandStringMsg;
	/****/ public static String RpmBuildCommand_buildTypeRequired;
	/****/ public static String RpmBuildCommand_callRpmBuildMsg;
	// RpmEvalCommand
	/****/ public static String RpmEvalCommand_variableMustBeSet;
	// MockBuildCommand
	/****/ public static String MockBuildCommand_buildArchNullError;
	/****/ public static String MockBuildCommand_archException;
	/****/ public static String MockBuildCommand_invalidMockConfigError;
	// RpmEvalCommandException
	/****/ public static String RpmEvalCommandException_msg;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, RpmText.class);
	}
}