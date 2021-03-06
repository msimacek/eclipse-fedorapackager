/*******************************************************************************
 * Copyright (c) 2010, 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.koji;

import org.eclipse.osgi.util.NLS;

/**
 * Text for the koji plug-in.
 *
 */
public class KojiText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 *
	 * This is the path to the file containing externalized strings.
	 */
	private static final String BUNDLE_NAME = "org.fedoraproject.eclipse.packager.koji.kojitext"; //$NON-NLS-1$

	// KojiBuildHandler Strings
	/****/ public static String KojiBuildHandler_pushBuildToKoji;
	/****/ public static String KojiBuildHandler_kojiBuild;
	/****/ public static String KojiBuildHandler_unknownBuildError;
	/****/ public static String KojiBuildHandler_invalidHubUrl;
	/****/ public static String KojiBuildHandler_errorGettingNVR;
	/****/ public static String KojiBuildHandler_invalidKojiWebUrl;
	/****/ public static String KojiBuildHandler_unknownLoginErrorMsg;
	/****/ public static String KojiBuildHandler_missingCertificatesMsg;
	/****/ public static String KojiBuildHandler_certificateExpriredMsg;
	/****/ public static String KojiBuildHandler_certificateRevokedMsg;
	// KojiMessageDialog Strings
	/****/ public static String KojiMessageDialog_buildNumberMsg;
	// KojiBuildCommand
	/****/ public static String KojiBuildCommand_sendBuildCmd;
	/****/ public static String KojiBuildCommand_kojiLogoutTask;
	/****/ public static String KojiBuildCommand_configErrorNoClient;
	/****/ public static String KojiBuildCommand_configErrorNoScmURL;
	/****/ public static String KojiBuildCommand_configErrorNoBuildTarget;
	/****/ public static String KojiBuildCommand_configErrorNoNVR;
	/****/ public static String KojiBuildCommand_kojiLogInTask;
	// BuildAlreadyExistsException
	/****/ public static String BuildAlreadyExistsException_msg;
	// ChainBuildDialog
	/****/ public static String ChainBuildDialog_AddButton;
	/****/ public static String ChainBuildDialog_AddNewButton;
	/****/ public static String ChainBuildDialog_AddPackagesLabel;
	/****/ public static String ChainBuildDialog_BuildTitle;
	/****/ public static String ChainBuildDialog_CancelButton;
	/****/ public static String ChainBuildDialog_DownButton;
	/****/ public static String ChainBuildDialog_GroupButton;
	/****/ public static String ChainBuildDialog_PackageTitle;
	/****/ public static String ChainBuildDialog_RemoveButton;
	/****/ public static String ChainBuildDialog_ResetButton;
	/****/ public static String ChainBuildDialog_StartButton;
	/****/ public static String ChainBuildDialog_UpButton;

	// FedoraPackagerKojiPreferencePage
	/****/ public static String FedoraPackagerKojiPreferencePage_AdvancedButtonText;
	/****/ public static String FedoraPackagerKojiPreferencePage_ChangeInstanceLabel;
	/****/ public static String FedoraPackagerKojiPreferencePage_DefaultPlaceholder;
	/****/ public static String FedoraPackagerKojiPreferencePage_InstanceColumnTitle;
	/****/ public static String FedoraPackagerKojiPreferencePage_InstancePromptMsg;
	/****/ public static String FedoraPackagerKojiPreferencePage_KojiPreferenceInformation;
	/****/ public static String FedoraPackagerKojiPreferencePage_KojiServers;
	/****/ public static String FedoraPackagerKojiPreferencePage_ProjectColumnTitle;
	/****/ public static String FedoraPackagerKojiPreferencePage_TableDescription;
	/****/ public static String FedoraPackagerKojiPreferencePage_ButtonAdd;
	/****/ public static String FedoraPackagerKojiPreferencePage_ButtonEdit;
	/****/ public static String FedoraPackagerKojiPreferencePage_ButtonRemove;
	/****/ public static String FedoraPackagerKojiPreferencePage_ColumnName;
	/****/ public static String FedoraPackagerKojiPreferencePage_ColumnWebURL;
	/****/ public static String FedoraPackagerKojiPreferencePage_ColumnXMLRPCURL;
	/****/ public static String FedoraPackagerKojiPreferencePage_ColumnCustomBuildTargets;
	// FedoraPackagerAdvancedKojiDialogPage
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_AddButton;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_EditButton;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_editDialogTitle;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_namespaceWarningMsg;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_placeholderWarningMsg;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_RemoveButton;
	/****/ public static String FedoraPackagerAdvancedKojiDialogPage_serverDialogTitle;
	// KojiServerDialog
	/****/ public static String KojiServerDialog_CustomTargetLabel;
	// KojiSRPMScratchBuildHandler
	/****/ public static String KojiSRPMScratchBuildHandler_UploadFileDialogTitle;
	// KojiHubClientLoginException
	/****/ public static String KojiHubClientLoginException_loginFailedMsg;
	// KojiProjectPropertyPage
	/****/ public static String KojiProjectPropertyPage_forceCustomTitle;
	/****/ public static String KojiProjectPropertyPage_ProjectKojiSelect;
	/****/ public static String KojiProjectPropertyPage_ProjectSettings;
	/****/ public static String KojiProjectPropertyPage_WorkspaceSettings;
	/****/ public static String KojiProjectPropertyPage_optionsGroup;
	// KojiSRPMBuildJob
	/****/ public static String KojiSRPMBuildJob_ChooseSRPM;
	/****/ public static String KojiSRPMBuildJob_ConfiguringClient;
	/****/ public static String KojiSRPMBuildJob_NoSRPMsFound;
	/****/ public static String KojiSRPMBuildJob_UploadingSRPM;
	// KojiTagDialog
	/****/ public static String KojiTargetDialog_DialogTitle;
	// KojiUploadSRPMCommand
	/****/ public static String KojiUploadSRPMCommand_CouldNotRead;
	/****/ public static String KojiUploadSRPMCommand_FileNotFound;
	/****/ public static String KojiUploadSRPMCommand_InvalidSRPM;
	/****/ public static String KojiUploadSRPMCommand_NoMD5;
	/****/ public static String KojiUploadSRPMCommand_NoSRPM;
	/****/ public static String KojiUploadSRPMCommand_NoUploadPath;
	// KojiUplaodSRPMJob
	/****/ public static String KojiUploadSRPMJob_KojiUpload;
	// Generic Strings
	/****/ public static String xmlRPCconfigNotInitialized;
	/****/ public static String ServerEntryTemplate;

	// Koji Wait Repo Strings
	/****/ public static String KojiWaitForRepoJob_repoUpdatedDialogTitle;
	/****/ public static String KojiWaitForRepoJob_repoUpdatedDialogText;
	/****/ public static String KojiWaitForRepoJob_collectingRepoTags;
	/****/ public static String KojiWaitForRepoJob_WaitingForUpdateMessage;

	// Generic Koji Strings
	/****/ public static String KojiWaitForRepoHandler_errorGettingRepoInfo;

	static {
		initializeMessages(BUNDLE_NAME,	KojiText.class);
	}
}
