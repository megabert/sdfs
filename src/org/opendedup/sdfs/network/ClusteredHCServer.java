/*******************************************************************************
 * Copyright (C) 2016 Sam Silverberg sam.silverberg@gmail.com	
 *
 * This file is part of OpenDedupe SDFS.
 *
 * OpenDedupe SDFS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenDedupe SDFS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.opendedup.sdfs.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Config;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.cluster.DSEServerSocket;
import org.opendedup.sdfs.servers.HCServiceProxy;
import org.opendedup.util.OSValidator;

public class ClusteredHCServer implements Daemon {

	// Declaration section:
	// declare a server socket and a client socket for the server
	// declare an input and an output stream

	static DSEServerSocket socket = null;

	// This chat server can accept up to 10 clients' connections

	public static Options buildOptions() {
		Options options = new Options();
		options.addOption("d", false, "debug output");
		options.addOption(
				"c",
				true,
				"sdfs cluster configuration file to start this storage node \ne.g. /etc/sdfs/cluster-dse-cfg.xml");
		options.addOption(
				"rv",
				true,
				"comma separated list of remote volumes that should also be accounted for when doing garbage collection. "
						+ "If not entered the volume will attempt to identify other volumes in the cluster.");
		options.addOption("h", false, "displays available options");
		return options;
	}

	public static void main(String args[]) throws IOException, ParseException {
		checkJavaVersion();
		CommandLineParser parser = new PosixParser();
		Options options = buildOptions();
		CommandLine cmd = parser.parse(options, args);
		ArrayList<String> volumes = new ArrayList<String>();
		if (cmd.hasOption("h")) {
			printHelp(options);
			System.exit(1);
		} else {
			if (!cmd.hasOption("c"))
				printHelp(options);
			else {
				ClusteredShutdownHook shutdownHook = new ClusteredShutdownHook();
				Runtime.getRuntime().addShutdownHook(shutdownHook);
				Main.standAloneDSE = true;
				Main.chunkStoreLocal = true;
				if (OSValidator.isUnix())
					Main.logPath = "/var/log/sdfs/"
							+ new File(cmd.getOptionValue("c")).getName()
							+ ".log";
				boolean debug = cmd.hasOption("d");
				if (debug) {
					SDFSLogger.setLevel(0);
				}
				if (cmd.hasOption("rv")) {
					StringTokenizer st = new StringTokenizer(
							cmd.getOptionValue("rv"), ",");
					while (st.hasMoreTokens()) {
						volumes.add(st.nextToken());
					}
				}
				try {
					Config.parseDSEConfigFile(cmd.getOptionValue("c"));
				} catch (IOException e1) {
					SDFSLogger.getLog().fatal(
							"exiting because of an error with the config file",
							e1);
					e1.printStackTrace();
					System.exit(-1);
				}
				try {
					init(volumes);
				} catch (Exception e) {

					e.printStackTrace();
					SDFSLogger.getLog()
							.fatal("unable to start cluster node", e);
					System.exit(-1);
				}
			}
		}

	}

	ArrayList<String> volumes = new ArrayList<String>();

	private void setup(String[] args) throws ParseException {
		CommandLineParser parser = new PosixParser();
		Options options = buildOptions();
		CommandLine cmd = parser.parse(options, args);

		Main.standAloneDSE = true;
		Main.chunkStoreLocal = true;
		if (OSValidator.isUnix())
			Main.logPath = "/var/log/sdfs/"
					+ new File(cmd.getOptionValue("c")).getName() + ".log";
		boolean debug = cmd.hasOption("d");
		if (debug) {
			SDFSLogger.setLevel(0);
		}
		if (cmd.hasOption("rv")) {
			StringTokenizer st = new StringTokenizer(cmd.getOptionValue("rv"),
					",");
			while (st.hasMoreTokens()) {
				volumes.add(st.nextToken());
			}
		}
		try {
			Config.parseDSEConfigFile(cmd.getOptionValue("c"));
			Main.DSEID = Main.DSEClusterMemberID;
		} catch (IOException e1) {
			SDFSLogger.getLog().fatal(
					"exiting because of an error with the config file", e1);
			e1.printStackTrace();
			System.exit(-1);
		}
		try {

		} catch (Exception e) {

			e.printStackTrace();
			SDFSLogger.getLog().fatal("unable to start cluster node", e);
			System.exit(-1);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp("Usage: ClusteredHCServer -c <configFile>", options);
	}

	private static void checkJavaVersion() {
		Properties sProp = java.lang.System.getProperties();
		String sVersion = sProp.getProperty("java.version");
		sVersion = sVersion.substring(0, 3);
		Float f = Float.valueOf(sVersion);
		if (f.floatValue() < (float) 1.8) {
			System.out.println("Java version must be 1.7 or newer");
			System.out
					.println("To get Java 7 go to https://jdk7.dev.java.net/");
			System.exit(-1);
		}
	}

	public static void init(ArrayList<String> volumes) throws Exception {
		HCServiceProxy.init(volumes);
		// Initialization section:
		// Try to open a server socket on port port_number (default 2222)
		// Note that we can't choose a port less than 1023 if we are not
		// privileged users (root)

		socket = new DSEServerSocket(Main.DSEClusterConfig, Main.DSEClusterID,
				Main.DSEClusterMemberID, volumes);
		HCServiceProxy.cs = socket;
	}

	public static void close() {
		try {
			System.out.println("#### Shutting Down Network Service ####");
		} catch (Exception e) {
		}
		System.out.println("#### Shutting down HashStore ####");
		HCServiceProxy.close();
		System.out.println("#### Shut down completed ####");
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		checkJavaVersion();
		setup(arg0.getArguments());

	}

	@Override
	public void start() throws Exception {
		init(volumes);

	}

	@Override
	public void stop() throws Exception {
		try {
			System.out.println("#### Shutting Down Network Service ####");
		} catch (Exception e) {
		}
		System.out.println("#### Shutting down HashStore ####");
		HCServiceProxy.close();
		System.out.println("#### Shut down completed ####");
		try {
			System.out.println("#### Shutting down StorageHub ####");

			ClusteredHCServer.close();
			System.out.println("#### Shut down StorageHub ####");
		} catch (Exception e) {

		}

	}
}

class ClusteredShutdownHook extends Thread {
	@Override
	public void run() {
		System.out.println("#### Shutting down StorageHub ####");

		ClusteredHCServer.close();
	}
}