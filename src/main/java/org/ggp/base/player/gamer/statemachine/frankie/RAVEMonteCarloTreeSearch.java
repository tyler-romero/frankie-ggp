package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	static double b = 0.001; // bias parameter

	RAVEMonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		super(sm, a, t);
		System.out.println("RAVEMonteCarloTreeSearch");
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
		C = 1;
	}

	@Override
	public void metaGame(MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException{
		MCTS(currentState);
	}


	@Override
	public Move getAction(List<Move> moves, MachineState root) throws MoveDefinitionException, TransitionDefinitionException {
		// Search the tree
		MCTS(root);	// Uses timer to terminate

		// Select the best action
		return selectMove(root);
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
			if(verbose) System.out.println("RAVE Best max action value: " + bestval);
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
			if(verbose) System.out.println("RAVE Best min action value: " + bestval);
			return Pair.of(bestJointAction, bestAction);
		}
	}

	List<Move> selectJointMove(MachineState state) throws MoveDefinitionException{
		return selectMoveHelper(state, C, false).left;
	}

	Move selectMove(MachineState state) throws MoveDefinitionException{
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
				N_bar.put(sa, 1);
				N.put(sa, 1);
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

        	List<Move> jointAction = selectJointMove(state);
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


// Uses root parallelism, so there is no synchronization
class MultiThreadedRAVEMonteCarloTreeSearch extends MonteCarloTreeSearch {
	// Depending on the game could be faster or slower than single threaded version.
	static int nThreads;
	List<StateMachine> machines;

	List<Map<Pair<MachineState, List<Move>>, Integer>> n_bar_list;
	List<Map<Pair<MachineState, List<Move>>, Integer>> n_list;
	List<Map<Pair<MachineState, List<Move>>, Double>> q_bar_list;
	List<Map<Pair<MachineState, List<Move>>, Double>> q_list;
	List<Map<MachineState, Integer>> v_list;

	// Settings
	static double b = 0.00001; // bias parameter

	// Thread pooling
	ExecutorService executor;
	CompletionService<Integer> completionService;

	MultiThreadedRAVEMonteCarloTreeSearch(StateMachine sm, Role a, Timer t, List<StateMachine> m) {
		super(sm, a, t);
		System.out.println("MultiThreadedRAVEMonteCarloTreeSearch");
		machines = m;
		nThreads = machines.size();

		n_bar_list = new ArrayList<Map<Pair<MachineState, List<Move>>, Integer>>(nThreads);
		n_list = new ArrayList<Map<Pair<MachineState, List<Move>>, Integer>>(nThreads);
		q_bar_list = new ArrayList<Map<Pair<MachineState, List<Move>>, Double>>(nThreads);
		q_list = new ArrayList<Map<Pair<MachineState, List<Move>>, Double>>(nThreads);
		v_list = new ArrayList<Map<MachineState, Integer>>(nThreads);

		// Init all of the lists
		for(int i = 0; i<nThreads; i++){
			n_bar_list.add(new HashMap<Pair<MachineState, List<Move>>, Integer>());
		}
		for(int i = 0; i<nThreads; i++){
			n_list.add(new HashMap<Pair<MachineState, List<Move>>, Integer>());
		}
		for(int i = 0; i<nThreads; i++){
			q_bar_list.add(new HashMap<Pair<MachineState, List<Move>>, Double>());
		}
		for(int i = 0; i<nThreads; i++){
			q_list.add(new HashMap<Pair<MachineState, List<Move>>, Double>());
		}
		for(int i = 0; i<nThreads; i++){
			v_list.add(new HashMap<MachineState, Integer>());
		}

		executor = Executors.newFixedThreadPool(nThreads);
		completionService = new ExecutorCompletionService<Integer>(executor);

		C = 40;
	}

	@Override
	public Move getAction(List<Move> moves, MachineState root) throws MoveDefinitionException, TransitionDefinitionException {
		// Search the tree
		MCTS(root);	// Uses timer to terminate

		// Select the best action
		return selectMove(root);
	}

	@Override
	public void MCTS(MachineState root) {
		System.out.println("Simulating...");

		// Start depth charges
		for(int i = 0; i<nThreads; i++){
			RAVEThread raveThread = new RAVEThread(machines.get(i), agent, timer, root, C,
					n_bar_list.get(i), n_list.get(i), q_bar_list.get(i), q_list.get(i), v_list.get(i));
			completionService.submit(raveThread);
		}

		// Wait for threads to end and accumulate rewards
		try {
			int numDepthCharges = 0;
            for (int i = 0;  i < nThreads; i++) {
                Future<Integer> fRave = completionService.take();	//take is a blocking method
                numDepthCharges += fRave.get();
            }
            System.out.println("Number of new simulations " + numDepthCharges);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
	}

	private int getTotalN(Pair<MachineState, List<Move>> sa){
		int N = 0;
		for(int i = 0; i<nThreads; i++){
			N += n_list.get(i).getOrDefault(sa, 1);
		}
		return N;
	}

	private int getTotalN_bar(Pair<MachineState, List<Move>> sa){
		int N_bar = 0;
		for(int i = 0; i<nThreads; i++){
			N_bar += n_bar_list.get(i).getOrDefault(sa, 1);
		}
		return N_bar;
	}

	private double getAvgQ_bar(Pair<MachineState, List<Move>> sa){
		// Uses a weighted average
		Double Q_bar = 0.0;
		int N_bar = 0;
		for(int i = 0; i<nThreads; i++){
			Q_bar += q_bar_list.get(i).getOrDefault(sa, 50.0) * n_bar_list.get(i).getOrDefault(sa, 1);
			N_bar += n_bar_list.get(i).getOrDefault(sa, 1);
		}
		Q_bar = Q_bar/N_bar;
		return Q_bar;
	}

	private double getAvgQ(Pair<MachineState, List<Move>> sa){
		// Uses a weighted average
		Double Q = 0.0;
		int N = 0;
		for(int i = 0; i<nThreads; i++){
			Q += q_list.get(i).getOrDefault(sa, 50.0) * n_list.get(i).getOrDefault(sa, 1);
			N += n_list.get(i).getOrDefault(sa, 1);
		}
		Q = Q/N;
		return Q;
	}

	double evalfn(MachineState state, List<Move> jointAction, double c) throws MoveDefinitionException{
		Pair<MachineState, List<Move>> sa = Pair.of(state, jointAction);
		Integer N_bar = getTotalN_bar(sa);
		Integer N = getTotalN(sa);
		Double Q_bar = getAvgQ_bar(sa);
		Double Q = getAvgQ(sa);

		double beta = N_bar/( N + N_bar + 4*N*N_bar*Math.pow(b, 2) );
		double rave = (1-beta)*Q + beta*(Q_bar);
		System.out.println("RAVE: " + rave + "\tN: " + N);
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
			if(verbose) System.out.println("MTRAVE Best action value: " + bestval);
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

	Move selectMove(MachineState state) throws MoveDefinitionException{
		return selectMoveHelper(state, 0, true).right;
	}

	boolean isMin(MachineState state) throws MoveDefinitionException {
		return (stateMachine.findLegals(agent, state).size() == 1);
	}

	@Override
	public void metaGame(MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException{
		MCTS(currentState);
	}
}
