package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.Pair;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class RAVEMonteCarloTreeSearch extends AbstractMonteCarloTreeSearch {
	Map<Pair<MachineState, List<Move>>, Integer> N_bar;
	Map<Pair<MachineState, List<Move>>, Integer> N;
	Map<Pair<MachineState, List<Move>>, Double> Q_bar;
	Map<Pair<MachineState, List<Move>>, Double> Q;
	Map<MachineState, Integer> visits;

	List<MachineState> stateHistory;
	List<List<Move>> jointActionHistory;
	Map<Role, List<List<Move>>> playersActionHistory;

	// Settings
	static double b = 0.00001; // bias parameter

	RAVEMonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		super(sm, a, t);
		System.out.println("RAVE");
		N_bar = new HashMap<Pair<MachineState, List<Move>>, Integer>();
		N = new HashMap<Pair<MachineState, List<Move>>, Integer>();
		Q_bar = new HashMap<Pair<MachineState, List<Move>>, Double>();
		Q = new HashMap<Pair<MachineState, List<Move>>, Double>();
		visits = new HashMap<MachineState, Integer>();
		stateHistory = new ArrayList<MachineState>();
		jointActionHistory = new ArrayList<List<Move>>();
		playersActionHistory = new HashMap<Role, List<List<Move>>>();
		for(Role player: roles){
			playersActionHistory.put(player, new ArrayList<List<Move>>());
		}
	}


	@Override
	public Move getAction(List<Move> moves, MachineState root) throws MoveDefinitionException, TransitionDefinitionException {
		// Search the tree
		MCTS(root);	// Uses timer to terminate

		// Select the best action
		return selectMove(root, false);
	}

	@Override
	public void MCTS(MachineState root) throws MoveDefinitionException, TransitionDefinitionException {
		System.out.println("Simulating...");
		int numDepthCharge = 0;
		while(!timer.isOutOfTime()) {
			stateHistory.clear();
			jointActionHistory.clear();
			for(Role player: roles) playersActionHistory.get(player).clear();

			simTree(root);
			Double score = simDefault();
			backup(score);
			numDepthCharge++;
		}
		System.out.println("Num new depth charges: " + numDepthCharge);
		System.out.println("Total visits to root: " + visits.get(root));
	}

	double evalfn(MachineState state, List<Move> jointAction, double c) throws MoveDefinitionException{
		Pair<MachineState, List<Move>> sa = Pair.of(state, jointAction);
		double beta = N_bar.get(sa)/( N.get(sa) + N_bar.get(sa) + 4*N.get(sa)*N_bar.get(sa)*Math.pow(b, 2) );
		double rave = (1-beta)*Q.get(sa) + beta*(Q_bar.get(sa));
		if (c != 0){
			rave += c*Math.sqrt(2*Math.log(visits.get(state))/N.get(Pair.of(state, jointAction)));
		}
		return rave;
	}

	Pair<List<Move>, Move> selectMoveHelper(MachineState state, double c, boolean verbose) throws MoveDefinitionException{
		List<Move> actions = stateMachine.findLegals(agent, state);
		List<Move> bestJointAction = null;
		Move bestAction = null;
		if(!isMin(state)){	// Argmax
			double bestval = 0;
			for(Move action: actions){
				List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, agent, action);
				for(List<Move> jointAction : jointMoves) {
					double val = evalfn(state, jointAction, c);
					if(val > bestval){
						bestval = val;
						bestAction = action;
						bestJointAction = jointAction;
					}
				}
			}
			if(verbose) System.out.println("Best action value: " + bestval);
			return Pair.of(bestJointAction, bestAction);
		}
		else{				// Argmin
			double bestval = 100;
			for(Move action: actions){
				List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, agent, action);
				for(List<Move> jointAction : jointMoves) {
					double val = evalfn(state, jointAction, -1*c);
					if(val < bestval){
						bestval = val;
						bestAction = action;
						bestJointAction = jointAction;
					}
				}
			}
			return Pair.of(bestJointAction, bestAction);
		}
	}

	List<Move> selectJointMove(MachineState state, boolean useOptimism) throws MoveDefinitionException{
		return selectMoveHelper(state, C, false).left;
	}

	Move selectMove(MachineState state, boolean useOptimism) throws MoveDefinitionException{
		return selectMoveHelper(state, 0, true).right;
	}

	boolean isMin(MachineState state) throws MoveDefinitionException {
		return (stateMachine.findLegals(agent, state).size() == 1);
	}

	boolean isTurn(Role role, MachineState state) throws MoveDefinitionException {
		return (stateMachine.findLegals(role, state).size() > 1);
	}

	boolean newNode(MachineState state) throws MoveDefinitionException{
		visits.put(state, 0);
		List<Move> actions = stateMachine.findLegals(agent, state);
		for(Move action: actions){
			List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, agent, action);
			for(List<Move> jointAction : jointMoves) {
				Pair<MachineState, List<Move>> sa = Pair.of(state, jointAction);
				//Implement heuristic init here. Currently uses even game heuristic
				N_bar.put(sa, 50);
				N.put(sa, 50);
				Q_bar.put(sa, 50.0);
				Q.put(sa, 50.0);
			}
		}
		return true;
	}

	void simTree(MachineState node) throws MoveDefinitionException, TransitionDefinitionException {
		MachineState state = node;
        while(!stateMachine.isTerminal(state)) {
        	if(!visits.containsKey(state)){
        		newNode(state);
        		List<Move> jointAction = stateMachine.getRandomJointMove(state);
        		stateHistory.add(state);
        		jointActionHistory.add(jointAction);
                return;
        	}

        	List<Move> jointAction = selectJointMove(state, true);
        	stateHistory.add(state);
    		jointActionHistory.add(jointAction);
            state = stateMachine.getNextState(state, jointAction);
        }
        return;
	}

	Double simDefault() throws MoveDefinitionException, TransitionDefinitionException {

		MachineState inputState = stateHistory.get(stateHistory.size()-1);
		List<Move> inputJointAction = jointActionHistory.get(jointActionHistory.size()-1);

		MachineState state = stateMachine.getNextState(inputState, inputJointAction);

		// Depth charge
		double reward = 50;
		while(!stateMachine.isTerminal(state)) {
			List<Move> jointAction = stateMachine.getRandomJointMove(state);
			for(Role player: roles){
				if(isTurn(player, state)){	// Only logs playout move if that player had more than one option
					playersActionHistory.get(player).add(jointAction);
				}
			}
            state = stateMachine.getNextState(state, jointAction);
        }

		try{
			reward = stateMachine.findReward(agent, state);
		}
		catch (GoalDefinitionException e){
			System.out.println("GoalDefinitionException in simDefault");
		}

		return reward;
	}

	void backup(double score) {
		for(int t = 0; t<stateHistory.size(); t++){	// For each of the states
			visits.put(stateHistory.get(t), visits.get(stateHistory.get(t)) + 1);
			Pair<MachineState, List<Move>> stat = Pair.of(stateHistory.get(t), jointActionHistory.get(t));
			N.put(stat,  N.get(stat) + 1);
			double deltaQ = (score - Q.get(stat))/N.get(stat);
			Q.put(stat, Q.get(stat) + deltaQ);

			for(Role player: roles){
				List<List<Move>> playerHistory = playersActionHistory.get(player);
				Collection<List<Move>> updatedJointActions = new HashSet<List<Move>>();
				for(List<Move> jointAction: playerHistory){	// For each of the random playout actions ?
					if(!updatedJointActions.contains(jointAction)){
						Pair<MachineState, List<Move>> stau = Pair.of(stateHistory.get(t), jointAction);
						N_bar.put(stau,  N_bar.getOrDefault(stau, 0) + 1);
						deltaQ = (score - Q_bar.getOrDefault(stat, 50.0))/N_bar.get(stat);
						Q_bar.put(stau, Q_bar.get(stat) + deltaQ);
						updatedJointActions.add(jointAction);
					}
				}
			}
		}
	}

}
