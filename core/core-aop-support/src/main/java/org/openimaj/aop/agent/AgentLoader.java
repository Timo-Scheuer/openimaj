/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.aop.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * Dynamic agent loader. Provides methods to extract an agent jar with a
 * specific agent class, and to attempt to dynamically load agents on Oracle
 * JVMs (including OpenJDK). Dynamic loading won't work on all JVMs (i.e. IBMs),
 * but the standard "-javaagent" commandline option can be used instead. If
 * dynamic loading fails, instructions on using the command line flag will be
 * printed to stderr.
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class AgentLoader {
	private static final Logger LOGGER = LogManager.getLogger(AgentLoader.class);

	private static long copy(InputStream input, OutputStream output) throws IOException {
		long count = 0;
		int n = 0;
		final byte[] buffer = new byte[4096];

		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}

		return count;
	}

	private static byte[] createManifest(Class<?> agentClass) {
		final StringBuffer sb = new StringBuffer();

		try {
			agentClass.getDeclaredMethod("premain", String.class, Instrumentation.class);
			sb.append("Premain-Class: " + agentClass.getName() + "\n");
		} catch (final NoSuchMethodException e) {
			// IGNORE//
		}

		try {
			agentClass.getDeclaredMethod("agentmain", String.class, Instrumentation.class);
			sb.append("Agent-Class: " + agentClass.getName() + "\n");
		} catch (final NoSuchMethodException e) {
			// IGNORE//
		}

		sb.append("Can-Redefine-Classes: true\n");
		sb.append("Can-Retransform-Classes: true\n");

		try {
			return sb.toString().getBytes("US-ASCII");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException("Charset US-ASCII isn't supported!! This should never happen.");
		}
	}

	/**
	 * Create an agent jar file with the required manifest entries.
	 *
	 * @param file
	 *            the location to create the jar
	 * @param agentClass
	 *            the agent class
	 * @throws IOException
	 *             if an error occurs
	 */
	public static void createAgentJar(File file, Class<?> agentClass) throws IOException {
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(file));

		String classEntryPath = agentClass.getName().replace(".", "/") + ".class";
		final InputStream classStream = agentClass.getClassLoader().getResourceAsStream(classEntryPath);

		if (classEntryPath.startsWith("/"))
			classEntryPath = classEntryPath.substring(1);

		JarEntry entry = new JarEntry(classEntryPath);
		jos.putNextEntry(entry);
		copy(classStream, jos);
		jos.closeEntry();

		entry = new JarEntry("META-INF/MANIFEST.MF");
		jos.putNextEntry(entry);
		jos.write(createManifest(agentClass));
		jos.closeEntry();

		jos.close();
	}

	/**
	 * Attempt to dynamically load the given agent class
	 *
	 * @param agentClass
	 *            the agent class
	 * @throws IOException
	 *             if an error occurs creating the agent jar
	 */
	public static void loadAgent(Class<?> agentClass) throws IOException {
		final File tmp = File.createTempFile("agent", ".jar");
		tmp.deleteOnExit();
		createAgentJar(tmp, agentClass);

		final String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		final int p = nameOfRunningVM.indexOf('@');
		final String pid = nameOfRunningVM.substring(0, p);

		try {
			VirtualMachine vm = VirtualMachine.attach(pid);
			try {
				vm.loadAgent(tmp.getAbsolutePath());
			} catch (AgentLoadException e) {
				LOGGER.error("Agent does not exist or cannot be started", e);
			} catch (AgentInitializationException e) {
				LOGGER.error("Agent initialization failed", e);
			} finally {
				vm.detach();
			}
		} catch (AttachNotSupportedException e) {
			LOGGER.error("JDK does not support loading the java agent dynamically");
		} catch (IOException e) {
			LOGGER.error("Unable to load the java agent dynamically", e);
		}
	}
}
