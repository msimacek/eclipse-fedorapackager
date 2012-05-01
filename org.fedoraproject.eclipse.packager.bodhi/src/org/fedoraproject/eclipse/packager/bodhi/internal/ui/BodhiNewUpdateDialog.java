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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.fedoraproject.eclipse.packager.bodhi.BodhiText;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand.RequestType;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand.UpdateType;

/**
 * UI dialog corresponding to the Web form of:
 * https://admin.fedoraproject.org/updates/new/
 */
public class BodhiNewUpdateDialog extends Dialog {

	protected Label lblError;
	private Text txtComment;
	private Text txtBugs;
	private Text txtStableKarmaThreshold;
	private Text txtUnstableKarmaThreshold;
	private Button btnEnableKarmaAutomatism;
	private Button btnSuggestReboot;
	private ComboViewer comboType;
	private ComboViewer comboRequest;
	private ListViewer listBuilds;
	private Button btnCloseBugs;
	// Data fields in order to be able to pre-fill some fields
	private String[] buildsData;
	private String bugsData;
	private String commentData;
	private RequestType requestTypeData;
	private UpdateType updateTypeData;
	private boolean suggestRebootData;
	private boolean closeBugsData;
	private boolean enableKarmaAutomatismData;
	private int stableKarmaThresholdData;
	private int unstableKarmaThresholdData;
	private String[] selectedBuild;

