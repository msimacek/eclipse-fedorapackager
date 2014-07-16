/*******************************************************************************
 * Copyright (c) 2011-2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

/**
 * This class is representing a root directory for a Local Fedora RPM package in a given
 * branch. This project is a local Git repository.
 * This class is a local version of org.fedoraproject.eclipse.packager.FedoraProjectRoot
 * 
 */
public class LocalFedoraPackagerProjectRoot extends FedoraProjectRoot {
	
	/**
	 * Default no-arg constructor. Required for instance creation via
	 * reflections.
	 */
	public LocalFedoraPackagerProjectRoot() {
		// nothing
	}
	
	@Override
	public void initialize(IContainer container) {
		this.rootContainer = container;
		this.productStrings = new ProductStringsNonTranslatable();
	}

	@Override
	public SourcesFile getSourcesFile() {
		return null;
	}
	
	@Override
	public ILookasideCache getLookAsideCache() {
		return null;
	}
	
	@Override
	public QualifiedName[] getSupportedProjectPropertyNames() {
		return new QualifiedName[] { PackagerPlugin.PROJECT_LOCAL_PROP };
	}

	@Override
	public boolean validate(IContainer candidate) {
		// For a local Fedora project we only require a .spec file. 
		// That .spec file has to have the format <ProjectName>.spec.
		IFile specFile = candidate.getFile(new Path(candidate.getProject()
				.getName() + ".spec")); //$NON-NLS-1$
		if (specFile.exists()) {
			return true;
		}
		return false;
	}

}
