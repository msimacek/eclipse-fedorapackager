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
package org.fedoraproject.eclipse.packager.utils;

import java.io.File;
import java.security.GeneralSecurityException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerExtensionPointException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;

/**
 * Utility class for Fedora Packager. Put commonly used code in here as long as
 * it's not RPM related. If it's RPM related, RPMUtils is the better choice.
 */
public class FedoraPackagerUtils {

	private static final String PROJECT_ROOT_EXTENSIONPOINT_NAME = "projectRootProvider"; //$NON-NLS-1$
	private static final String PROJECT_ROOT_ELEMENT_NAME = "projectRoot"; //$NON-NLS-1$
	private static final String PROJECT_ROOT_CLASS_ATTRIBUTE_NAME = "class"; //$NON-NLS-1$
	private static final String VCS_CONTRIBUTION_EXTENSIONPOINT_NAME = "vcsContribution"; //$NON-NLS-1$
	private static final String VCS_CONTRIBUTION_ELEMENT_NAME = "vcs"; //$NON-NLS-1$
	private static final String VCS_CONTRIBUTION_TYPE_ATTRIBUTE_NAME = "type"; //$NON-NLS-1$
	private static final String VCS_CONTRIBUTION_CONTRIB_PLUGIN_ID_ATTRIBUTE_NAME = "contribPlugin"; //$NON-NLS-1$
	private static final String VCS_CONTRIBUTION_CLASS_ATTRIBUTE_NAME = "class"; //$NON-NLS-1$

	/**
	 * Returns a FedoraProjectRoot from the given resource after performing some
	 * validations.
	 * 
	 * @param resource
	 *            The container for this Fedora project root or a resource
	 *            within it.
	 * @throws InvalidProjectRootException
	 *             If the project root does not contain a .spec with the proper
	 *             name or doesn't contain a sources file.
	 * 
	 * @return The retrieved FedoraProjectRoot.
	 */
	public static IProjectRoot getProjectRoot(IResource resource)
			throws InvalidProjectRootException {
		IContainer candidate = null;
		if (resource instanceof IFolder || resource instanceof IProject) {
			candidate = (IContainer) resource;
		} else if (resource instanceof IFile) {
			candidate = resource.getParent();
		}
		if (candidate != null) {
			try {
				// instantiate, but do not initialize yet
				IProjectRoot root = instantiateProjectRoot(candidate);
				// Make sure this root passes its own validation
				if (root.validate(candidate)) {
					// Do initialization
					root.initialize(candidate);
					return root; // All good
				} else {
					throw new InvalidProjectRootException(
							FedoraPackagerText.FedoraPackagerUtils_invalidProjectRootError);
				}
			} catch (FedoraPackagerExtensionPointException e) {
				FedoraPackagerLogger logger = FedoraPackagerLogger
						.getInstance();
				logger.logError(e.getMessage(), e);
				throw new InvalidProjectRootException(e.getMessage());
			}
		} else {
			throw new InvalidProjectRootException(
					FedoraPackagerText.FedoraPackagerUtils_invalidContainerOrProjectType);
		}
	}