	/**
	 * Create the dialog and pre-fill it with some data
	 * 
	 * @param parent
	 *            The parent shell
	 * @param builds
	 *            The initial list of builds
	 * @param selectedBuild
	 *            The item of the builds list which should be selected when the
	 *            dialog comes up
	 * @param bugs
	 *            The comma/space separated list of builds
	 * @param comment
	 *            The update comment (i.e. notice)
	 */
	public BodhiNewUpdateDialog(Shell parent, String[] builds,
			String[] selectedBuild, String bugs, String comment) {
		super(parent);
		this.buildsData = builds;
		this.bugsData = bugs;
		this.commentData = comment;
		this.selectedBuild = selectedBuild;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(BodhiText.BodhiNewUpdateDialog_createNewUpdateTitle);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			if (!validateForm()) {
				return;
			}
		}
		super.buttonPressed(buttonId);
	}
	
	/**
	 * @return The list of builds, which were selected.
	 */
	public String[] getBuilds() {
		// since we require at least one build to be selected,
		// this should always return a non-empty list
		return this.buildsData;
	}

	private void setBuilds() {
		this.buildsData = listBuilds.getList().getSelection();
	}

	/**
	 * @return A comma separated list of bugs, et. al.
	 */
	public String getBugs() {
		// since we do validation on this field it should contain
		// a reasonable string at this point.
		return this.bugsData;
	}

	private void setBugs() {
		this.bugsData = txtBugs.getText();
	}

	/**
	 * 
	 * @return The update comment/note.
	 */
	public String getComment() {
		return this.commentData;
	}

	private void setComment() {
		this.commentData = txtComment.getText();
	}

	/**
	 * 
	 * @return The suggest reboot selection.
	 */
	public boolean isSuggestReboot() {
		return this.suggestRebootData;
	}

	private void setSuggestReboot() {
		this.suggestRebootData = btnSuggestReboot.getSelection();
	}

	/**
	 * 
	 * @return The close bugs selection.
	 */
	public boolean isCloseBugs() {
		return this.closeBugsData;
	}

	private void setCloseBugs() {
		this.closeBugsData = btnCloseBugs.getSelection();
	}

	/**
	 * 
	 * @return The karma automatism selection.
	 */
	public boolean isKarmaAutomatismEnabled() {
		return this.enableKarmaAutomatismData;
	}

	private void setKarmaAutomatismEnabled() {
		this.enableKarmaAutomatismData = btnEnableKarmaAutomatism
				.getSelection();
	}

	/**
	 * 
	 * @return The selected request type.
	 */
	public RequestType getRequestType() {
		return this.requestTypeData;
	}

	private void setRequestType() {
		this.requestTypeData = RequestType.valueOf(comboRequest.getCombo().getItem(
				comboRequest.getCombo().getSelectionIndex()));
	}

	/**
	 * 
	 * @return The selected update type.
	 */
	public UpdateType getUpdateType() {
		return this.updateTypeData;
	}

	private void setUpdateType() {
		this.updateTypeData = UpdateType.valueOf(comboType.getCombo().getItem(
				comboType.getCombo().getSelectionIndex()));
	}

	/**
	 * 
	 * @return The entered stable karma threshold.
	 */
	public int getStableKarmaThreshold() {
		return this.stableKarmaThresholdData;
	}

	private void setStableKarmaThreshold() {
		// since we attempted to parse an integer from
		// the string while validating it, this should
		// not throw a NumberFormatException
		this.stableKarmaThresholdData = Integer
				.parseInt(txtStableKarmaThreshold.getText());
	}

	/**
	 * 
	 * @return The entered unstable karma threshold.
	 */
	public int getUnstableKarmaThreshold() {
		return this.unstableKarmaThresholdData;
	}

	private void setUnstableKarmaThreshold() {
		// since we attempted to parse an integer from
		// the string while validating it, this should
		// not throw a NumberFormatException
		this.unstableKarmaThresholdData = Integer
				.parseInt(txtUnstableKarmaThreshold.getText());
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super
				.createDialogArea(parent);
		composite.setLayout(new RowLayout());

		Composite labelComposite = new Composite(composite, SWT.NONE);
		labelComposite.setBounds(10, 10, 206, 601);
		labelComposite.setLayout(null);

		Label lblPackage = new Label(labelComposite, SWT.RIGHT);
		lblPackage.setBounds(131, 40, 70, 21);
		lblPackage.setText(BodhiText.BodhiNewUpdateDialog_packageLbl);

		Label lblType = new Label(labelComposite, SWT.RIGHT);
		lblType.setBounds(131, 122, 70, 21);
		lblType.setText(BodhiText.BodhiNewUpdateDialog_typeLbl);

		Label lblRequest = new Label(labelComposite, SWT.RIGHT);
		lblRequest.setBounds(131, 159, 70, 21);
		lblRequest.setText(BodhiText.BodhiNewUpdateDialog_requestTypeLbl);

		Label lblBugs = new Label(labelComposite, SWT.RIGHT);
		lblBugs.setBounds(131, 199, 70, 21);
		lblBugs.setText(BodhiText.BodhiNewUpdateDialog_bugsLbl);

		Label lblNotes = new Label(labelComposite, SWT.SHADOW_NONE | SWT.RIGHT);
		lblNotes.setBounds(131, 243, 70, 21);
		lblNotes.setText(BodhiText.BodhiNewUpdateDialog_notesLbl);

		Label lblStableKarma = new Label(labelComposite, SWT.WRAP | SWT.RIGHT);
		lblStableKarma.setBounds(57, 462, 149, 44);
		lblStableKarma
				.setText(BodhiText.BodhiNewUpdateDialog_stableKarmaThresholdLbl);

		Label lblUnstableKarma = new Label(labelComposite, SWT.WRAP | SWT.RIGHT);
		lblUnstableKarma.setBounds(113, 513, 93, 41);
		lblUnstableKarma
				.setText(BodhiText.BodhiNewUpdateDialog_unstableKarmaThresholdLbl);

		Composite valuesComposite = new Composite(composite, SWT.NONE);
		valuesComposite.setBounds(222, 10, 459, 601);
		valuesComposite.setLayout(null);

		btnEnableKarmaAutomatism = new Button(valuesComposite, SWT.CHECK);
		btnEnableKarmaAutomatism
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_enableKarmaAutomatismTooltip);
		btnEnableKarmaAutomatism
				.setSelection(PushUpdateCommand.DEFAULT_KARMA_AUTOMATISM);
		btnEnableKarmaAutomatism.setBounds(7, 432, 196, 25);
		btnEnableKarmaAutomatism
				.setText(BodhiText.BodhiNewUpdateDialog_enableKarmaAutomatismLbl);
		btnSuggestReboot = new Button(valuesComposite, SWT.CHECK);
		btnSuggestReboot
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_suggestRebootTooltip);
		btnSuggestReboot.setBounds(7, 403, 166, 25);
		btnSuggestReboot.setSelection(PushUpdateCommand.DEFAULT_SUGGEST_REBOOT);
		btnSuggestReboot
				.setText(BodhiText.BodhiNewUpdateDialog_suggestRebootLbl);

		txtComment = new Text(valuesComposite, SWT.WRAP | SWT.V_SCROLL
				| SWT.MULTI|SWT.BORDER);
		txtComment.setText(this.commentData);
		final HtmlTooltip tooltip = new HtmlTooltip(txtComment,
				BodhiText.BodhiNewUpdateDialog_notesHtmlTooltipTxt, 330, 270);
		txtComment.addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseHover(MouseEvent e) {
				tooltip.show(new Point(e.x + 10, e.y + 10));
			}
		});
		txtComment.setBounds(10, 243, 439, 152);

		txtBugs = new Text(valuesComposite, SWT.SINGLE|SWT.BORDER);
		txtBugs.setToolTipText(BodhiText.BodhiNewUpdateDialog_bugsTooltip);
		txtBugs.setText(this.bugsData);
		txtBugs.setBounds(10, 197, 193, 31);

		comboType = new ComboViewer(valuesComposite, SWT.READ_ONLY);
		comboType.setContentProvider(ArrayContentProvider.getInstance());
		comboType.setInput(UpdateType.values());
		comboType.getCombo().setBounds(9, 116, 196, 33);
		comboType.setSelection(new StructuredSelection(UpdateType.BUGFIX));

		comboRequest = new ComboViewer(valuesComposite, SWT.READ_ONLY);
		comboRequest.setContentProvider(ArrayContentProvider.getInstance());
		comboRequest.setInput(RequestType.values());
		comboRequest.getCombo().setBounds(9, 155, 196, 33);

		txtStableKarmaThreshold = new Text(valuesComposite, SWT.SINGLE|SWT.BORDER);
		txtStableKarmaThreshold
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_stableKarmaTooltip);
		txtStableKarmaThreshold.setText(String
				.valueOf(PushUpdateCommand.DEFAULT_STABLE_KARMA_THRESHOLD));
		txtStableKarmaThreshold.setBounds(10, 470, 40, 31);

		txtUnstableKarmaThreshold = new Text(valuesComposite, SWT.SINGLE|SWT.BORDER);
		txtUnstableKarmaThreshold
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_unstableKarmaTooltip);
		txtUnstableKarmaThreshold.setText(String
				.valueOf(PushUpdateCommand.DEFAULT_UNSTABLE_KARMA_THRESHOLD));
		txtUnstableKarmaThreshold.setBounds(10, 522, 40, 31);

		listBuilds = new ListViewer(valuesComposite, SWT.BORDER | SWT.V_SCROLL
				| SWT.MULTI);
		listBuilds.setContentProvider(ArrayContentProvider.getInstance());
		listBuilds.getList().setToolTipText(BodhiText.BodhiNewUpdateDialog_buildsTooltip);
		listBuilds.setInput(this.buildsData);
		listBuilds.getList().setBounds(10, 40, 439, 70);
		listBuilds.setSelection(new StructuredSelection(selectedBuild));

		Button btnAddBuild = new Button(valuesComposite, SWT.NONE);
		btnAddBuild.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR) {
					AddNewBuildDialog newBuildDialog = new AddNewBuildDialog(
							composite.getShell());
					if (newBuildDialog.open() == Window.OK) {
						listBuilds.add(newBuildDialog.getBuilds());
					}
				}
			}

		});
		btnAddBuild.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent e) {
				AddNewBuildDialog newBuildDialog = new AddNewBuildDialog(
						composite.getShell());
				if (newBuildDialog.open() == Window.OK) {
					listBuilds.add(newBuildDialog.getBuilds());
				}
			}

		});
		btnAddBuild
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_addBuildsBtnTooltip);
		btnAddBuild.setBounds(375, 116, 74, 33);
		btnAddBuild.setText(BodhiText.BodhiNewUpdateDialog_addBuildsBtn);

		btnCloseBugs = new Button(valuesComposite, SWT.CHECK|SWT.BORDER);
		btnCloseBugs
				.setToolTipText(BodhiText.BodhiNewUpdateDialog_closeBugsTooltip);
		btnCloseBugs.setSelection(true);
		btnCloseBugs.setBounds(209, 200, 242, 25);
		btnCloseBugs.setText(BodhiText.BodhiNewUpdateDialog_closeBugsBtn);

		lblError = new Label(valuesComposite, SWT.NONE);
		lblError.setForeground(getColor(SWT.COLOR_RED));
		lblError.setBounds(10, 10, 439, 21);
		// set the tab order when tabbing through controls using the
		// keyboard
		valuesComposite.setTabList(new Control[] { btnAddBuild,
				txtBugs, btnCloseBugs, txtComment,
				btnSuggestReboot, btnEnableKarmaAutomatism,
				txtStableKarmaThreshold, txtUnstableKarmaThreshold });
		return composite;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.bodhi.internal.ui.AbstractBodhiDialog
	 * #validateForm()
	 */
	protected boolean validateForm() {
		String bugs = txtBugs.getText();
		// need to have at least one build selected
		if (listBuilds.getSelection().isEmpty()) {
			setValidationError(BodhiText.BodhiNewUpdateDialog_buildsSelectionErrorMsg);
			return false;
		}
		if (!bugs.equals("") && !bugs.matches("[0-9]+(,[0-9]+)*")) { //$NON-NLS-1$ //$NON-NLS-2$
			setValidationError(BodhiText.BodhiNewUpdateDialog_invalidBugsErrorMsg);
			return false;
		}
		// require stable karma to be a reasonable number
		try {
			Integer.parseInt(txtStableKarmaThreshold.getText());
		} catch (NumberFormatException e) {
			setValidationError(BodhiText.BodhiNewUpdateDialog_invalidStableKarmaErrorMsg);
			return false;
		}
		// require unstable karma to be a reasonable number
		try {
			Integer.parseInt(txtUnstableKarmaThreshold.getText());
		} catch (NumberFormatException e) {
			setValidationError(BodhiText.BodhiNewUpdateDialog_invalidUnstableKarmaMsg);
			return false;
		}
		// requestType needs to be set
		if (comboRequest.getCombo().getSelectionIndex() < 0) {
			setValidationError(BodhiText.BodhiNewUpdateDialog_invalidRequestTypeErrorMsg);
			return false;
		}
		// Update notice must not be empty
		if (txtComment.getText().trim().equals("")) { //$NON-NLS-1$
			setValidationError(BodhiText.BodhiNewUpdateDialog_invalidNotesErrorMsg);
			return false;
		}
		// values seem to be good, set data fields
		setDataFields();
		return true;
	}

	/**
	 * Set all data fields from UI widgets.
	 * 
	 * pre: validation passed post: public getters are functional
	 */
	private void setDataFields() {
		setBugs();
		setBuilds();
		setCloseBugs();
		setComment();
		setKarmaAutomatismEnabled();
		setStableKarmaThreshold();
		setSuggestReboot();
		setUnstableKarmaThreshold();
		setRequestType();
		setUpdateType();
	}

	/**
	 * Provide an inline error message feedback if some field is invalid.
	 */
	protected void setValidationError(String error) {
		this.lblError.setText(error);
		this.lblError.setForeground(getColor(SWT.COLOR_RED));
		this.lblError.redraw();
	}

	protected Color getColor(int systemColorID) {
		Display display = Display.getCurrent();
		return display.getSystemColor(systemColorID);
	}

}
