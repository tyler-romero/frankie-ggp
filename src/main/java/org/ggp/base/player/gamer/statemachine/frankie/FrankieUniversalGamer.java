package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieUniversalGamer extends FrankieGamer {

	private Map<Role, Integer> roleMap;
	private Role agent;
	private List<Role> roles;
	private int numRoles;
	private int iterative_deepening_search_depth;
	private boolean isSinglePlayer;
	private boolean did_timeout;
	private StateMachine stateMachine;
	private long finishBy;
	private long buffer;
	private FrankieEvaluationFunction evalFn;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println(getName());	// Print the agent's name

		stateMachine = getStateMachine();
		roleMap = stateMachine.getRoleIndices();
		agent = getRole();
		roles = stateMachine.getRoles();
		numRoles = roles.size();

		// Configure Settings
		// TODO: Learn this from experience
		// TODO: Set search depth based on initial branching factor
		buffer = 3000;

		// Determine if game is single-player or multi-player and init heuristics
		if(roles.size() > 1){
			isSinglePlayer = false;
			System.out.println("Multi-Player Game");
			iterative_deepening_search_depth = 3;
		}
		else if(roles.size() == 1){
			isSinglePlayer = true;
			System.out.println("Single-Player Game");
			iterative_deepening_search_depth = 6;
		}
		else assert(true);

		// Set up heuristics
		List<FrankieHeuristic> heuristics = new ArrayList<FrankieHeuristic>();
		heuristics.add( new MonteCarloHeuristic(1, 4) );
		heuristics.add( new GoalProximityHeuristic(0) );
		heuristics.add( new AgentMobilityHeuristic(0) );
		heuristics.add( new OppMobilityHeuristic(0) );
		heuristics.add( new AgentFocusHeuristic(0) );
		heuristics.add( new OppFocusHeuristic(0) );
		evalFn = new FrankieEvaluationFunction(stateMachine, heuristics);
	}

	// ----- Helper Functions ----- //
	private boolean isOutOfTime(){
		long currentTime = System.currentTimeMillis();
		if(currentTime > finishBy) {
			did_timeout = true;
			return true;
		}
		else return false;
	}

	// ----- Alpha Beta Pruning ----- //
	private int minScoreAlphaBeta(Move agentMove, MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		List<List<Move>> all_legal_moves = stateMachine.getLegalJointMoves(state, agent, agentMove);

		for(List<Move> moves : all_legal_moves) {
			int result = 0;

			MachineState nextState = stateMachine.getNextState(state, moves);
			result = maxScoreAlphaBeta(agent, nextState, alpha, beta, depth + 1);

			beta = java.lang.Math.min(beta, result);
			if(beta == 0) {
				return beta;
			}
			if (beta <= alpha){
				return alpha;
			}
		}
		return beta;
	}

	private int maxScoreAlphaBeta(Role role, MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		if (depth >= iterative_deepening_search_depth) {
			return evalFn.value(role, state);
		}
		if (isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {

			int result = minScoreAlphaBeta(move, state, alpha, beta, depth);
			alpha = java.lang.Math.max(alpha, result);

			if(alpha == 100){
				return alpha;
			}
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private Move alphaBeta(List<Move> moves)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		Move action = moves.get(0);

		// Go through all of the legal moves
		int score = 0;
		for(Move move : moves) {

			int alpha = 0;
			int beta = 100;

			int result = minScoreAlphaBeta(move, getCurrentState(), alpha, beta, 0);

			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				return action;
			}
		}

		return action;
	}

	// ----- Compulsive Deliberation ----- //
	private int maxScoreDeliberation(Role role, MachineState state, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		if (depth >= iterative_deepening_search_depth) {
			return evalFn.value(role, state);
		}
		if (isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, role);

		int score = 0;
		for(Move move : moves) {
			List<Move> moveList = new ArrayList<Move>();
			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(state, moveList);

			int result = maxScoreDeliberation(role, nextState, depth + 1);

			if (result == 100) {
				score = result;
				return score;
			}
			if (result > score) {
				score = result;
			}
		}

		return score;
	}

	private Move compulsiveDeliberation(List<Move> moves)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException  {
		assert(stateMachine.getRoles().size() == 1);

		// Go through all of the legal moves
		int score = 0;
		Move action = moves.get(0);
		for(Move move : moves) {

			List<Move> moveList = new ArrayList<Move>();

			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(getCurrentState(), moveList);

			int result = maxScoreDeliberation(getRole(), nextState, 0);

			if (result == 100) {
				action = move;
				return action;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				return action;
			}
		}

		return action;
	}

	// ----- Select Move ----- //
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		finishBy = timeout - buffer;

		did_timeout = false;

		// Get randomized list of moves
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		moves = new ArrayList<Move>(moves);
		Collections.shuffle(moves);	// Randomize move order

		// Select an Action
		Move action = moves.get(0);
		if(isSinglePlayer){
			action = compulsiveDeliberation(moves);
		} else {
			action = alphaBeta(moves);
		}

		// Iterative Deepening
		if(moves.size() != 1){	// Only increase depth if it was our turn
			if(!did_timeout){
				iterative_deepening_search_depth += 1;
				System.out.println("Set search depth to: " + iterative_deepening_search_depth);
			}
			else {	//There was a timeout
				iterative_deepening_search_depth = java.lang.Math.max(iterative_deepening_search_depth-1, 0);
				System.out.println("Out of time! Set search depth to: " + iterative_deepening_search_depth);
			}
		}


		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
