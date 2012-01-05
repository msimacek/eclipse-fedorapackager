package org.fedoraproject.eclipse.packager.koji.internal.ui;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog prompting the user for creating a Koji server instance.
 * 
 */
public class KojiServerDialog extends Dialog {

	private final String[] TITLES = { "Name", "Web URL", "XMLRPC URL" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private String[] serverInfo;
	private int result;
	private String title;
	private Shell shell;
	private Button okButton;
	private Button cancelButton;
	private Text[] texts;
	private Label[] labels;
	private String[] returnValue;

	/**
	 * @param parent
	 *            The parent shell for the dialog.
	 * @param serverInfo
	 *            Previous server info for editing an instance or null for
	 *            adding an instance.
	 * @param title
	 *            The title of the Dialog.
	 */
	public KojiServerDialog(Shell parent, String[] serverInfo, String title) {
		super(parent);
		this.serverInfo = serverInfo;
		this.title = title;
	}

	/**
	 * Open the dialog.
	 * 
	 * @return The server information.
	 */
	public String[] open() {
		shell = new Shell(getParent(), SWT.MIN | SWT.BORDER);
		shell.setText(title);
		shell.setSize(550, 200);
		shell.setLayout(new GridLayout(1, false));
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setSize(550, 200);
		composite.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		labels = new Label[3];
		texts = new Text[3];
		GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
		textData.heightHint = 25;
		textData.widthHint = 450;
		for (int i = 0; i < 3; i++) {
			labels[i] = new Label(composite, SWT.LEFT);
			labels[i].setText(TITLES[i]);
			texts[i] = new Text(composite, SWT.LEFT | SWT.SINGLE | SWT.BORDER
					| SWT.WRAP);
			if (serverInfo != null && serverInfo.length > i
					&& serverInfo[i] != null) {
				texts[i].setText(serverInfo[i]);
			}
			texts[i].addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					checkOk();
				}
			});
			labels[i].pack();
			texts[i].setLayoutData(textData);
			texts[i].pack();
		}
		okButton = new Button(composite, SWT.DEFAULT);
		okButton.setText("OK"); //$NON-NLS-1$
		okButton.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// no op
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// no op
			}

			@Override
			public void mouseUp(MouseEvent e) {
				result = Window.OK;
				returnValue = new String[] { texts[0].getText(),
						texts[1].getText(), texts[2].getText() };
				shell.close();
			}
		});
		checkOk();
		cancelButton = new Button(composite, SWT.DEFAULT);
		cancelButton.setText("Cancel"); //$NON-NLS-1$
		cancelButton.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// no op
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// no op
			}

			@Override
			public void mouseUp(MouseEvent e) {
				result = Window.CANCEL;
				shell.close();
			}
		});
		shell.open();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		if (result == Window.CANCEL) {
			return null;
		} else {
			return returnValue;
		}
	}
	
	private void checkOk(){
		okButton.setEnabled(true);
		for (Text text : texts) {
			String contents = text.getText();
			if (contents == null || contents.contains(";") //$NON-NLS-1$
					|| contents.contains(",")) { //$NON-NLS-1$
				okButton.setEnabled(false);
				break;
			}
		}
		if (okButton.getEnabled()) {
			if (texts[0].getText().trim().contentEquals("") //$NON-NLS-1$
					|| !texts[1].getText().startsWith("http") //$NON-NLS-1$
					|| !texts[2].getText().startsWith("http")) { //$NON-NLS-1$
				okButton.setEnabled(false);
			}
		}
	}
}