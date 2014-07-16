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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmEvalCommandException;

/**
 * Call out to rpm in order to evaluate some value.
 */
public class RpmEvalCommand extends FedoraPackagerCommand<EvalResult> {

	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "RpmEvalCommand"; //$NON-NLS-1$
	/**
	 * Variable for architecture eval.
	 */
	public static final String ARCH = "%{_arch}"; //$NON-NLS-1$

	private static final String RPM_CMD = "rpm"; //$NON-NLS-1$
	private static final String EVAL_OPTION = "--eval"; //$NON-NLS-1$
	private List<String> command;

	private String variable;

	@Override
	protected void checkConfiguration() throws CommandMisconfiguredException {
		if (variable == null) {
			throw new CommandMisconfiguredException(
					RpmText.RpmEvalCommand_variableMustBeSet);
		}
	}

	/**
	 * Set the variable, which should get evaluated.
	 * 
	 * @param var
	 *            The RPM variable being evaluated.
	 * @return This instance.
	 */
	public RpmEvalCommand variable(String var) {
		variable = var;
		return this;
	}

	/**
	 * Call the RPM eval.
	 * 
	 * @throws CommandListenerException
	 *             If some listener failed.
	 * @throws RpmEvalCommandException
	 *             If this command failed.
	 */
	@Override
	public EvalResult call(IProgressMonitor monitor)
			throws CommandListenerException,
			RpmEvalCommandException {
		try {
			callPreExecListeners();
		} catch (CommandListenerException e) {
			throw e;
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		String[] cmdArray = getCmdArray();
		EvalResult result;
		try {
			result = new EvalResult(cmdArray,
					Utils.runCommandToString(cmdArray));
		} catch (IOException e) {
			throw new RpmEvalCommandException(e);
		}
		callPostExecListeners();
		setCallable(false); // reuse of instance's call() not allowed
		monitor.done();
		return result;
	}

	/**
	 * @param fp
	 *            The project root the command is being run under.
	 * @throws FedoraPackagerCommandInitializationException
	 *             If project root is null.
	 */
	@Override
	public void initialize(IProjectRoot fp)
			throws FedoraPackagerCommandInitializationException {
		super.initialize(fp);
		this.command = new ArrayList<>();
		this.command.add(RPM_CMD);
		this.command.add(EVAL_OPTION);
	}

	/**
	 * 
	 * @return The converted command list.
	 */
	private String[] getCmdArray() {
		assert this.variable != null;
		this.command.add(variable);
		return this.command.toArray(new String[0]);
	}
}
