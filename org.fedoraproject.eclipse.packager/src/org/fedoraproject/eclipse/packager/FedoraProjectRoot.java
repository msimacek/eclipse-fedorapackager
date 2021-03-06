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
package org.fedoraproject.eclipse.packager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.Document;
import org.eclipse.linuxtools.rpm.core.utils.RPMQuery;
import org.eclipse.linuxtools.rpm.ui.editor.markers.SpecfileErrorHandler;
import org.eclipse.linuxtools.rpm.ui.editor.parser.Specfile;
import org.eclipse.linuxtools.rpm.ui.editor.parser.SpecfilePackage;
import org.eclipse.linuxtools.rpm.ui.editor.parser.SpecfileParser;

/**
 * This class is representing a root directory for a Fedora package in a given
 * branch. It can be a folder in the cvs case or a project in the git case.
 *
 */
public class FedoraProjectRoot implements IProjectRoot {

	private static final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
	protected IContainer rootContainer;
	private SourcesFile sourcesFile;
	private ILookasideCache lookAsideCache; // The lookaside cache abstraction
	protected IProductStrings productStrings;

	/**
	 * Default no-arg constructor. Required for instance creation via
	 * reflections.
	 */
	public FedoraProjectRoot() {
		// nothing
	}

	@Override
	public void initialize(IContainer container) {
		this.rootContainer = container;

		if (!rootContainer.isSynchronized(IResource.DEPTH_INFINITE)){
			try {
				rootContainer.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				logger.logError(FedoraPackagerText.FedoraProjectRoot_failedToRefreshResource, e);
			}
		}

		this.sourcesFile = new SourcesFile(rootContainer.getFile(new Path(
				SourcesFile.SOURCES_FILENAME)));
		// statically pass Fedora type
		this.lookAsideCache = new LookasideCache();
		this.productStrings = new ProductStringsNonTranslatable();
	}

	@Override
	public IContainer getContainer() {
		return rootContainer;
	}

	@Override
	public IProject getProject() {
		return this.rootContainer.getProject();
	}

	@Override
	public SourcesFile getSourcesFile() {
		return sourcesFile;
	}

	@Override
	public String getPackageName() {
		String str = ""; //$NON-NLS-1$
		try {
			str = RPMQuery.eval(this.getSpecfileModel().getName()).trim();
		} catch (CoreException e) {
			logger.logError(FedoraPackagerText.FedoraPackager_cannotEvalPackageName, e);
		}
		return str;
	}

	@Override
	public IFile getSpecFile() {
		try {
			for (IResource resource : rootContainer.members()) {
				if (resource instanceof IFile) {
					String ext = ((IFile) resource).getFileExtension();
					if (ext != null && ext.equals("spec")) //$NON-NLS-1$
						return (IFile) resource;
				}
			}
		} catch (CoreException e) {
			logger.logError(FedoraPackagerText.FedoraProjectRoot_invalidResource, e);
		}
		return null;
	}

	@Override
	public Specfile getSpecfileModel() {
		SpecfileParser parser = new SpecfileParser();
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					getSpecFile().getContents()));
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n"); //$NON-NLS-1$
			}
		} catch (IOException e) {
			logger.logError(FedoraPackagerText.FedoraProjectRoot_failureReadingFromFile, e);
		} catch (CoreException e) {
			logger.logError(FedoraPackagerText.FedoraProjectRoot_invalidResource, e);
		}
		parser.setErrorHandler(new SpecfileErrorHandler(getSpecFile(), new Document(sb.toString())));
		Specfile specfile = parser.parse(sb.toString());
		return specfile;
	}

	@Override
	public ILookasideCache getLookAsideCache() {
		return lookAsideCache;
	}

	@Override
	public IProductStrings getProductStrings() {
		return this.productStrings;
	}

	@Override
	public QualifiedName[] getSupportedProjectPropertyNames() {
		return new QualifiedName[] { PackagerPlugin.PROJECT_PROP };
	}

	@Override
	public List<String> getPackageNVRs(BranchConfigInstance bci) {
		Specfile specfile = getSpecfileModel();
		String version = specfile.getVersion();
		String release = specfile.getRelease().replace("%{?dist}", bci.getDist());  //$NON-NLS-1$
		List<String> rawNvrs = new ArrayList<>();
		for (SpecfilePackage p: specfile.getPackages().getPackages()) {
			rawNvrs.add(p.getFullPackageName() + "-" + version + "-" + release); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Collections.sort(rawNvrs);
		return rawNvrs;
	}

	/**
	 * A valid project root contains a .spec file and a "sources"
	 * file. The RPM spec-file must be of the form package-name.spec.
     *
	 * (non-Javadoc)
	 * @see org.fedoraproject.eclipse.packager.IProjectRoot#validate(org.eclipse.core.resources.IContainer)
	 */
	@Override
	public boolean validate(IContainer candidate) {
		IFile sourceFile = candidate.getFile(new Path("sources")); //$NON-NLS-1$
		// FIXME: Determine rpm package name from a persistent property. In
		// future the project name might not be equal to the RPM package name.
		IFile specFile = candidate.getFile(new Path(candidate.getProject()
				.getName() + ".spec")); //$NON-NLS-1$
		if (sourceFile.exists() && specFile.exists()) {
			return true;
		}
		return false;
	}

	@Override
	public String getPluginID() {
		return PackagerPlugin.PLUGIN_ID;
	}
}
