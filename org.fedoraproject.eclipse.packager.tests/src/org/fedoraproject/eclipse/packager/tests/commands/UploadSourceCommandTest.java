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
package org.fedoraproject.eclipse.packager.tests.commands;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Stack;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.SourcesFileUpdater;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;
import org.fedoraproject.eclipse.packager.api.UploadSourceResult;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.FileAvailableInLookasideCacheException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidUploadFileException;
import org.fedoraproject.eclipse.packager.tests.units.UploadFileValidityTest;
import org.fedoraproject.eclipse.packager.tests.utils.MockableUploadSourceCommand;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/**
 * Eclipse plug-in test for UploadSourceCommand. Note: in order to run this test
 * successfully, one has to deploy the upload.cgi Python script as provided in
 * the resources folder on a test machine. After that make sure to set the
 * "org.fedoraproject.eclipse.packager.tests.LookasideUploadUrl" system property
 * to point to the appropriate URL.
 */
public class UploadSourceCommandTest {

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	private static final String EXAMPLE_UPLOAD_FILE = "resources/callgraph-factorial.zip"; // $NON-NLS-1$
	private static final String INVALID_UPLOAD_FILE = "resources/invalid_upload_file.exe"; // $NON-NLS-1$
	
	private final String EMPTY_FILE_NAME_VALID = "REMOVE_ME.zip";

	private String uploadURLForTesting;

	// List of temporary resources which should get deleted after test runs
	private Stack<File> tempFilesAndDirectories = new Stack<>();

	/**
	 * Set up a Fedora project and run the command.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.uploadURLForTesting = "https://pkgs.fedoraproject.org/repo/pkgs/upload.cgi";
		this.testProject = new GitTestProject("apache-commons-codec");
		IProjectRoot fpRoot = new FedoraProjectRoot();
		fpRoot.initialize(this.testProject.getProject());
		this.packager = new FedoraPackager(fpRoot);
	}

	@After
	public void tearDown() throws Exception {
		this.testProject.dispose();
		while (!tempFilesAndDirectories.isEmpty()) {
			File file = tempFilesAndDirectories.pop();
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					f.delete();
				}
			}
			file.delete();
		}
	}

	/**
	 * UploadSourceCommand.setUploadURL() should not accept invalid URLs.
	 * 
	 * @throws Exception
	 */
	@Test(expected = MalformedURLException.class)
	public void shouldThrowMalformedURLException() throws Exception {
		UploadSourceCommand uploadCmd = (UploadSourceCommand) packager
				.getCommandInstance(UploadSourceCommand.ID);
		uploadCmd.setUploadURL("very bad url");
	}

