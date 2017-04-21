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
	private boolean isSinglePlayer;
	private StateMachine stateMachine;

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

	// ----- Helper Functions ----- //
	private Role nextRole(Role currentRole){
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

	private boolean isOutOfTime(long start, long finishBy){
		long currentTime = System.currentTimeMillis();
		if(currentTime > start + finishBy) return true;
		else return false;
	}

	// ----- Alpha Beta Pruning ----- //
	private int minScoreAlphaBeta(Role role, List<Move> selected_moves, MachineState state, StateMachine stateMachine, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {
			selected_moves.set(roleMap.get(role), move);
			int result = 0;

			if(nextRole(role).equals(agent)){
				MachineState nextState = stateMachine.getNextState(state, selected_moves);
				result = maxScoreAlphaBeta(agent, nextState, stateMachine, alpha, beta);
			}
			else{
				result = minScoreAlphaBeta(nextRole(role), selected_moves, state, stateMachine, alpha, beta);
			}

			beta = java.lang.Math.min(beta, result);
			if (beta <= alpha){
				return alpha;
			}

		}
		return beta;
	}

	private int maxScoreAlphaBeta(Role role, MachineState state, StateMachine stateMachine, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {

			List<Move> selected_moves = initListWithMove(move);
			selected_moves.set(roleMap.get(agent), move);

			int result = minScoreAlphaBeta(nextRole(role), selected_moves, state, stateMachine, alpha, beta);
			alpha = java.lang.Math.max(alpha, result);

			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private Move alphaBeta(long start, long finishBy, List<Move> moves)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		Move action = moves.get(0);
		List<Move> selected_moves = initListWithMove(action);

		// Go through all of the legal moves
		int score = 0;
		for(Move move : moves) {

			int alpha = 0;
			int beta = 100;
			selected_moves.set(roleMap.get(agent), move);
			int result = minScoreAlphaBeta(nextRole(agent), selected_moves, getCurrentState(), stateMachine, alpha, beta);

			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time (TODO: Improve this)
			if(isOutOfTime(start, finishBy)) {
				System.out.println("Out of time! Returning best action so far.");
				return action;
			}
		}

		return action;
	}

	// ----- Compulsive Deliberation ----- //
	private int maxScoreDeliberation(Role role, MachineState state, StateMachine stateMachine)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}

		List<Move> moves = stateMachine.getLegalMoves(state, role);

		int score = 0;
		for(Move move : moves) {
			List<Move> moveList = new ArrayList<Move>();
			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(state, moveList);

			int result = maxScoreDeliberation(role, nextState, stateMachine);

			if (result == 100) {
				score = result;
				break;
			}
			if (result > score) {
				score = result;
			}
		}

		return score;
	}

	private Move compulsiveDeliberation(long start, long finishBy, List<Move> moves)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException  {
		assert(stateMachine.getRoles().size() == 1);

		// Go through all of the legal moves
		int score = 0;
		Move action = moves.get(0);
		for(Move move : moves) {

			List<Move> moveList = new ArrayList<Move>();

			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(getCurrentState(), moveList);

			int result = maxScoreDeliberation(getRole(), nextState, stateMachine);

			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(isOutOfTime(start, finishBy)) {
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
		long finishBy = timeout - 1000;

		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		moves = new ArrayList<Move>(moves);
		Collections.shuffle(moves);	// Randomize move order

		Move action = moves.get(0);
		if(isSinglePlayer){
			action = compulsiveDeliberation(start, finishBy, moves);
		} else {
			action = alphaBeta(start, finishBy, moves);
		}

		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
