package com.sixsq.slipstream.util;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;

public class ProcessUtils {

	public static String execGetOutput(String[] command) throws IOException,
			SlipStreamClientException {

		String commandMessage = "";
		for (String part : command) {
			commandMessage += part + " ";
		}
		getLogger().info("Calling: " + commandMessage);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);

		Process p = pb.start();

		StringBuffer outputBuf = new StringBuffer();
		BufferedReader stdOutErr = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line;
		while ((line = stdOutErr.readLine()) != null) {
			outputBuf.append(line);
			outputBuf.append("\n");
			getLogger().info(line);
		}

		// Check for failure
		try {
			if (p.waitFor() != 0) {
				String error = "Error executing: " + commandMessage
						+ ". With exit code = " + p.exitValue()
						+ " and output: " + outputBuf;
				getLogger().severe(error);
				throw (new SlipStreamClientException(outputBuf.toString()));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		} finally {
			stdOutErr.close();
		}
		return outputBuf.toString();
	}

	protected static Logger getLogger() {
		return Logger.getLogger("SlipStream");
	}
}