	/**
	 * Uploading sources in Fedora entails two requests. First a POST is fired
	 * with filename and MD5 as parameters and the server returns if the
	 * resource is "missing" or "available". Should sources be already
	 * available, an FileAvailableInLookasideCacheException should be thrown.
	 * @throws IOException 
	 * 
	 */
	@Test(expected=FileAvailableInLookasideCacheException.class)
	public void canDetermineIfSourceIsAvailable() throws IOException, FedoraPackagerAPIException  {
		String fileName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_UPLOAD_FILE), null)).getFile();
		File file = new File(fileName);
		MockableUploadSourceCommand uploadCmd = (MockableUploadSourceCommand) packager
				.getCommandInstance(MockableUploadSourceCommand.ID);
		CloseableHttpClient mockClient = createStrictMock(CloseableHttpClient.class);
		CloseableHttpResponse mockResponse = createMock(CloseableHttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream(UploadSourceCommand.RESOURCE_AVAILABLE
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class));
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		uploadCmd.setClient(mockClient).setUploadURL(uploadURLForTesting)
					.setFileToUpload(file).call(new NullProgressMonitor());
	}

	/**
	 * Generate a file which will have a different checksum than any other
	 * already uploaded file for package {@code eclipse-fedorapackager}. Then
	 * attempt to upload this file.
	 * 
	 */
	@Test
	public void canUploadSources() throws FedoraPackagerAPIException, IOException {
		MockableUploadSourceCommand uploadCmd = (MockableUploadSourceCommand) packager
				.getCommandInstance(MockableUploadSourceCommand.ID);
		CloseableHttpClient mockClient = createStrictMock(CloseableHttpClient.class);
		CloseableHttpResponse mockResponse = createMock(CloseableHttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream(UploadSourceCommand.RESOURCE_MISSING
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class));
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class));
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);

		// create a a temp file with checksum, which hasn't been uploaded so far
		File newUploadFile = File.createTempFile(
				"eclipse-fedorapackager-uploadsources-test-", "-REMOVE_ME.tar");
		// add file to stack for removal after test run
		tempFilesAndDirectories.push(newUploadFile);
		writeRandomContentToFile(newUploadFile);
		UploadSourceResult result = null;
		result = uploadCmd.setClient(mockClient)
					.setUploadURL(uploadURLForTesting)
					.setFileToUpload(newUploadFile)
					.call(new NullProgressMonitor());
		assertNotNull(result);
		assertTrue(result.isSuccessful());
		verify(mockClient);
	}

	/**
	 * After a file is uploaded, the {@code sources} file should be updated with
	 * the new checksum/filename. This test checks for this.
	 */
	@Test
	public void canUpdateSourcesFile() throws IOException, FedoraPackagerAPIException  {
		// Create a a temp file with checksum, which hasn't been uploaded so
		// far. We need to upload a new non-existing file into the lookaside
		// cache. Otherwise a file exists exception is thrown and nothing will
		// be updated.
		File newUploadFile = File.createTempFile(
				"eclipse-fedorapackager-uploadsources-test-", "-REMOVE_ME.tar");
		// add file to stack for removal after test run
		tempFilesAndDirectories.push(newUploadFile);
		writeRandomContentToFile(newUploadFile);

		// sources file pre-update
		File sourcesFile = new File(testProject.getProject().getLocation()
				.toFile().getAbsolutePath()
				+ File.separatorChar + SourcesFile.SOURCES_FILENAME);
		String sourcesFileContentPre = TestsUtils.readContents(sourcesFile);
		IProjectRoot root = FedoraPackagerUtils.getProjectRoot(testProject
				.getProject());

		// create listener
		SourcesFileUpdater sourcesUpdater = new SourcesFileUpdater(root,
				newUploadFile);
		MockableUploadSourceCommand uploadCmd = (MockableUploadSourceCommand) packager
				.getCommandInstance(MockableUploadSourceCommand.ID);
		CloseableHttpClient mockClient = createStrictMock(CloseableHttpClient.class);
		CloseableHttpResponse mockResponse = createMock(CloseableHttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream(UploadSourceCommand.RESOURCE_MISSING
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class));
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class));
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		uploadCmd.setClient(mockClient);
		uploadCmd.setFileToUpload(newUploadFile);
		uploadCmd.setUploadURL(uploadURLForTesting);
		uploadCmd.addCommandListener(sourcesUpdater);
		UploadSourceResult result = uploadCmd.call(new NullProgressMonitor());
		assertNotNull(result);
		assertTrue(result.isSuccessful());
		final String sourceContentPost = TestsUtils.readContents(sourcesFile);
		// assert sources file has been updated as expected
		assertNotSame(sourcesFileContentPre, sourceContentPost);
		assertTrue(sourceContentPost.contains(sourcesFileContentPre));
		assertTrue(sourceContentPost.contains(newUploadFile.getName()));
		int lastLineCharPos = sourceContentPost.lastIndexOf('\n');
		String lastLine = sourceContentPost.substring(++lastLineCharPos);
		assertEquals(SourcesFile.calculateChecksum(newUploadFile) + "  "
				+ newUploadFile.getName(), lastLine);
	}

	/**
	 * When setting the upload file it should throw InvalidUploadFileException
	 * if the file name is not valid. Other upload file validity test are tested
	 * in {@link UploadFileValidityTest}.
	 */
	@Test(expected = InvalidUploadFileException.class)
	public void canDetermineValidUploadFiles() throws FedoraPackagerAPIException, IOException {
		UploadSourceCommand uploadCmd = (UploadSourceCommand) packager
				.getCommandInstance(UploadSourceCommand.ID);
		String invalidUploadFileName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(INVALID_UPLOAD_FILE), null)).getFile();
		File invalidUploadFile = new File(invalidUploadFileName);
		uploadCmd.setFileToUpload(invalidUploadFile);
	}

	@Test(expected = InvalidUploadFileException.class)
	public void cannotUploadEmptyFile() throws FedoraPackagerAPIException, IOException {
		UploadSourceCommand uploadCmd = (UploadSourceCommand) packager
				.getCommandInstance(UploadSourceCommand.ID);
		
		IProject project = testProject.getProject();
		File emptyUploadFile = new File(project.getLocation().toOSString() +
				IPath.SEPARATOR + EMPTY_FILE_NAME_VALID);
		emptyUploadFile.createNewFile();
		
		uploadCmd.setFileToUpload(emptyUploadFile);
	}
	
	/**
	 * Make sure to write some randomly generated content to this temporary file
	 * 
	 * @param newFile
	 */
	private static void writeRandomContentToFile(File newFile) {
		try (FileOutputStream out = new FileOutputStream(newFile)){
			StringBuilder randomContent = new StringBuilder();
			randomContent.append(Math.random());
			randomContent.append("GARBAGE");
			randomContent.append(System.nanoTime());
			randomContent.append("more random content");
			randomContent.append(System.nanoTime());
			ByteArrayInputStream in = new ByteArrayInputStream(randomContent
					.toString().getBytes());
			
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buf)) != -1) {
				out.write(buf, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
