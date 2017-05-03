package org.ggp.base.player.gamer.statemachine.frankie;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public final class FrankieEvaluationFunction {
	private List<FrankieHeuristic> heuristicList;
	private StateMachine stateMachine;

	private boolean assertValidWeights() {
		double totalWeight = 0.0;
		for(FrankieHeuristic h : heuristicList){
			totalWeight += h.getWeight();
		}
		boolean areValidWeights = totalWeight <= 1.0;
		assert(areValidWeights);
		return areValidWeights;
	}

	FrankieEvaluationFunction(StateMachine SM, List<FrankieHeuristic> heurList) {
		stateMachine = SM;
		heuristicList = heurList;
		assertValidWeights();
	}

	public int value(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException {
		double value = 0.0;
		for(FrankieHeuristic h : heuristicList){
			if (h.getWeight() == 0) continue;	// Dont waste time, if the heuristic doesn't matter
			value += h.value(stateMachine, role, state);
		}
		return (int) value;
	}

	public void reweight(List<Double> newWeights) {
		assert(newWeights.size() == heuristicList.size());
		for(int i = 0; i < heuristicList.size(); i++){
			heuristicList.get(i).updateWeight(newWeights.get(i)); //Double check this
		}
		assertValidWeights();
	}

	public void randomizeWeights() {
		Random r = new Random();
		List<Double> newWeights = new ArrayList<Double>();

		// Get list of random numbers
		double total = 0.0;
		for(int i = 0; i < heuristicList.size(); i++){
			newWeights.add(r.nextDouble());
			total += newWeights.get(i);
		}

		// Normalize
		for(int i = 0; i < newWeights.size(); i++){
			newWeights.set(i, newWeights.get(i)/total);
			total += newWeights.get(i);
		}

		// Set
		reweight(newWeights);
	}

	public void save(String fileName) throws FileNotFoundException {
	    PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
	    for (FrankieHeuristic h : heuristicList)
	        pw.println(h.getWeight());
	    pw.close();
	}

	public void load(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		ArrayList<Double> weights = new ArrayList<Double>();

		// Read in weights
		while ((line = br.readLine()) != null) {
			Double d = Double.parseDouble(line);
			weights.add(d);
		}
		br.close();

		// Make sure weights are the right size
		assert(weights.size() == heuristicList.size());

		// Set weights
		for(int i = 0; i<heuristicList.size(); i++) {
			heuristicList.get(i).updateWeight(weights.get(i));
		}

	}
}
