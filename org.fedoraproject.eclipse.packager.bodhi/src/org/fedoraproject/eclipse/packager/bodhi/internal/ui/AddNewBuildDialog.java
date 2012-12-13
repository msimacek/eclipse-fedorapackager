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
package org.fedoraproject.eclipse.packager.bodhi.internal.ui;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.bodhi.BodhiText;

/**
 * Dialog to add additional builds to the fixed list for an update.
 * 
 */
public class AddNewBuildDialog extends InputDialog {

	private static final String BUILDS_REGEX = "^(?:([^, ]+)[, ]?)+$"; //$NON-NLS-1$

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 *            The shell the dialog is to be opened in.
	 */
	public AddNewBuildDialog(Shell parent) {
		super(parent, BodhiText.AddNewBuildDialog_addAnotherBuild,
				BodhiText.AddNewBuildDialog_packageBuildsLbl, "",
				new IInputValidator() {

					@Override
					public String isValid(String newText) {
						Pattern pattern = Pattern.compile(BUILDS_REGEX);
						Matcher matcher = pattern.matcher(newText);
						if (!matcher.matches()) {
							return BodhiText.AddNewBuildDialog_buildsFormatErrorMsg;
						}
						return null;
					}
				});
	}

	/**
	 * @return The list of builds to add or {@code null}.
	 */
	public String[] getBuilds() {
		Pattern pattern = Pattern.compile(BUILDS_REGEX);
		Matcher matcher = pattern.matcher(getValue());
		ArrayList<String> builds = new ArrayList<>();
		if (matcher.matches()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				for (String item : matcher.group(i).split("[, ]")) { //$NON-NLS-1$
					builds.add(item);
				}
			}
		}
		return builds.toArray(new String[] {});
	}

}
