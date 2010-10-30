/**
 * Copyright (c) 2009, 2010, Barthelemy Dagenais All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * - The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package py4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import py4j.commands.ArrayCommand;
import py4j.commands.CallCommand;
import py4j.commands.Command;
import py4j.commands.ConstructorCommand;
import py4j.commands.FieldCommand;
import py4j.commands.HelpPageCommand;
import py4j.commands.JVMViewCommand;
import py4j.commands.ListCommand;
import py4j.commands.MemoryCommand;
import py4j.commands.ReflectionCommand;
import py4j.commands.ShutdownGatewayServerCommand;

/**
 * <p>
 * Manage the connection between a Python program and a Gateway. A
 * GatewayConnection lives in its own thread and is created on demand (e.g., one
 * per concurrent thread).
 * </p>
 * 
 * <p>
 * The request to connect to the JVM goes through the {@link py4j.GatewayServer
 * GatewayServer} first and is then passed to a GatewayConnection.
 * </p>
 * 
 * <p>
 * This class is not intended to be directly accessed by users.
 * </p>
 * 
 * 
 * @author Barthelemy Dagenais
 * 
 */
public class GatewayConnection implements Runnable {

	private final static List<Class<? extends Command>> baseCommands;
	private final Gateway gateway;
	private final Socket socket;
	private final BufferedWriter writer;
	private final BufferedReader reader;
	private final Map<String, Command> commands;
	private final Logger logger = Logger.getLogger(GatewayConnection.class
			.getName());

	static {
		baseCommands = new ArrayList<Class<? extends Command>>();
		baseCommands.add(ArrayCommand.class);
		baseCommands.add(CallCommand.class);
		baseCommands.add(ConstructorCommand.class);
		baseCommands.add(FieldCommand.class);
		baseCommands.add(HelpPageCommand.class);
		baseCommands.add(ListCommand.class);
		baseCommands.add(MemoryCommand.class);
		baseCommands.add(ReflectionCommand.class);
		baseCommands.add(ShutdownGatewayServerCommand.class);
		baseCommands.add(JVMViewCommand.class);
	}

	public GatewayConnection(Gateway gateway, Socket socket,
			List<Class<? extends Command>> customCommands) throws IOException {
		super();
		this.gateway = gateway;
		this.socket = socket;
		this.reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream(), Charset.forName("UTF-8")));
		this.writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream(), Charset.forName("UTF-8")));
		this.commands = new HashMap<String, Command>();
		initCommands(gateway, baseCommands);
		if (customCommands != null) {
			initCommands(gateway, customCommands);
		}
		Thread t = new Thread(this);
		t.start();
	}

	public GatewayConnection(Gateway gateway, Socket socket) throws IOException {
		this(gateway, socket, null);
	}

	/**
	 * 
	 * @return The list of base commands that are provided by default. Can be
	 *         hidden by custom commands with the same command id by passing a
	 *         list of custom commands to the {@link py4j.GatewayServer
	 *         GatewayServer}.
	 */
	public static List<Class<? extends Command>> getBaseCommands() {
		return baseCommands;
	}

	/**
	 * <p>
	 * Override this method to initialize custom commands.
	 * </p>
	 * 
	 * @param gateway
	 */
	protected void initCommands(Gateway gateway,
			List<Class<? extends Command>> commandsClazz) {
		for (Class<? extends Command> clazz : commandsClazz) {
			try {
				Command cmd = clazz.newInstance();
				cmd.init(gateway);
				commands.put(cmd.getCommandName(), cmd);
			} catch (Exception e) {
				String name = "null";
				if (clazz != null) {
					name = clazz.getName();
				}
				logger.log(Level.SEVERE,
						"Could not initialize command " + name, e);
			}
		}
	}

	@Override
	public void run() {
		try {
			logger.info("Gateway Connection ready to receive messages");
			String commandLine = null;
			do {
				commandLine = reader.readLine();
				logger.info("Received command: " + commandLine);
				Command command = commands.get(commandLine);
				if (command != null) {
					command.execute(commandLine, reader, writer);
				} else {
					logger.log(Level.WARNING, "Unknown command " + commandLine);
				}
			} while (commandLine != null && !commandLine.equals("q"));
		} catch (Exception e) {
			logger.log(Level.WARNING,
					"Error occurred while waiting for a command.", e);
		} finally {
			logger.log(Level.INFO, "Closing connection.");
			// NetworkUtil.quietlyClose(writer);
			// NetworkUtil.quietlyClose(reader);
			NetworkUtil.quietlyClose(socket);
			gateway.closeConnection();
		}
	}

}