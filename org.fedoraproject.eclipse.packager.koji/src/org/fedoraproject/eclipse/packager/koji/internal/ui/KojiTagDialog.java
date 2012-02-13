package org.fedoraproject.eclipse.packager.koji.internal.ui;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.fedoraproject.eclipse.packager.koji.KojiText;

/**
 * Dialog prompting user for a build tag.
 * 
 */
public class KojiTagDialog extends TitleAreaDialog {

	private Text tagText;
	private List tagList;
	private Collection<String> tags;
	private String returnTag;

	/**
	 * @param parent
	 *            The dialog's parent shell.
	 * @param tags
	 *            The list of known tags.
	 */
	public KojiTagDialog(Shell parent, Collection<String> tags) {
		super(parent);
		this.tags = tags;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		tagText = new Text(parent, SWT.SINGLE | SWT.LEFT);
		tagList = new List(parent, SWT.SINGLE | SWT.V_SCROLL);
		GridData listData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		listData.heightHint = 100;
		tagList.setLayoutData(listData);
		tagText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tagText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (tagList.getSelectionCount() > 0
						&& !tagList.getSelection()[0].contentEquals(tagText
								.getText())) {
					tagList.deselectAll();
				}
				if (tagText.getText().trim().contentEquals("")) { //$NON-NLS-1$
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				} else {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
				returnTag = tagText.getText();
			}
		});
		tagList.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (tagList.getSelectionCount() > 0) {
					tagText.setText(tagList.getSelection()[0]);
					returnTag = tagText.getText();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}

		});
		String[] listItems = tags.toArray(new String[0]);
		Arrays.sort(listItems);
		tagList.setItems(listItems);
		tagText.pack();
		tagList.pack();
		return parent;
	}
	
	/**
	 * Create and open the dialog, returning the chosen tag.
	 * @return The chosen tag.
	 */
	public String openForTag(){
		create();
		setTitle(KojiText.KojiTagDialog_DialogTitle);
		if (open() == OK){
			return returnTag;
		}
		return null;
	}
	
	@Override
	protected void constrainShellSize(){
		super.constrainShellSize();
		getShell().setSize(300, 300);
	}
	
	@Override
	protected Control createButtonBar(Composite parent){
		Control returnControl = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return returnControl;
	}

}
