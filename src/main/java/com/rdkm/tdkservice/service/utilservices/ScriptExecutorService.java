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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * The ScriptExecutorService class provides the methods to execute Test script
 * and other framework Python scripts.
 */
@Service
public class ScriptExecutorService {

	public static final Logger LOGGER = LoggerFactory.getLogger(ScriptExecutorService.class);

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * Executes a Python script with the given command and wait time. Mostly used
	 * for executing the framework python scripts for image version, file transfer ,
	 * device status checker etc.
	 *
	 * @param command  the command to execute as an array of strings
	 * @param waittime the maximum time to wait for the script to complete, in
	 *                 seconds
	 * @return the output of the script, or null if an error occurs
	 */
	public String executeScript(String[] command, int waittime) {

		// Initialize the process variable
		Process process = null;
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			process = processBuilder.start();

			// Create StreamReaderJob instances to read the process's input and error
			// streams
			StreamReaderJob dataReader = new StreamReaderJob(process.getInputStream());
			StreamReaderJob errorReader = new StreamReaderJob(process.getErrorStream());

			// Create FutureTask instances to handle the reading of the streams concurrently
			FutureTask<String> dataReaderTask = new FutureTask<>(dataReader);
			FutureTask<String> errorReaderTask = new FutureTask<>(errorReader);
			executorService.execute(dataReaderTask);
			executorService.execute(errorReaderTask);

			boolean finished;
			if (waittime == 0) {
				process.waitFor(); // just wait until finished
				finished = true;
			} else {
				finished = process.waitFor(waittime, TimeUnit.SECONDS);
				if (!finished) {
					process.destroyForcibly();
					LOGGER.debug("Process killed due to timeout.");
				}
			}

			String outputData;
			String errorData;
			outputData = dataReaderTask.get();
			errorData = errorReaderTask.get();

			if (errorData != null && !errorData.isEmpty()) {
				LOGGER.error(" Error while executing command: {}", String.join(" ", command));
			}

			if (null != process)
				process.destroyForcibly();
			return outputData;
		} catch (IOException e) {
			LOGGER.error("Error executing script: " + e.getMessage());
			return null;
		} catch (InterruptedException e) {
			LOGGER.error("Script execution interrupted: " + e.getMessage());
			return null;
		} catch (Exception e) {
			LOGGER.error("Error reading script output: " + e.getMessage());
			return null;
		} finally {
			if (null != process)
				process.destroyForcibly();
		}
	}

	/**
	 * Executes a test script with the given command, wait time, and output file.
	 *
	 * @param command    the command to execute as an array of strings
	 * @param waittime   the maximum time to wait for the script to complete, in
	 *                   minutes
	 * @param outputFile the name of the file to write the output to
	 * @return the output of the script
	 */
	public String executeTestScript(String[] command, final int waitTime, String outputFile) {
		Process process;
		try {
			// Ensure the log file is created initially
			Path logFilePath = Paths.get(outputFile);
			Files.createDirectories(logFilePath.getParent());
			if (!Files.exists(logFilePath)) {
				Files.createFile(logFilePath);
			}

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.environment().put("PYTHONUNBUFFERED", "1");
			process = processBuilder.start();
			// Stream logs in real-time
			ExecutorService executorService = Executors.newFixedThreadPool(2);
			StreamReaderJob outputReader = new StreamReaderJob(process.getInputStream(), outputFile);
			StreamReaderJob errorReader = new StreamReaderJob(process.getErrorStream(), outputFile);

			FutureTask<String> dataReaderTask = new FutureTask<>(outputReader);
			FutureTask<String> errorReaderTask = new FutureTask<>(errorReader);
			executorService.execute(dataReaderTask);
			executorService.execute(errorReaderTask);

			boolean finished;
			if (waitTime > 0) {
				finished = process.waitFor(waitTime, TimeUnit.MINUTES);
				if (!finished) {
					process.destroyForcibly();
					LOGGER.debug("Process killed due to timeout.");
				}
			}

			dataReaderTask.get();
			errorReaderTask.get();

			process.destroy();
			executorService.shutdown();
			return Files.readString(logFilePath); // Return the log content
		} catch (Exception e) {
			LOGGER.error("Error executing script", e);
			return null;
		}
	}

	/**
	 * Executes a test script with the given command and output file.
	 *
	 * @param command    the command to execute as an array of strings
	 * @param outputFile the name of the file to write the output to
	 */
	@Async
	public void executeTestScript(String[] command, String outputFile) {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // Merge error stream with input stream

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			Process process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				writer.write(line);
				writer.newLine();
			}

			int exitCode = process.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error executing script: " + e.getMessage());
		}
	}
}
