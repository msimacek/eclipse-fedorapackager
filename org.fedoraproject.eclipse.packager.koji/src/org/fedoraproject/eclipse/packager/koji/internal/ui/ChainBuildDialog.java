package org.fedoraproject.eclipse.packager.koji.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Dialog for getting information required for a chain build.
 * 
 */
public class ChainBuildDialog extends Dialog {

	/**
	 * Default constructor.
	 * 
	 * @param parent
	 *            Parent shell to the shell the dialog will be launched in.
	 */
	public ChainBuildDialog(Shell parent) {
		super(parent);
	}

	private Table projectTable;
	private Tree buildTree;
	private Shell shell;
	private int result = Window.CANCEL;
	private List<List<String>> buildInfo = null;
	private List<IProjectRoot> rootList = new ArrayList<IProjectRoot>();

	/**
	 * Open dialog for user interaction.
	 * 
	 * @return List of lists used for the chainBuild srcs argument in the Koji
	 *         api.
	 */
	public List<List<String>> open() {
		shell = new Shell(getParent(), SWT.MIN | SWT.BORDER);
		shell.setSize(640, 480);
		shell.setLayout(new GridLayout(1, false));

		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FormLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group projectGroup = new Group(composite, SWT.NONE);
		projectGroup.setLayout(new GridLayout(1, false));
		FormData projectData = new FormData();
		projectData.left = new FormAttachment(0, 0);
		projectData.right = new FormAttachment(0, 200);
		projectData.top = new FormAttachment(0, 0);
		projectData.bottom = new FormAttachment(100, 0);
		projectGroup.setLayoutData(projectData);
		projectGroup.setText(KojiText.ChainBuildDialog_PackageTitle);

		projectTable = new Table(projectGroup, SWT.CHECK | SWT.MULTI);
		resetTable();
		projectTable
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		DragSource tableSource = new DragSource(projectTable, DND.DROP_MOVE);
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		tableSource.setTransfer(types);
		tableSource.addDragListener(new DragSourceAdapter() {
			private TableItem[] items;

			@Override
			public void dragStart(DragSourceEvent event) {
				items = projectTable.getSelection();
				if (items.length > 0) {
					event.doit = true;
				} else {
					event.doit = false;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = ""; //$NON-NLS-1$
				for (TableItem item : items) {
					event.data = event.data + item.getText(0) + ";"; //$NON-NLS-1$
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE) {
					for (TableItem item : items) {
						item.dispose();
					}
				}
			}
		});
		
		DropTarget tableTarget = new DropTarget(projectTable, DND.DROP_MOVE);
		tableTarget.setTransfer(types);
		tableTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				String text = (String) event.data;
				for (String itemText : text.split(";")) { //$NON-NLS-1$
					TableItem projectItem = new TableItem(projectTable,
							SWT.NONE);
					projectItem.setText(0, itemText);
				}
			}
		});

		Composite projectButtons = new Composite(projectGroup, SWT.NONE);
		projectButtons.setLayout(new GridLayout(2, false));

		Button addButton = new Button(projectButtons, SWT.NONE);
		addButton.setText(KojiText.ChainBuildDialog_AddButton);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem root;
				if (buildTree.getItemCount() == 0) {
					root = new TreeItem(buildTree, SWT.NONE);
					root.setText("Group 1"); //$NON-NLS-1$
				} else {
					root = buildTree.getItem(buildTree.getItemCount() - 1);
				}
				for (TableItem item : projectTable.getItems()) {
					if (item.getChecked()) {
						new TreeItem(root, SWT.NONE).setText(item.getText(0));
						item.dispose();
					}
				}
				root.setExpanded(true);
			}
		});

		Button addNewButton = new Button(projectButtons, SWT.NONE);
		addNewButton.setText(KojiText.ChainBuildDialog_AddNewButton);
		addNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem root = new TreeItem(buildTree, SWT.NONE);
				root.setText("Group " //$NON-NLS-1$
						+ Integer.toString(buildTree.getItemCount()));
				for (TableItem item : projectTable.getItems()) {
					if (item.getChecked()) {
						new TreeItem(root, SWT.NONE).setText(item.getText(0));
						item.dispose();
					}
				}
				root.setExpanded(true);
			}
		});

		Label addPackages = new Label(composite, SWT.WRAP);
		addPackages.setText(KojiText.ChainBuildDialog_AddPackagesLabel);
		FormData labelData = new FormData();
		labelData.left = new FormAttachment(projectGroup, 10, SWT.RIGHT);
		labelData.right = new FormAttachment(projectGroup, 110, SWT.RIGHT);
		labelData.top = new FormAttachment(1, 2, 0);
		addPackages.setLayoutData(labelData);

		Group buildGroup = new Group(composite, SWT.NONE);
		buildGroup.setText(KojiText.ChainBuildDialog_BuildTitle);
		buildGroup.setLayout(new GridLayout(1, false));
		FormData buildData = new FormData();
		buildData.left = new FormAttachment(addPackages, 10, SWT.RIGHT);
		buildData.right = new FormAttachment(addPackages, 210, SWT.RIGHT);
		buildData.top = new FormAttachment(0, 0);
		buildData.bottom = new FormAttachment(100, 0);
		buildGroup.setLayoutData(buildData);

		buildTree = new Tree(buildGroup, SWT.CHECK | SWT.MULTI);
		new TreeItem(buildTree, SWT.NONE).setText("Group 1"); //$NON-NLS-1$
		buildTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		DragSource treeSource = new DragSource(buildTree, DND.DROP_MOVE);
		treeSource.setTransfer(types);
		treeSource.addDragListener(new DragSourceAdapter() {
			private TreeItem[] items;

			@Override
			public void dragStart(DragSourceEvent event) {
				items = buildTree.getSelection();
				for (TreeItem item : items) {
					if (item.getItemCount() == 0) {
						event.doit = true;
						return;
					}
				}
				event.doit = false;
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = ""; //$NON-NLS-1$
				for (TreeItem item : items) {
					if (item.getItemCount() == 0) {
						event.data = event.data + item.getText() + ";"; //$NON-NLS-1$
					}
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE) {
					for (TreeItem item : items) {
						if (item.getItemCount() == 0) {
							item.dispose();
						}
					}
				}
			}
		});
		
		DropTarget treeTarget = new DropTarget(buildTree, DND.DROP_MOVE);
		treeTarget.setTransfer(types);
		treeTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SELECT;
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				String text = (String) event.data;
				TreeItem item = (TreeItem) event.item;
				if (item.getParentItem() == null) {
					for (String itemText : text.split(";")) { //$NON-NLS-1$
						new TreeItem(item, SWT.NONE).setText(itemText);
					}
				} else {
					for (String itemText : text.split(";")) { //$NON-NLS-1$
						new TreeItem(item.getParentItem(), SWT.NONE)
								.setText(itemText);
					}
				}
			}
		});
		
		Button removeButton = new Button(buildGroup, SWT.NONE);
		removeButton.setText(KojiText.ChainBuildDialog_RemoveButton);
		removeButton
				.setLayoutData(new GridData(SWT.LEFT, SWT.END, false, false));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < buildTree.getItemCount(); i++) {
					TreeItem root = buildTree.getItem(i);
					if (root.getChecked()) {
						for (TreeItem item : root.getItems()) {
							TableItem projectItem = new TableItem(projectTable,
									SWT.NONE);
							projectItem.setText(0, item.getText());
						}
						root.dispose();
						i--;
					} else {
						root.setText("Group " + Integer.toString(i + 1)); //$NON-NLS-1$
						for (TreeItem item : root.getItems()) {
							if (item.getChecked()) {
								TableItem projectItem = new TableItem(
										projectTable, SWT.NONE);
								projectItem.setText(0, item.getText());
								item.dispose();
							}
						}
					}
				}
			}
		});

		Composite buildButtons = new Composite(composite, SWT.NONE);
		buildButtons.setLayout(new GridLayout(1, false));

		FormData buttonsData = new FormData();
		buttonsData.left = new FormAttachment(buildGroup);
		buildButtons.setLayoutData(buttonsData);

		Button upButton = new Button(buildButtons, SWT.NONE);
		upButton.setText(KojiText.ChainBuildDialog_UpButton);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (int i = 1; i < buildTree.getItemCount(); i++) {
					for (TreeItem item : buildTree.getItem(i).getItems()) {
						if (item.getChecked()) {
							new TreeItem(buildTree.getItem(i - 1), SWT.NONE)
									.setText(item.getText());
							item.dispose();
						}
					}
				}
			}
		});

		Button newButton = new Button(buildButtons, SWT.NONE);
		newButton.setText(KojiText.ChainBuildDialog_GroupButton);
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new TreeItem(buildTree, SWT.NONE).setText("Group " //$NON-NLS-1$
						+ Integer.toString(buildTree.getItemCount()));
			}
		});

		Button downButton = new Button(buildButtons, SWT.NONE);
		downButton.setText(KojiText.ChainBuildDialog_DownButton);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < buildTree.getItemCount() - 1; i++) {
					for (TreeItem item : buildTree.getItem(i).getItems()) {
						if (item.getChecked()) {
							new TreeItem(buildTree.getItem(i + 1), SWT.NONE)
									.setText(item.getText());
							item.dispose();
						}
					}
				}
			}
		});

		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true,
				false));
		buttonComposite.setLayout(new FormLayout());

		Button resetButton = new Button(buttonComposite, SWT.NONE);
		resetButton.setText(KojiText.ChainBuildDialog_ResetButton);
		FormData resetData = new FormData();
		resetData.left = new FormAttachment(0, 0);
		resetButton.setLayoutData(resetData);
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildTree.removeAll();
				new TreeItem(buildTree, SWT.NONE).setText("Group 1"); //$NON-NLS-1$
				resetTable();
			}
		});

		Button startButton = new Button(buttonComposite, SWT.NONE);
		startButton.setText(KojiText.ChainBuildDialog_StartButton);
		FormData startData = new FormData();
		startData.right = new FormAttachment(100, 0);
		startButton.setLayoutData(startData);
		startButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildInfo = new ArrayList<List<String>>();
				for (TreeItem root : buildTree.getItems()) {
					List<String> buildList = new ArrayList<String>();
					for (TreeItem item : root.getItems()) {
						try {
							setResult(Window.OK);
							IProjectRoot projectRoot = FedoraPackagerUtils
									.getProjectRoot(ResourcesPlugin
											.getWorkspace().getRoot()
											.getProject(item.getText()));
							rootList.add(projectRoot);
							buildList.add(FedoraPackagerUtils.getVcsHandler(
									projectRoot).getScmUrlForKoji(projectRoot,
									null));
						} catch (InvalidProjectRootException e1) {
							// probably deleted/renamed spec, just skip package
						}
					}
					buildInfo.add(buildList);
				}
				shell.close();
			}
		});
		Button cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setText(KojiText.ChainBuildDialog_CancelButton);
		FormData cancelData = new FormData();
		cancelData.right = new FormAttachment(startButton);
		cancelButton.setLayoutData(cancelData);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setResult(Window.CANCEL);
				shell.close();
			}
		});

		Display display = getParent().getDisplay();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return buildInfo;
	}

	/**
	 * Get result of dialog.
	 * 
	 * @return OK iff "Start Build" is pressed and there are packages in the
	 *         build list. Cancel otherwise.
	 */
	public int getResult() {
		return result;
	}

	private void setResult(int result) {
		this.result = result;
	}

	/**
	 * Get project roots for all packages used in the build.
	 * 
	 * @return The roots.
	 */
	public IProjectRoot[] getRoots() {
		return rootList.toArray(new IProjectRoot[] {});
	}

	private void resetTable() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			if (project.isOpen()) {
				String fedpkgProject;
				try {
					fedpkgProject = project
							.getPersistentProperty(PackagerPlugin.PROJECT_PROP);

					if (fedpkgProject != null
							&& fedpkgProject.contentEquals("true")) { //$NON-NLS-1$

						TableItem projectItem = new TableItem(projectTable,
								SWT.NONE);
						projectItem.setText(0, project.getName());

					}
				} catch (CoreException e) {
					// ignore inaccessible projects
				}
			}
		}
	}
}
