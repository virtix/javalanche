/*
 * Copyright (C) 2011 Saarland University
 * 
 * This file is part of Javalanche.
 * 
 * Javalanche is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Javalanche is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with Javalanche.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unisb.cs.st.javalanche.mutation.javaagent.classFileTransfomer;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import de.unisb.cs.st.ds.util.Util;
import de.unisb.cs.st.javalanche.mutation.bytecodeMutations.MutationsCollectorClassAdapter;
import de.unisb.cs.st.javalanche.mutation.javaagent.MutationPreMain;
import de.unisb.cs.st.javalanche.mutation.javaagent.classFileTransfomer.mutationDecision.MutationDecision;
import de.unisb.cs.st.javalanche.mutation.javaagent.classFileTransfomer.mutationDecision.MutationDecisionFactory;
import de.unisb.cs.st.javalanche.mutation.mutationPossibilities.MutationPossibilityCollector;
import de.unisb.cs.st.javalanche.mutation.properties.ConfigurationLocator;
import de.unisb.cs.st.javalanche.mutation.properties.DebugProperties;
import de.unisb.cs.st.javalanche.mutation.results.MutationCoverageFile;
import de.unisb.cs.st.javalanche.mutation.results.persistence.QueryManager;
import de.unisb.cs.st.javalanche.mutation.util.AsmUtil;

public class MutationScanner implements ClassFileTransformer {

	private static Logger logger = Logger.getLogger(MutationScanner.class);

	private MutationPossibilityCollector mpc = new MutationPossibilityCollector();

	private MutationDecision md = MutationDecisionFactory.SCAN_DECISION;



	public MutationScanner() {
		addShutDownHook();
	}

	public static void addShutDownHook() {
		final String projectPrefix = ConfigurationLocator
				.getJavalancheConfiguration().getProjectPrefix();
		Runtime runtime = Runtime.getRuntime();
		final long mutationPossibilitiesPre = QueryManager
				.getNumberOfMutationsWithPrefix(projectPrefix);
		final long numberOfTestsPre = QueryManager.getNumberOfTests();
		runtime.addShutdownHook(new Thread() {
			@Override
			public void run() {
				// lastLineInfo.write();
				String message1 = String.format(
						"Got %d mutation possibilities before run.",
						mutationPossibilitiesPre);
				final long mutationPossibilitiesPost = QueryManager
						.getNumberOfMutationsWithPrefix(projectPrefix);
				String message2 = String.format(
						"Got %d mutation possibilities after run.",
						mutationPossibilitiesPost);
				String message3 = String.format(
						"Added %d mutation possibilities.",
						mutationPossibilitiesPost - mutationPossibilitiesPre);
				long numberOfTests = QueryManager.getNumberOfTestsForProject();
				long addedTests = QueryManager.getNumberOfTests()
						- numberOfTestsPre;
				String testMessage = String
						.format("Added %d tests. Total number of tests for project %s : %d",
								addedTests, projectPrefix,
								numberOfTests);
				long coveredMutations = MutationCoverageFile
						.getNumberOfCoveredMutations();
				String coveredMessage = String
						.format("%d (%.2f %%) mutations are covered by tests.",
								coveredMutations,
								(((double) coveredMutations) / mutationPossibilitiesPost) * 100.);
				System.out.println(message1);
				System.out.println(message2);
				System.out.println(message3);
				System.out.println(testMessage);
				System.out.println(coveredMessage);
			}
		});
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		if (className != null) {
			try {

				String classNameWithDots = className.replace('/', '.');
				logger.debug(classNameWithDots);
				if (md.shouldBeHandled(classNameWithDots)) {

					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

					ClassVisitor cv = cw;
					// cv = new CheckClassAdapter(cw);
					if (DebugProperties.TRACE_BYTECODE) {
						cv = new TraceClassVisitor(cv, new PrintWriter(
								MutationPreMain.sysout));
					}
					cv = new MutationsCollectorClassAdapter(cv, mpc);
					ClassReader cr = new ClassReader(classfileBuffer);
					// cr.accept(cv, ClassReader.EXPAND_FRAMES);
					cr.accept(cv, ClassReader.SKIP_FRAMES);
					classfileBuffer = cw.toByteArray();

					 AsmUtil.checkClass2(classfileBuffer);

					// classfileBuffer = mutationScannerTransformer
					// .transformBytecode(classfileBuffer);
					logger.info(mpc.size()
							+ " mutation possibilities found for class "
							+ className);

					mpc.updateDB();
					mpc.clear();

				} else {
					logger.debug("Skipping class " + className);
				}

			} catch (Throwable t) {
				t.printStackTrace();
				String message = "Exception during instrumentation";
				logger.warn(message, t);
				logger.warn(Util.getStackTraceString());
				System.out.println(message + " - exiting");
				System.exit(1);
			}
		}
		return classfileBuffer;
	}

	// private void computeBytecodeInfo(byte[] classfileBuffer) {
	// ClassReader cr = new ClassReader(classfileBuffer);
	// ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	// LastLineClassAdapter cv = new LastLineClassAdapter(cw, lastLineInfo);
	// cr.accept(cv, ClassReader.EXPAND_FRAMES);
	// }

}
