package org.fedoraproject.eclipse.packager.koji.internal.ui;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.fedoraproject.eclipse.packager.koji.KojiText;

/**
 * Dialog prompting user for a build target.
 *
 */
public class KojiTargetDialog extends TitleAreaDialog {

	private Text targetText;
	private List targetList;
	private Collection<String> targets;
	private String returnTarget;
	private boolean sort;

	/**
	 * @param parent
	 *            The dialog's parent shell.
	 * @param targets
	 *            The list of known targets.
	 */
	public KojiTargetDialog(Shell parent, Collection<String> targets) {
		super(parent);
		this.targets = targets;
		this.sort = true;
	}

	/**
	 * Use this constructor if you have a list of targets that is already
	 * ordered in the way it is intended to be displayed.
	 *
	 * @param parent
	 *            The dialog's parent shell.
	 * @param targets
	 *            The list of targets
	 * @param sort
	 *            Whether or not to sort the given list before displaying it.
	 */
	public KojiTargetDialog(Shell parent, Collection<String> targets,
			boolean sort) {
		this(parent, targets);
		this.sort = sort;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		targetText = new Text(parent, SWT.SINGLE | SWT.LEFT);
		targetList = new List(parent, SWT.SINGLE | SWT.V_SCROLL);
		GridData listData = new GridData(SWT.FILL, SWT.BEGINNING, true,
				false);
		listData.heightHint = 100;
		targetList.setLayoutData(listData);
		targetText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		targetText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (targetList.getSelectionCount() > 0
						&& !targetList.getSelection()[0]
								.contentEquals(targetText.getText())) {
					targetList.deselectAll();
				}
				if (targetText.getText().trim().isEmpty()) {
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				} else {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
				returnTarget = targetText.getText();
			}
		});
		targetList.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (targetList.getSelectionCount() > 0) {
					targetText.setText(targetList.getSelection()[0]);
					returnTarget = targetText.getText();
				}
			}
		});
		String[] listItems = targets.toArray(new String[0]);

		if (this.sort){
			Arrays.sort(listItems);
		}

		targetList.setItems(listItems);
		targetText.pack();
		return parent;
	}

	/**
	 * Create and open the dialog, returning the chosen target. If there user
	 * hits cancel instead of OK return null.
	 *
	 * @return The chosen target.
	 */
	public String openForTarget() {
		create();
		setTitle(KojiText.KojiTargetDialog_DialogTitle);
		if (open() == OK) {
			return returnTarget;
		}
		return null;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control returnControl = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return returnControl;
	}

}
