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

import org.eclipse.osgi.util.NLS;


/**
 * Translation bundle for FedoraPackager core.
 */
@SuppressWarnings("javadoc")
public class FedoraPackagerText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 *
	 * This is the path to the file containing externalized strings.
	 */
	private static final String BUNDLE_NAME = "org.fedoraproject.eclipse.packager.fedorapackagertext"; //$NON-NLS-1$

	// ConsoleWriterThread
	/****/ public static String ConsoleWriterThread_ioFail;
	// DownloadHandler Strings
	/****/ public static String DownloadHandler_downloadSourceTask;
	// UploadHandler Strings
	/****/ public static String UploadHandler_expiredCertificateError;
	/****/ public static String UploadHandler_revokedCertificateError;
	/****/ public static String UploadHandler_taskName;
	/****/ public static String UploadHandler_versionOfFileExistsAndUpToDate;
	/****/ public static String UploadHandler_invalidUrlError;
	// UploadSourceCommand
	/****/ public static String UploadSourceCommand_uploadFileUnspecified;
	/****/ public static String UploadSourceCommand_uploadFileInvalid;
	/****/ public static String UploadSourceCommand_uploadingFileSubTaskName;
	// FileAvailableInLookasideCacheException
	/****/ public static String FileAvailableInLookasideCacheException_message;
	// DownloadSourceCommand
	/****/ public static String DownloadSourceCommand_nothingToDownload;
	/****/ public static String DownloadSourceCommand_downloadFile;
	/****/ public static String DownloadSourceCommand_downloadFileError;
	/****/ public static String DownloadSourceCommand_invalidURL;
	/****/ public static String DownloadSourceCommand_downloadingFileXofY;
	/****/ public static String DownloadSourceCommand_downloadFileErrorNotInLookaside;
	// SourcesFile
	/****/ public static String SourcesFile_saveFailedMsg;
	/****/ public static String SourcesFile_saveJob;
	// FedoraSSL
	/****/ public static String FedoraSSL_certificatesMissingError;
	// FedoraPackagerUtils
	/****/ public static String FedoraPackagerUtils_invalidProjectRootError;
	/****/ public static String FedoraPackagerUtils_invalidContainerOrProjectType;
	/****/ public static String FedoraPackagerUtils_cannotEvalPackageName;
	/****/ public static String FedoraPackagerUtils_cannotEvalChangelog;
	// UnpushedChangesListener
	/****/ public static String UnpushedChangesListener_checkUnpushedChangesMsg;
	/****/ public static String UnpushedChangesListener_unpushedChangesError;
	// TagSourcesListener
	/****/ public static String TagSourcesListener_tagBeforeSendingBuild;
	/****/ public static String TagSourcesListener_tagSourcesMsg;
	// ChecksumValidListener
	/****/ public static String ChecksumValidListener_badChecksum;
	// VCSIgnoreFileUpdater
	/****/ public static String VCSIgnoreFileUpdater_couldNotCreateFile;
	/****/ public static String VCSIgnoreFileUpdater_errorWritingFile;
	// SourcesFileUpdater
	/****/ public static String SourcesFileUpdater_errorSavingFile;
	public static String FedoraPackagerPreferencePage_Always;

	public static String FedoraPackagerPreferencePage_Ask;

	// FedoraPackagerPreferencesPage
	/****/ public static String FedoraPackagerPreferencePage_lookasideUploadURLLabel;
	/****/ public static String FedoraPackagerPreferencePage_lookasideDownloadURLLabel;
	/****/ public static String FedoraPackagerPreferencePage_description;
	/****/ public static String FedoraPackagerPreferencePage_invalidDownloadURLMsg;
	/****/ public static String FedoraPackagerPreferencePage_invalidUploadURLMsg;
	/****/ public static String FedoraPackagerPreferencePage_invalidConfigurationFile;
	/****/ public static String FedoraPackagerPreferencePage_invalidConfigurationFileMissing;
	/****/ public static String FedoraPackagerPreferencePage_kojiWebURLLabel;
	/****/ public static String FedoraPackagerPreferencePage_kojiHubURLLabel;
	/****/ public static String FedoraPackagerPreferencePage_kojiWebURLInvalidMsg;
	/****/ public static String FedoraPackagerPreferencePage_kojiHubURLInvalidMsg;
	/****/ public static String FedoraPackagerPreferencePage_buildSystemGroupName;
	/****/ public static String FedoraPackagerPreferencePage_lookasideGroupName;
	/****/ public static String FedoraPackagerPreferencePage_generalGroupName;
	/****/ public static String FedoraPackagerPreferencePage_debugSwitchLabel;
	/****/ public static String FedoraPackagerPreferencePage_fedpkgConfigGroupName;
	/****/ public static String FedoraPackagerPreferencePage_fedpkgConfigStringLabel;
	/****/ public static String FedoraPackagerPreferencePage_fedpkgConfigSwitch;

	// FedoraPackagerConfigPreference
	/****/ public static String FedoraPackagerConfigPreference_parsingFileError;
	/****/ public static String FedoraPackagerConfigPreference_settingPreferenceError;

	public static String FedoraPackagerPreferencePage_Never;

	public static String FedoraPackagerPreferencePage_switchPerspectiveAfterLocalProjectCreation;

	public static String FedoraPackagerPreferencePage_switchPerspectiveAfterProjectCheckout;
	// FedoraPackagerCommand
	/****/ public static String FedoraPackagerCommand_projectRootSetTwiceError;
	/****/ public static String FedoraPackager_Cant_Create;

	// FedoraPackager
	/****/ public static String FedoraPackager_commandNotFoundError;
	/****/ public static String FedoraPackager_cannotEvalPackageName;
	// FedoraPackagerRoot
	/****/ public static String FedoraProjectRoot_failedToRefreshResource;
	/****/ public static String FedoraProjectRoot_invalidResource;
	/****/ public static String FedoraProjectRoot_failureReadingFromFile;
	// Generic strings
	/****/ public static String somethingUnexpectedHappenedError;
	/****/ public static String commandWasCalledInTheWrongState;
	/****/ public static String invalidFedoraProjectRootError;
	/****/ public static String extensionNotFoundError;
	//Local Fedora Packager Project-wizards-main
	/****/ public static String LocalFedoraPackagerWizardPage_title;
	/****/ public static String LocalFedoraPackagerWizardPage_description;
	/****/ public static String LocalFedoraPackagerWizardPage_image;
	//Local Fedora Packager Project-wizards-page one
	/****/ public static String LocalFedoraPackagerPageOne_lblNoteGit;
	//Local Fedora Packager Project-wizards-page Two
	/****/ public static String LocalFedoraPackagerPageTwo_linkFAS;
	/****/ public static String LocalFedoraPackagerPageTwo_urlFAS;
	/****/ public static String LocalFedoraPackagerPageTwo_lblTextFAS;
	/****/ public static String LocalFedoraPackagerPageTwo_linkBugzilla;
	/****/ public static String LocalFedoraPackagerPageTwo_urlBugzilla;
	/****/ public static String LocalFedoraPackagerPageTwo_linkInitial;
	/****/ public static String LocalFedoraPackagerPageTwo_urlInitial;
	/****/ public static String LocalFedoraPackagerPageTwo_linkIntroduce;
	/****/ public static String LocalFedoraPackagerPageTwo_urlIntroduce;
	/****/ public static String LocalFedoraPackagerPageTwo_btnRadioNewMaintainer;
	/****/ public static String LocalFedoraPackagerPageTwo_btnRadioExistMaintainer;
	/****/ public static String LocalFedoraPackagerPageTwo_grpAccountSetup;
	//Local Fedora Packager Project-wizards-page Three
	/****/ public static String LocalFedoraPackagerPageThree_grpSpec;
	/****/ public static String LocalFedoraPackagerPageThree_btnCheckStubby;
	/****/ public static String LocalFedoraPackagerPageThree_btnBrowse;
	/****/ public static String LocalFedoraPackagerPageThree_btnCheckSrpm;
	/****/ public static String LocalFedoraPackagerPageThree_lblSrpm;
	/****/ public static String LocalFedoraPackagerPageThree_lblSpecPlain;
	/****/ public static String LocalFedoraPackagerPageThree_btnCheckPlain;
	/****/ public static String LocalFedoraPackagerPageThree_btnTemplateSpec;
	/****/ public static String LocalFedoraPackagerPageThree_fileDialog;
	/****/ public static String LocalFedoraPackagerPageThree_fileExistence;
	//Local Fedora Packager Project-api
	/****/ public static String LocalFedoraPackagerProjectCreator_FirstCommit;
	//Local Fedora Packager Project-api-errors
	/****/ public static String invalidLocalFedoraProjectRootError;
	public static String UiUtils_RememberChoice;

	//Fedora Packager Project-perspective message
	/****/ public static String UiUtils_switchPerspectiveQuestionTitle;
	/****/ public static String UiUtils_switchPerspectiveQuestionMsg;
	// ScpCommand
	/****/ public static String ScpCommand_choosePrivateKey;
	/****/ public static String ScpCommand_filesToScpMissing;
	/****/ public static String ScpCommand_notificationTitle;
	/****/ public static String ScpCommand_filesToScpExist;
	/****/ public static String ScpCommand_filesToScpNonReadable;
	/****/ public static String ScpCommand_NoSession;
	// ScpHandler
	/****/ public static String ScpHandler_taskName;
	/****/ public static String ScpHandler_notificationTitle;
	/****/ public static String ScpHandler_scpFilesNotifier;
	/****/ public static String ScpHandler_failToScp;
	/****/ public static String ScpHandler_fasAccountMissing;
	/****/ public static String ScpHandler_FilesDialogTitle;
	// LinkedMessageDialog
	/****/ public static String LinkedMessageDialog_unableToInitView;

	static {
		initializeMessages(BUNDLE_NAME,	FedoraPackagerText.class);
	}
}
