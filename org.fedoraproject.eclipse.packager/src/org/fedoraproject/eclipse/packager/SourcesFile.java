/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Sources file (named sources) are files containing MD5 hash and file name.
 * They are used by Fedora build tools to fetch SourceN files from the Fedora
 * file server.
 * 
 */
public class SourcesFile {

	private IFile sourcesFile;
	Map<String, String> sources = new LinkedHashMap<String, String>();

	/**
	 * Creates the sources file model from the given file.
	 * 
	 * @param sources
	 *            The file to parse.
	 */
	public SourcesFile(IFile sources) {
		sourcesFile = sources;
		parseSources();
	}

	/**
	 * Returns the name of the file.
	 * 
	 * @return The file name.
	 */
	public String getName() {
		return sourcesFile.getName();
	}

	private void parseSources() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					sourcesFile.getContents()));
			String line = br.readLine();
			while (line != null) {
				String[] source = line.split("\\s+"); //$NON-NLS-1$
				if (source.length != 2) {
					continue;
				}
				sources.put(source[1], source[0]);
				line = br.readLine();
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Returns the parsed sources in a map with the file name used as a key.
	 * @return The parsed sources.
	 */
	public Map<String, String> getSources() {
		return sources;
	}

	/**
	 * Returns the md5 for the given file in the sources file.
	 * @param key The file name.
	 * @return The md5 of the uploaded file.
	 */
	public String getSource(String key) {
		return sources.get(key);
	}

	/**
	 * Returns the missing sources.
	 * 
	 * @return Files that are missing locally or has different md5.
	 */
	public Set<String> getSourcesToDownload() {
		HashSet<String> missingSources = new HashSet<String>();
		for (String source : sources.keySet()) {
			IResource r = sourcesFile.getParent().findMember(source);
			// matched source name
			if (r == null || !checkMD5(sources.get(source), r)) {
				// match
				missingSources.add(source);
			}
		}
		return missingSources;
	}

	/**
	 * Delete actual file in project
	 * 
	 * @param file
	 * @throws CoreException
	 */
	public void deleteSource(String file) throws CoreException {
		IContainer branch = sourcesFile.getParent();
		IResource toDelete = branch.findMember(file);
		if (toDelete != null) {
			toDelete.delete(true, null);
		}
	}

	/**
	 * Checks whether given md5 corresponds to the given resource.
	 * @param storedMd5 The md5 to check.
	 * @param resource The file whose md5 should be compared.
	 * @return True if the given md5 is the same as the calculated one, false otherwise.
	 */
	public static boolean checkMD5(String storedMd5, IResource resource) {
		// open file
		File file = resource.getLocation().toFile();
		String md5 = getMD5(file);

		// perform check
		return md5 == null ? false : md5.equalsIgnoreCase(storedMd5);
	}

	/**
	 * Calculates md5 checksum for given file.
	 * @param file The file to calculate checksum for.
	 * @return The calculated checksum.
	 */
	public static String getMD5(File file) {
		String result = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte buf[] = new byte[(int) file.length()];
			fis.read(buf); // read entire file into buf array
			result = DigestUtils.md5Hex(buf);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

		return result;
	}

	/**
	 * Saves the sources file to the underlying file.
	 * @throws CoreException
	 */
	public void save() throws CoreException {
		PipedInputStream in = new PipedInputStream();

		// create an OutputStream with the InputStream from above as input
		PipedOutputStream out = null;
		try {
			out = new PipedOutputStream(in);
			PrintWriter pw = new PrintWriter(out);
			for (Map.Entry<String, String> entry : sources.entrySet()) {
				pw.println(entry.getValue() + "  " + entry.getKey()); //$NON-NLS-1$
			}
			pw.close();
			out.close();
			sourcesFile.refreshLocal(1, null);
			sourcesFile.setContents(in, true, true, null);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR,
					PackagerPlugin.PLUGIN_ID, "Error saving "
							+ sourcesFile.getName()));
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Nothing to do
				}
			}
		}

	}

}
