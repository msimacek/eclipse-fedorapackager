/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.internal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fedoraproject.eclipse.packager.ConfigKeys;
import org.fedoraproject.eclipse.packager.KojiConfigKeys;

/**
 * Grab preference properties from the configuration file(s). This will also
 * grab the koji configuration properties if "kojiconfig" property exists within
 * the configuration file.
 *
 */
public class ConfigParser {
	private File config_file;
	private Map<String, String> configMap = new ConfigMap(new String[] {
			ConfigKeys.LOOKASIDE, ConfigKeys.LOOKASIDE_HASH,
			ConfigKeys.LOOKASIDE_CGI, ConfigKeys.GIT_BASE_URL,
			ConfigKeys.ANON_GIT_URL, ConfigKeys.TRAC_BASE_URL,
			ConfigKeys.BRANCH_RE, ConfigKeys.KOJI_CONFIG,
			ConfigKeys.BUILD_CLIENT });
	private Map<String, String> kojiConfigMap = new ConfigMap(new String[] {
			KojiConfigKeys.AUTH_TYPE, KojiConfigKeys.CA, KojiConfigKeys.CERT,
			KojiConfigKeys.KRB_SERVICE, KojiConfigKeys.SERVER,
			KojiConfigKeys.SERVER_CA, KojiConfigKeys.TOP_DIR,
			KojiConfigKeys.TOP_URL, KojiConfigKeys.WEB_URL });
	private static final String RX_DEFINITION = "\\s*(\\w+)\\s*(?:=\\s*(\\S+))\\s*"; //$NON-NLS-1$

	/**
	 * Constructor that parses after verifying file is not empty.
	 *
	 * @param file
	 *            The packager configuration file.
	 * @throws IOException
	 *             Thrown when cannot see available bytes in file.
	 */
	public ConfigParser(File file) throws IOException {
		config_file = file;
		if (config_file.length() <= 0) {
			return;
		}
		parse();
	}

	/**
	 * Parse the configuration file(s) and store the configuration properties.
	 *
	 * @throws FileNotFoundException
	 *             Thrown when file cannot be found.
	 */
	private void parse() throws FileNotFoundException {
		try (Scanner scanner = new Scanner(new FileInputStream(config_file))) {
			String line;
			while (scanner.hasNext()) {
				line = scanner.nextLine();
				if (matchesSimpleAssignment(line)) {
					configMap.putAll(getValue(line));
				}
			}
		}
		String kojiConfigLocation = configMap.get(ConfigKeys.KOJI_CONFIG);
		File kojiFile = new File(kojiConfigLocation);
		if (kojiFile.exists()) {
			try (Scanner scanner = new Scanner(new FileInputStream(kojiFile))) {
				String line;
				while (scanner.hasNext()) {
					line = scanner.nextLine();
					if (matchesSimpleAssignment(line)) {
						kojiConfigMap.putAll(getValue(line));
					}
				}
			}
		}
	}

	/**
	 * Get the configuration properties from the file.
	 *
	 * @return The configuration properties.
	 */
	public Map<String, String> getConfig() {
		return configMap;
	}

	/**
	 * Get the koji configuration properties from the file.
	 *
	 * @return The koji configuration properties.
	 */
	public Map<String, String> getKojiConfig() {
		return kojiConfigMap;
	}

	/**
	 * Grab the base URL used for cloning. Ignore the URL scheme and the files.
	 *
	 * @return The base clone URL.
	 */
	public String getCloneBaseURL() {
		String rc = ""; //$NON-NLS-1$
		String anongiturl = configMap.get(ConfigKeys.ANON_GIT_URL);
		final String PROTOCOL_SEPARATOR = "://"; //$NON-NLS-1$
		final String UNWANTED_MACRO = "%(module)s"; //$NON-NLS-1$

		if (!anongiturl.isEmpty()) {
			int baseURLOffset = anongiturl.indexOf(PROTOCOL_SEPARATOR);
			baseURLOffset = baseURLOffset == -1 ? 0 : baseURLOffset
					+ PROTOCOL_SEPARATOR.length();
			int endOfBaseURL = anongiturl.indexOf(UNWANTED_MACRO);
			endOfBaseURL = endOfBaseURL == -1 ? anongiturl.length()
					: endOfBaseURL;
			rc = anongiturl.substring(baseURLOffset, endOfBaseURL);
		}

		return rc;
	}

	/**
	 * Get the value from the line.
	 *
	 * @param line
	 *            The line to get the value from.
	 * @return The configuration property with its associated value.
	 */
	private static Map<String, String> getValue(String line) {
		Map<String, String> rc = new HashMap<>();
		Pattern pattern = Pattern.compile(RX_DEFINITION,
				Pattern.CASE_INSENSITIVE);
		Matcher variableMatcher = pattern.matcher(line);
		if (variableMatcher.find()) {
			rc.put(variableMatcher.group(1).trim().toLowerCase(),
					variableMatcher.group(2).trim());
		}
		return rc;
	}

	/**
	 * Check if the line matches a simple assignment.
	 *
	 * @param line
	 *            The line to check against.
	 * @return True if the line is a simple assignment, false otherwise.
	 */
	private static boolean matchesSimpleAssignment(String line) {
		boolean rc = false;
		Pattern pattern = Pattern.compile(RX_DEFINITION,
				Pattern.CASE_INSENSITIVE);
		Matcher variableMatcher = pattern.matcher(line);
		if (variableMatcher.matches()) {
			rc = true;
		}
		return rc;
	}
}