	/**
	 * Returns the IFpProjectBits used to abstract vcs specific things.
	 * 
	 * @param fedoraprojectRoot
	 *            The project for which to get the VCS specific parts.
	 * @return The needed IFpProjectBits.
	 */
	public static IFpProjectBits getVcsHandler(IProjectRoot fedoraprojectRoot) {
		QualifiedName propertyName = fedoraprojectRoot
				.getSupportedProjectPropertyNames()[0];
		IExtensionPoint vcsExtensions = Platform.getExtensionRegistry()
				.getExtensionPoint(PackagerPlugin.PLUGIN_ID,
						VCS_CONTRIBUTION_EXTENSIONPOINT_NAME);
		if (vcsExtensions != null) {
			IConfigurationElement[] elements = vcsExtensions
					.getConfigurationElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getName().equals(VCS_CONTRIBUTION_ELEMENT_NAME)
						&& elements[i]
								.getAttribute(VCS_CONTRIBUTION_CONTRIB_PLUGIN_ID_ATTRIBUTE_NAME) != null
						&& elements[i]
								.getAttribute(
										VCS_CONTRIBUTION_CONTRIB_PLUGIN_ID_ATTRIBUTE_NAME)
								.startsWith(propertyName.getQualifier())
						&& elements[i]
								.getAttribute(VCS_CONTRIBUTION_TYPE_ATTRIBUTE_NAME) != null
						&& elements[i].getAttribute(
								VCS_CONTRIBUTION_TYPE_ATTRIBUTE_NAME).equals(
								"GIT")) { //$NON-NLS-1$
					try {
						IConfigurationElement bob = elements[i];
						IFpProjectBits vcsContributor = (IFpProjectBits) bob
								.createExecutableExtension(VCS_CONTRIBUTION_CLASS_ATTRIBUTE_NAME);
						// Do initialization
						if (vcsContributor != null) {
							vcsContributor.initialize(fedoraprojectRoot);
						}
						FedoraPackagerLogger logger = FedoraPackagerLogger
								.getInstance();
						logger.logDebug("Using " + vcsContributor.getClass().getName() + //$NON-NLS-1$
								" as IFpProjectBits"); //$NON-NLS-1$
						return vcsContributor;
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks if <code>candidate</code> is a valid file for uploading. I.e. is
	 * non-empty and has a valid file extension. Valid file extensions are:
	 * <code>'tar', 'gz', 'bz2', 'lzma', 'xz', 'Z', 'zip', 'tff', 'bin',
	 *            'tbz', 'tbz2', 'tlz', 'txz', 'pdf', 'rpm', 'jar', 'war', 'db',
	 *            'cpio', 'jisp', 'egg', 'gem'</code>
	 * 
	 * @param candidate
	 *            The file being examined.
	 * @return <code>true</code> if <code>candidate</code> is a valid file for
	 *         uploading <code>false</code> otherwise.
	 */
	public static boolean isValidUploadFile(File candidate) {
		if (candidate.length() != 0) {
			Pattern extensionPattern = Pattern
					.compile("^.*\\.(?:tar|gz|bz2|lzma|xz|Z|zip|tff|bin|tbz|tbz2|tlz|txz|pdf|rpm|jar|war|db|cpio|jisp|egg|gem)$"); //$NON-NLS-1$
			Matcher extMatcher = extensionPattern.matcher(candidate.getName());
			if (extMatcher.matches()) {
				// file extension seems to be good
				return true;
			}
		}
		return false;
	}

	/**
	 * @return A (probably) unique String.
	 */
	public static String getUniqueIdentifier() {
		// ensure number is not in scientific notation and does not use grouping
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumIntegerDigits(9);
		nf.setGroupingUsed(false);
		// get time stamp for upload folder
		String timestamp = nf
				.format(((double) System.currentTimeMillis()) / 1000);
		// get random String to ensure that uploads that occur in the same
		// millisecond don't collide
		// two simultaneous uploads of the same srpm still have a 1 in 200
		// billion chance of collision
		String randomDifferentiator = ""; //$NON-NLS-1$
		for (int i = 0; i < 8; i++) {
			randomDifferentiator = randomDifferentiator.concat(Character
					.toString((char) (new Random().nextInt('Z' - 'A') + 'A')));
		}
		return timestamp + "." + randomDifferentiator; //$NON-NLS-1$
	}

	/**
	 * This function gets the likely target from the SRPM name.
	 * 
	 * @param srpmName
	 *            The filename of the SRPM.
	 * @return The target build platform for the SRPM.
	 */
	public static String getTargetFromSRPM(String srpmName) {
		String[] splitSRPM = srpmName.split("\\."); //$NON-NLS-1$
		String target = splitSRPM[splitSRPM.length - 3];
		if (target.startsWith("fc")) { //$NON-NLS-1$
			if (Integer.parseInt(target.substring(2)) < 16) {
				return "f" + target.substring(2) + "-candidate"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return "dist-rawhide"; //$NON-NLS-1$
			}
		} else if (target.startsWith("el")) { //$NON-NLS-1$
			if (Integer.parseInt(target.substring(2)) < 6) {
				return "dist-" + target.substring(2) + "E-epel-testing-candidate"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return "dist-rawhide"; //$NON-NLS-1$
			}
		} else {
			return null;
		}
	}

	/**
	 * Wrap a basic HttpClient object in an all trusting SSL enabled HttpClient
	 * object.
	 * 
	 * @param base
	 *            The HttpClient to wrap.
	 * @return The SSL wrapped HttpClient.
	 * @throws GeneralSecurityException
	 *             Function fails for security reasons.
	 */
	public static HttpClient trustAllSslEnable(HttpClient base)
			throws GeneralSecurityException {
		// Get an initialized SSL context
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		// set up the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL"); //$NON-NLS-1$
		sc.init(null, trustAllCerts, new java.security.SecureRandom());

		SSLSocketFactory sf = new SSLSocketFactory(sc,
				SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		ClientConnectionManager ccm = base.getConnectionManager();
		SchemeRegistry sr = ccm.getSchemeRegistry();
		Scheme https = new Scheme("https", 443, sf); //$NON-NLS-1$
		sr.register(https);
		return new DefaultHttpClient(ccm, base.getParams());
	}

	/**
	 * Get a list of all Fedora Git projects. I.e. all projects in the current
	 * workspace which have the Fedora Packager for Eclipse persistent property.
	 * 
	 * @return All projects in the current workspace with the
	 *         {@link PackagerPlugin#PROJECT_PROP} property.
	 */
	public static IProject[] getAllFedoraGitProjects() {
		IProject[] wsProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IProject> projectsSet = new ArrayList<IProject>();
		for (IProject project : wsProjects) {
			String property = null;
			try {
				property = project
						.getPersistentProperty(PackagerPlugin.PROJECT_PROP);
			} catch (CoreException e) {
				// ignore
			}
			if (property != null) {
				projectsSet.add(project);
			}
		}
		return projectsSet.toArray(new IProject[] {});
	}

	/**
	 * Instatiate a project root instance using the projectRoot extension point.
	 * 
	 * @param container
	 *            The container to act as the root for the project.
	 * 
	 * @return the newly created instance
	 * @throws FedoraPackagerExtensionPointException
	 *             If project root providing plugin could not be found.
	 */
	private static IProjectRoot instantiateProjectRoot(IContainer container)
			throws FedoraPackagerExtensionPointException {
		IExtensionPoint projectRootExtension = Platform.getExtensionRegistry()
				.getExtensionPoint(PackagerPlugin.PLUGIN_ID,
						PROJECT_ROOT_EXTENSIONPOINT_NAME);
		if (projectRootExtension != null) {
			List<IProjectRoot> projectRootList = new ArrayList<IProjectRoot>();
			for (IConfigurationElement projectRoot : projectRootExtension
					.getConfigurationElements()) {
				if (projectRoot.getName().equals(PROJECT_ROOT_ELEMENT_NAME)) {
					// found extension point element
					try {
						IProjectRoot root = (IProjectRoot) projectRoot
								.createExecutableExtension(PROJECT_ROOT_CLASS_ATTRIBUTE_NAME);
						assert root != null;
						projectRootList.add(root);
					} catch (CoreException e) {
						throw new FedoraPackagerExtensionPointException(
								e.getMessage(), e);
					}
				}
			}
			// We need at least one project root
			if (projectRootList.size() == 0) {
				throw new FedoraPackagerExtensionPointException(NLS.bind(
						FedoraPackagerText.extensionNotFoundError,
						PROJECT_ROOT_EXTENSIONPOINT_NAME));
			}
			// Get the best matching project root
			IProjectRoot projectRoot = findBestMatchingProjectRoot(
					projectRootList, container);
			if (projectRoot == null) {
				// can't continue
				throw new FedoraPackagerExtensionPointException(NLS.bind(
						FedoraPackagerText.extensionNotFoundError,
						PROJECT_ROOT_EXTENSIONPOINT_NAME));
			}
			return projectRoot;
		}
		throw new FedoraPackagerExtensionPointException(NLS.bind(
				FedoraPackagerText.extensionNotFoundError,
				PROJECT_ROOT_EXTENSIONPOINT_NAME));
	}

	/**
	 * Determine the project root, which is the best match for the given
	 * container.
	 * 
	 * @param projectRootList The list of known roots.
	 * @param container The container the root is being found in respect to.
	 * @return The project root which has support for the project property of
	 *         the container or {@code null} if no such project root exists.
	 */
	private static IProjectRoot findBestMatchingProjectRoot(
			List<IProjectRoot> projectRootList, IContainer container) {
		for (IProjectRoot root : projectRootList) {
			for (QualifiedName propName : root
					.getSupportedProjectPropertyNames()) {
				try {
					String property = container.getProject()
							.getPersistentProperty(propName);
					if (property != null) {
						// match found
						FedoraPackagerLogger logger = FedoraPackagerLogger
								.getInstance();
						logger.logDebug(NLS
								.bind(FedoraPackagerText.FedoraPackagerUtils_projectRootClassNameMsg,
										root.getClass().getName()));
						return root;
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
