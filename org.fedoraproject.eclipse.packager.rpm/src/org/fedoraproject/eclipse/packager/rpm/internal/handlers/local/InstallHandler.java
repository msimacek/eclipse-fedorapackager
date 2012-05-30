/*******************************************************************************
 * Copyright (c) 2010-2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.rpm.internal.handlers.local;

import org.eclipse.core.commands.AbstractHandler;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;

/**
 * Handler for RPM install.
 * 
 */
public class InstallHandler extends LocalBuildHandler {

	@Override
	protected BuildType getBuildType() {
		return BuildType.INSTALL;
	}
	
	@Override
	protected String getTaskName() {
		return RpmText.InstallHandler_taskName;
	}
	
	@Override
	protected AbstractHandler getDispatchee() {
		return new org.fedoraproject.eclipse.packager.rpm.internal.handlers.LocalBuildHandler(
				getBuildType(), getTaskName());
	}

}
