/*
 * If not stated otherwise in this file or this component's Licenses.txt file the
 * following copyright and licenses apply:
 *
 * Copyright 2024 RDK Management
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.service.utilservices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reads the data from the input stream and returns the data as a
 * string.
 */
public class StreamReaderJob implements Callable<String> {

	public static final Logger LOGGER = LoggerFactory.getLogger(StreamReaderJob.class);

	private InputStream inputStream;
	private String outputFileName;

	/**
	 * Constructor that takes the input stream.
	 *
	 * @param inputStream The input stream of data.
	 */
	public StreamReaderJob(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public StreamReaderJob(InputStream inputStream, String outputFileName) {
		this.inputStream = inputStream;
		this.outputFileName = outputFileName;
	}

	/**
	 * This method will be called by the invoking thread. Reads the data from the
	 * input stream and adds the content to a String buffer. On the end of the
	 * stream, the string buffer is returned.
	 * 
	 * @return Data read from the stream.
	 */
	@Override
	public String call() throws Exception {
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line).append(System.lineSeparator());
				if (outputFileName != null) {
					writeToOutputFile(outputFileName, line + System.lineSeparator());
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error reading input stream", e);
		}
		return stringBuilder.toString();
	}

	/**
	 * This method writes the data to the output file.
	 * 
	 * @param fileName The output file name.
	 * @param data     The data to be written to the file.
	 */
	private void writeToOutputFile(String fileName, String data) {
		try {
			Path filePath = Paths.get(fileName);
			Files.createDirectories(filePath.getParent());
			if (!Files.exists(filePath)) {
				Files.createFile(filePath);
			}
			Files.writeString(filePath, data, StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.error("Error writing to log file", e);
		}
	}
}