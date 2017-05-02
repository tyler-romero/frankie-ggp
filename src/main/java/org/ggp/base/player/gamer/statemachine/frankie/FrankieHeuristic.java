package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public abstract class FrankieHeuristic {
	protected double weight;

	public FrankieHeuristic(double initialWeight) {
		weight = initialWeight;
	}

	public void updateWeight(double newWeight) {weight = newWeight;}
	public double getWeight() {return weight;}

	abstract double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException;
}


class AgentMobilityHeuristic extends FrankieHeuristic {

	public AgentMobilityHeuristic(double initialWeight) {
		super(initialWeight);
		// TODO Auto-generated constructor stub
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException {
		List<Move> legal_actions = stateMachine.getLegalMoves(state, role);
		List<Move> all_actions = stateMachine.findActions(role);
		return legal_actions.size()/all_actions.size() * 100 * weight;
	}

}


class AgentFocusHeuristic extends FrankieHeuristic {

	public AgentFocusHeuristic(double initialWeight) {
		super(initialWeight);
		// TODO Auto-generated constructor stub
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException {
		List<Move> legal_actions = stateMachine.getLegalMoves(state, role);
		List<Move> all_actions = stateMachine.findActions(role);
		return (100 - legal_actions.size()/all_actions.size() * 100) * weight;
	}

}


class OppMobilityHeuristic extends FrankieHeuristic {

	public OppMobilityHeuristic(double initialWeight) {
		super(initialWeight);
		// TODO Auto-generated constructor stub
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException {
		int total_legal_actions = 0;
		int total_all_actions = 0;
		for (Role r : stateMachine.getRoles()){
			if (r.equals(role))
				continue;
			total_legal_actions = total_legal_actions + stateMachine.getLegalMoves(state, r).size();
			total_all_actions = total_all_actions + stateMachine.findActions(r).size();
		}
		return total_legal_actions/total_all_actions * 100 * weight;
	}
}


class OppFocusHeuristic extends FrankieHeuristic {

	public OppFocusHeuristic(double initialWeight) {
		super(initialWeight);
		// TODO Auto-generated constructor stub
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException {
		int total_legal_actions = 0;
		int total_all_actions = 0;
		for (Role r : stateMachine.getRoles()){
			if (r.equals(role))
				continue;
			total_legal_actions = total_legal_actions + stateMachine.getLegalMoves(state, r).size();
			total_all_actions = total_all_actions + stateMachine.findActions(r).size();
		}
		return (100 - total_legal_actions/total_all_actions * 100) * weight;
	}
}


class GoalProximityHeuristic extends FrankieHeuristic {

	public GoalProximityHeuristic(double initialWeight) {
		super(initialWeight);
		// TODO Auto-generated constructor stub
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException {
		MachineState terminalState = stateMachine.performDepthCharge(state, null);	// Does this actually find a desireable terminal state? Does it matter?
		int terminalReward = 0;

		try{
			terminalReward = stateMachine.findReward(role, terminalState);
		}
		catch (GoalDefinitionException e){
			System.out.println("GoalDefinitionException in goalProx");
		}

		Set<GdlSentence> terminalGDL = terminalState.getContents();
		Set<GdlSentence> currentGDL = state.getContents();
		Set<GdlSentence> missingGDL = terminalGDL;
		missingGDL.removeAll(currentGDL);
		float fracTerm = (terminalGDL.size()-missingGDL.size())/terminalGDL.size();
		return terminalReward * fracTerm * weight;
	}
}


class MonteCarloHeuristic extends FrankieHeuristic {

	private int monte_carlo_count;

	public MonteCarloHeuristic(double initialWeight, int count) {
		super(initialWeight);
		monte_carlo_count = count;
	}

	@Override
	double value(StateMachine stateMachine, Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException {
		int total = 0;
		for(int i = 0; i < monte_carlo_count; i++){
			MachineState mcFinalState = stateMachine.performDepthCharge(state, null);
			try{
				total += stateMachine.findReward(role, mcFinalState);
			}
			catch (GoalDefinitionException e){
				System.out.println("GoalDefinitionException in Monte Carlo");
				total += 0;
			}
		}
		return total/monte_carlo_count * weight;
	}
}













