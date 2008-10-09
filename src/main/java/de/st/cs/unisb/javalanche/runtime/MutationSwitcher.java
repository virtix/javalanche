package de.st.cs.unisb.javalanche.runtime;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import de.st.cs.unisb.javalanche.javaagent.MutationForRun;
import de.st.cs.unisb.javalanche.properties.MutationProperties;
import de.st.cs.unisb.javalanche.results.Mutation;
import de.st.cs.unisb.javalanche.results.persistence.QueryManager;

/**
 * Class handles the activation and deactivation of the mutations during
 * runtime.
 *
 * @author David Schuler
 *
 */
public class MutationSwitcher {

	private static Logger logger = Logger.getLogger(MutationSwitcher.class);

	/**
	 * The mutations that are activated in this run
	 */
	private Collection<Mutation> mutations;

	private Iterator<Mutation> iter;

	/**
	 * Holds the currently activated mutation.
	 */
	private Mutation actualMutation;

	private long mutationStartTime;

	private void initMutations() {
		if (mutations == null) {
			mutations = MutationForRun.getInstance().getMutations();
			logger.info(mutations);
			iter = mutations.iterator();
		} else {
			throw new RuntimeException("Already initialized");
		}
	}

	/**
	 * Checks if there is a mutation to apply.
	 *
	 * @return True, if next() will return a mutation.
	 */
	public boolean hasNext() {
		if (iter == null) {
			initMutations();
		}
		return iter.hasNext();
	}

	/**
	 * Takes the next mutation without a result and sets it as the actual
	 * mutation.
	 *
	 * @return The mutation that is now the actual mutation.
	 */
	public Mutation next() {
		if (iter == null) {
			initMutations();
		}
		while (iter.hasNext()) {
			actualMutation = iter.next();
			if (actualMutation.getMutationResult() == null) {
				return actualMutation;
			} else {
				logger.info("Mutation already got Results");
			}
		}
		return actualMutation;
	}

	/**
	 * Turns the actual mutation on.
	 */
	public void switchOn() {
		if (actualMutation != null) {
			logger.info("enabling mutation: "
					+ actualMutation.getMutationVariable() + " in line "
					+ actualMutation.getLineNumber() + " - "
					+ actualMutation.toString());
			mutationStartTime = System.currentTimeMillis();
			System.setProperty(actualMutation.getMutationVariable(), "1");
			System.setProperty(MutationProperties.ACTUAL_MUTATION_KEY,
					actualMutation.getId() + "");

		}
	}

	/**
	 * Turns the actual mutation off.
	 */
	public void switchOff() {
		if (actualMutation != null) {
			System.clearProperty(actualMutation.getMutationVariable());
			System.clearProperty(MutationProperties.ACTUAL_MUTATION_KEY);
			mutationStartTime = System.currentTimeMillis() - mutationStartTime;
			logger.info("Disabling mutation: "
					+ actualMutation.getMutationVariable() + " Time needed "
					+ mutationStartTime);
			actualMutation = null;
		}
	}

	/**
	 * @return The test cases that cover the actual activated mutation.
	 */
	public Set<String> getTests() {
		return QueryManager.getTestsCollectedData(actualMutation);
	}
}