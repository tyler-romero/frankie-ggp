package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;

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
}
