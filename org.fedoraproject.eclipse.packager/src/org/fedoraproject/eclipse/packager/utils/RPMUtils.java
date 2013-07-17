/*******************************************************************************
 * Copyright (c) 2010-2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.rpm.core.utils.RPMQuery;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.linuxtools.rpm.ui.editor.parser.Specfile;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;

/**
 * Utility class for RPM related things.
 *
 */
public class RPMUtils {

	private static final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();

	/**
	 * Creates a list of rpm defines to use the given directory as a base
	 * directory.
	 *
	 * @param dir
	 *            The base directory.
	 * @return Defines to instruct rpmbuild to use given directory.
	 */
	public static List<String> getRPMDefines(String dir) {
		ArrayList<String> rpmDefines = new ArrayList<>();
		rpmDefines.add("--define"); //$NON-NLS-1$
		rpmDefines.add("_sourcedir " + dir); //$NON-NLS-1$
		rpmDefines.add("--define"); //$NON-NLS-1$
		rpmDefines.add("_builddir " + dir); //$NON-NLS-1$
		rpmDefines.add("--define"); //$NON-NLS-1$
		rpmDefines.add("_srcrpmdir " + dir); //$NON-NLS-1$
		rpmDefines.add("--define"); //$NON-NLS-1$
		rpmDefines.add("_rpmdir " + dir); //$NON-NLS-1$

		return rpmDefines;
	}

	/**
	 * Submit a query to RPM. Uses org.eclipse.linuxtools.rpm.Utils.
	 *
	 * @param projectRoot The root under which the query is occuring.
	 * @param format The format of the query result String.
	 * @param bci The configuration for the current branch.
	 * @return The result of the query.
	 * @throws IOException
	 *             If rpm command failed.
	 */
	public static String rpmQuery(IProjectRoot projectRoot, String format,
			BranchConfigInstance bci) throws IOException {
		IResource parent = projectRoot.getSpecFile().getParent();
		String dir = parent.getLocation().toString();
		List<String> defines = getRPMDefines(dir);
		List<String> distDefines = getDistDefines(bci);

		String result = null;
		defines.add(0, "rpm"); //$NON-NLS-1$
		defines.addAll(distDefines);
		defines.add("-q"); //$NON-NLS-1$
		defines.add("--qf"); //$NON-NLS-1$
		defines.add("%{" + format + "}\\n"); //$NON-NLS-1$//$NON-NLS-2$
		defines.add("--specfile"); //$NON-NLS-1$
		defines.add(projectRoot.getSpecFile().getLocation().toString());

		result = Utils.runCommandToString(defines.toArray(new String[0]));

		return result.substring(0, result.indexOf('\n'));
	}

	/**
	 * Returns the N-V-R retrieved from the .spec file in the project root.
	 *
	 * @param projectRoot
	 *            Container used to retrieve the needed data.
	 * @param bci
	 *            Current branch configuration.
	 * @return N-V-R (Name-Version-Release) retrieved.
	 */
	public static String getNVR(IProjectRoot projectRoot,
			BranchConfigInstance bci) {
		Specfile specfile = projectRoot.getSpecfileModel();
		String str = ""; //$NON-NLS-1$
		try {
			str = (RPMQuery.eval(specfile.getName()).trim() + "-" + specfile.getVersion() + "-" + specfile.getRelease().replace("%{?dist}", bci.getDist())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (CoreException e) {
			logger.logError(FedoraPackagerText.FedoraPackagerUtils_cannotEvalPackageName, e);
		}
		return str;
	}

	/**
	 * Get distribution definitions required for RPM build.
	 *
	 * @param bci
	 *            The BranchConfigInstance on which to base the definitions.
	 * @return A list of required dist-defines.
	 */
	public static List<String> getDistDefines(BranchConfigInstance bci) {
		// substitution for rhel
		ArrayList<String> distDefines = new ArrayList<>();
		String distvar = bci.getDistVariable().equals("epel") ? "rhel" //$NON-NLS-1$//$NON-NLS-2$
				: bci.getDistVariable();
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add("dist " + bci.getDist()); //$NON-NLS-1$
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add(distvar + ' ' + bci.getDistVal());
		return distDefines;
	}
}
