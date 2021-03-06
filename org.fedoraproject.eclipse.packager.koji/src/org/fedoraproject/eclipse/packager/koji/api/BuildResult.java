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
package org.fedoraproject.eclipse.packager.koji.api;

import org.fedoraproject.eclipse.packager.api.ICommandResult;

/**
 * Result of a koji build as triggered by KojiBuildCommand.
 */
public class BuildResult implements ICommandResult {

	/**
	 * Flag if build was successful.
	 */
	private boolean successful;

	/**
	 * Task id of a successful build.
	 */
	private int taskId;

	/**
	 * Set the task ID of the pushed build.
	 *
	 * @param taskId The id of the task in Koji.
	 */
	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	/**
	 * The task id of the build, which has been pushed.
	 *
	 * @return The task id of the pushed build.
	 */
	public int getTaskId() {
		return this.taskId;
	}

	/**
	 * Invoke if build was successful.
	 *
	 * @param success The state - successful or not.
	 */
	public void setSuccessful(boolean success) {
		this.successful = success;
	}

	@Override
	public boolean isSuccessful() {
		return this.successful;
	}

}
