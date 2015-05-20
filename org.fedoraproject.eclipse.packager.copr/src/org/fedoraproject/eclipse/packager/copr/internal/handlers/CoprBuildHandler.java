package org.fedoraproject.eclipse.packager.copr.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.fedoraproject.copr.client.BuildRequest;
import org.fedoraproject.copr.client.CoprException;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.ScpJob;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.copr.CoprPlugin;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Handler for executing build in Copr
 *
 * @author msimacek
 *
 */
public class CoprBuildHandler extends CoprHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		prepareProject(event);
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		final IProjectRoot projectRoot = getProjectRoot(event);
		if (projectRoot == null)
			return null;

		IPath srpmPath = null;
		if (eventResource instanceof IFile
				&& eventResource.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
			srpmPath = eventResource.getLocation();
		}
		if (srpmPath == null) {
			try {
				srpmPath = FedoraHandlerUtils.chooseRootFileOfType(shell,
						projectRoot, ".src.rpm", //$NON-NLS-1$
						"Choose SRPM to be build in Copr");
			} catch (OperationCanceledException e) {
				return null;
			} catch (CoreException e) {
				logger.logError(e.getMessage(), e);
				return new Status(IStatus.ERROR, CoprPlugin.PLUGIN_ID,
						e.getMessage(), e);
			}
		}
		if (srpmPath == null) {
			// FIXME this is temporary, it should build the SRPM
			FedoraHandlerUtils.showErrorDialog(shell, "No SRPM",
					"Copr build requires a SRPM to exist");
			return null;
		}

		FedoraPackager packager = new FedoraPackager(projectRoot);
		try {
			ScpCommand scpCommand = (ScpCommand) packager
					.getCommandInstance(ScpCommand.ID);
			// TODO this is unnecessarily uploading the specfile
			ScpJob job = new ScpJob("Uploading SRPM", projectRoot,
					srpmPath.toString(), scpCommand);

			job.setUser(true);
			job.schedule();
			job.join();
			// TODO check?
		} catch (FedoraPackagerAPIException e) {
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, CoprPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}

		String fasAccount = new FedoraSSL().getUsernameFromCert();
		// TODO hardcoded constant
		String remotePath = "http://" + fasAccount + ".fedorapeople.com/"
				+ srpmPath.toFile().getName();

		try {
			BuildRequest buildRequest = new BuildRequest(
					coprProject.getUserName(), coprProject.getProjectName());
			buildRequest.addSourceRpm(remotePath);
			coprSession.build(buildRequest);
		} catch (CoprException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, "Copr Error",
					e.getMessage());
			return new Status(IStatus.ERROR, CoprPlugin.PLUGIN_ID,
					e.getMessage(), e);
		}
		return null;
	}

}
