package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
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

public class FrankieAlphaBetaGamer extends FrankieGamer {

	private Map<Role, Integer> roleMap;
	private Role agent;
	private List<Role> roles;
	private int numRoles;

	private Role nextRole(Role currentRole){
		int curRoleIndex = roleMap.get(currentRole);
		int nextRoleIndex = (curRoleIndex + 1)%numRoles;
		return roles.get(nextRoleIndex);
	}

	private int minScore(Role role, List<Move> selected_moves, MachineState state, StateMachine stateMachine, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {
			selected_moves.set(roleMap.get(role), move);
			int result = 0;

			if(nextRole(role).equals(agent)){
				MachineState nextState = stateMachine.getNextState(state, selected_moves);
				result = maxScore(agent, nextState, stateMachine, alpha, beta);
			}
			else{
				result = minScore(nextRole(role), selected_moves, state, stateMachine, alpha, beta);
			}

			beta = java.lang.Math.min(beta, result);
			if (beta <= alpha){
				return alpha;
			}

		}
		return beta;
	}

	private int maxScore(Role role, MachineState state, StateMachine stateMachine, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		for(Move move : moves) {
			List<Move> selected_moves = new ArrayList<Move>(numRoles);
			for (int i =0; i<numRoles; i++){
				selected_moves.add(move);
			}
			selected_moves.set(roleMap.get(agent), move);
			int result = 0;
			if(nextRole(role).equals(agent)){
				MachineState nextState = stateMachine.getNextState(state, selected_moves);
				result = maxScore(agent, nextState, stateMachine, alpha, beta);
			}
			else
				result = minScore(nextRole(role), selected_moves, state, stateMachine, alpha, beta);
			alpha = java.lang.Math.max(alpha, result);
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub
		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		@SuppressWarnings("unused")
		long finishBy = timeout - 1000;

		roleMap = stateMachine.getRoleIndices();
		agent = getRole();
		roles = stateMachine.getRoles();
		numRoles = roles.size();
		System.out.println(""+numRoles);

		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);

		// Go through all of the legal moves

		List<Move> selected_moves = new ArrayList<Move>(numRoles);
		for (int i=0; i<numRoles; i++){
			selected_moves.add(action);
		}

		int score = 0;
		for(Move move : moves) {
			System.out.println(""+move);
			int alpha = 0;
			int beta = 100;
			selected_moves.set(roleMap.get(agent), move);
			int result = 0;
			if(nextRole(agent).equals(agent)){
				MachineState nextState = stateMachine.getNextState(getCurrentState(), selected_moves);
				result = maxScore(agent, nextState, stateMachine, alpha, beta);
			}
			else
				result = minScore(nextRole(agent), selected_moves, getCurrentState(), stateMachine, alpha, beta);
			System.out.println("Maxscore: " + result);
			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}
		}

		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
