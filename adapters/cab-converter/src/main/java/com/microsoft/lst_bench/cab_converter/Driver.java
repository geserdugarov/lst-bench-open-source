/*
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.lst_bench.cab_converter;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Driver {

  private static final Logger LOGGER = LoggerFactory.getLogger(Driver.class);

  private static final String OPT_INPUT_CAB_STREAMS_DIR = "cab-streams-dir";
  private static final String OPT_OUTPUT_DIR = "output-dir";
  private static final String OPT_INPUT_SPLIT_STREAMS = "split-read-write-streams";
  private static final String OPT_INPUT_CONNECTIONS_GEN_MODE = "connections-gen-mode";

  /** Defeat instantiation. */
  private Driver() {}

  /** Main method. */
  public static void main(String[] args) throws Exception {
    String inputCABStreamsDir = null;
    String outputDir = null;
    String inputSplitReadWriteStreams = null;
    String inputConnectionsGenMode = null;

    // Retrieve program input values
    final Options options = createOptions();
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine cmd = parser.parse(options, args);
      if (cmd.getOptions().length == 0) {
        usageAndHelp();
      } else {
        inputCABStreamsDir = cmd.getOptionValue(OPT_INPUT_CAB_STREAMS_DIR, (String) null);
        outputDir = cmd.getOptionValue(OPT_OUTPUT_DIR, (String) null);
        inputSplitReadWriteStreams = cmd.getOptionValue(OPT_INPUT_SPLIT_STREAMS, "false");
        inputConnectionsGenMode = cmd.getOptionValue(OPT_INPUT_CONNECTIONS_GEN_MODE, "single");
      }
    } catch (MissingOptionException | UnrecognizedOptionException e) {
      usageAndHelp();
      return;
    }

    // Validate input values
    Validate.notNull(inputCABStreamsDir, "CAB streams directory is required.");
    File streamsDir = new File(inputCABStreamsDir);
    if (!streamsDir.exists() || !streamsDir.isDirectory()) {
      throw new IllegalArgumentException(
          "The provided streams directory path is invalid: " + inputCABStreamsDir);
    }
    Validate.notNull(outputDir, "Output directory is required.");
    File dir = new File(outputDir);
    if (dir.exists() && !dir.isDirectory()) {
      throw new IllegalArgumentException(
          "The provided output directory path is invalid: " + outputDir);
    }
    if (!inputSplitReadWriteStreams.equalsIgnoreCase("true")
        && !inputSplitReadWriteStreams.equalsIgnoreCase("false")) {
      throw new IllegalArgumentException(
          "The inputSplitStreams value must be 'true' or 'false'. Provided: "
              + inputSplitReadWriteStreams);
    }
    if (!inputConnectionsGenMode.equalsIgnoreCase("single")
        && !inputConnectionsGenMode.equalsIgnoreCase("per-db")
        && !inputConnectionsGenMode.equalsIgnoreCase("per-stream")
        && !inputConnectionsGenMode.equalsIgnoreCase("per-stream-type")) {
      throw new IllegalArgumentException(
          "The inputConnectionsGenMode value must be one of 'single', 'per-db', 'per-stream', or 'per-stream-type'. Provided: "
              + inputConnectionsGenMode);
    }

    // Run the converter
    final Converter converter =
        new Converter(
            streamsDir,
            dir,
            Boolean.parseBoolean(inputSplitReadWriteStreams),
            parseConnectionGenMode(inputConnectionsGenMode));
    converter.execute();
  }

  private static Options createOptions() {
    final Options options = new Options();

    final Option inputCABStreamsDirOption =
        Option.builder()
            .required()
            .option("d")
            .longOpt(OPT_INPUT_CAB_STREAMS_DIR)
            .hasArg()
            .argName("directory")
            .desc("Path to the directory containing the query streams generated by CAB-gen")
            .build();
    options.addOption(inputCABStreamsDirOption);

    final Option outputDirOption =
        Option.builder()
            .required()
            .option("o")
            .longOpt(OPT_OUTPUT_DIR)
            .hasArg()
            .argName("directory")
            .desc(
                "Path to the directory where the output files from the CAB conversion will be saved")
            .build();
    options.addOption(outputDirOption);

    final Option inputInputSplitStreams =
        Option.builder()
            .required(false)
            .option("s")
            .longOpt(OPT_INPUT_SPLIT_STREAMS)
            .hasArg()
            .argName("boolean")
            .desc(
                "Whether to split each input query stream into separate read/write streams (default: false)")
            .build();
    options.addOption(inputInputSplitStreams);

    final Option inputConnectionsGenModeOption =
        Option.builder()
            .required(false)
            .option("c")
            .longOpt(OPT_INPUT_CONNECTIONS_GEN_MODE)
            .hasArg()
            .argName("mode")
            .desc(
                "Connection generation mode. Options: 'single' (a single connection for all streams), "
                    + "'per-db' (one connection per target database), 'per-stream' (one connection per stream), "
                    + "'per-stream-type' (one connection per stream type, i.e., read/write) (default: 'single')")
            .build();
    options.addOption(inputConnectionsGenModeOption);

    return options;
  }

  private static void usageAndHelp() {
    // Print usage and help
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(120);
    formatter.printHelp("./cab-converter.sh", createOptions(), true);
  }

  private static ConnectionGenMode parseConnectionGenMode(String modeValue) {
    switch (modeValue.toLowerCase()) {
      case "single":
        return ConnectionGenMode.SINGLE;
      case "per-db":
        return ConnectionGenMode.PER_DB;
      case "per-stream":
        return ConnectionGenMode.PER_STREAM;
      case "per-stream-type":
        return ConnectionGenMode.PER_STREAM_TYPE;
      default:
        throw new IllegalArgumentException("Invalid connection generation mode: " + modeValue);
    }
  }
}
