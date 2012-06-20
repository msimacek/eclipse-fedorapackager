/*******************************************************************************
 * Copyright (c) 2010-2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.rpm.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Result of a call to {@link RpmBuildCommand}.
 */
public class RpmBuildResult extends Result {

	private boolean success;
	private Set<String> srpms;

	/**
	 * 
	 * @param cmdList
	 *            The command and arguments run.
	 */
	public RpmBuildResult(String[] cmdList) {
		super(cmdList);
		this.srpms = new HashSet<String>();
	}

	/**
	 * @param success
	 *            the success to set
	 */
	public void setSuccessful(boolean success) {
		this.success = success;
	}

	/**
	 * Collect SRPM related output in this result
	 * 
	 * @param line
	 *            The output line from the command describing the location of
	 *            the srpm.
	 */
	public void addSrpm(String line) {
		// of the form "Wrote: path/to/src.rpm
		this.srpms.add(line.split("\\s+")[1]); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.api.ICommandResult#wasSuccessful()
	 */
	@Override
	public boolean isSuccessful() {
		return this.success;
	}

	/**
	 * Returns the path to the written SRPM if applicable.
	 * 
	 * @return The absolute path to the source RPM or {@code null} if there are
	 *         none.
	 */
	public String getAbsoluteSRPMFilePath() {
		for (String srpm : srpms) {
			return srpm; // there really should only be one
		}
		return null;
	}

}
