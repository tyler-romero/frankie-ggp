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
	private int single_player_max_search_depth;
	private int multi_player_max_search_depth;
	private boolean isSinglePlayer;
	private StateMachine stateMachine;
	private long finishBy;
	private long buffer;

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
		single_player_max_search_depth = 15;	// 0 indicates no maximum
		multi_player_max_search_depth = 10;
		buffer = 2000;

		// Determine if game is single-player or multi-player
		if(roles.size() > 1){
			isSinglePlayer = false;
			System.out.println("Multi-Player Game");
		}
		else if(roles.size() == 1){
			isSinglePlayer = true;
			System.out.println("Single-Player Game");
		}
		else assert(true);

		// TODO: Determine if game is zero-sum
	}

	// ----- Heuristic Functions ----- //
	private int evalFn(Role role, MachineState state) {

		return 0;
	}

	private int mobility(Role role, MachineState state) throws MoveDefinitionException {
		List<Move> actions = stateMachine.getLegalMoves(state, role);

	}



	// ----- Helper Functions ----- //
	private Role nextRole(Role currentRole) {
		int curRoleIndex = roleMap.get(currentRole);
		int nextRoleIndex = (curRoleIndex + 1)%numRoles;
		return roles.get(nextRoleIndex);
	}

	private List<Move> initListWithMove(Move action) {
		List<Move> selected_moves = new ArrayList<Move>(numRoles);
		for (int i=0; i<numRoles; i++){
			selected_moves.add(action);
		}
		return selected_moves;
	}

	// TODO: make sure this is correct. I could have the finishBy value interpreted incorrectly
	private boolean isOutOfTime(){
		long currentTime = System.currentTimeMillis();
		if(currentTime > finishBy) return true;
		else return false;
	}

	// ----- Alpha Beta Pruning ----- //
	private int minScoreAlphaBeta(Role role, List<Move> selected_moves, MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {
			selected_moves.set(roleMap.get(role), move);
			int result = 0;

			if(nextRole(role).equals(agent)){
				MachineState nextState = stateMachine.getNextState(state, selected_moves);
				result = maxScoreAlphaBeta(agent, nextState, alpha, beta, depth + 1);
			}
			else{
				result = minScoreAlphaBeta(nextRole(role), selected_moves, state, alpha, beta, depth);
			}

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
		if (depth >= multi_player_max_search_depth) {
			return evalFn(role, state);
		}
		if (isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {

			List<Move> selected_moves = initListWithMove(move);
			selected_moves.set(roleMap.get(agent), move);

			int result = minScoreAlphaBeta(nextRole(role), selected_moves, state, alpha, beta, depth);
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
		List<Move> selected_moves = initListWithMove(action);

		// Go through all of the legal moves
		int score = 0;
		for(Move move : moves) {

			int alpha = 0;
			int beta = 100;
			selected_moves.set(roleMap.get(agent), move);
			int result = minScoreAlphaBeta(nextRole(agent), selected_moves, getCurrentState(), alpha, beta, 0);

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
		if (depth >= single_player_max_search_depth) {
			return 0;
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


		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		moves = new ArrayList<Move>(moves);
		Collections.shuffle(moves);	// Randomize move order

		Move action = moves.get(0);
		if(isSinglePlayer){
			action = compulsiveDeliberation(moves);
		} else {
			action = alphaBeta(moves);
		}

		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
