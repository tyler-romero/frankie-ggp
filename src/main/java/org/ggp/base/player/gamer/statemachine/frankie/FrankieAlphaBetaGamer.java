package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieAlphaBetaGamer extends FrankieGamer {

	private Role agent;
	private Role opp;

	private int minScore(Role role, Move agent_move, MachineState state, StateMachine stateMachine)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		int score = 100;

		for(Move opp_move : moves) {
			List<Move> moveList = new ArrayList<Move>();
			if(role.equals(stateMachine.getRoles().get(0))){
				moveList.add(opp_move);
				moveList.add(agent_move);
			}
			else{
				moveList.add(agent_move);
				moveList.add(opp_move);
			}

			MachineState nextState = stateMachine.getNextState(state, moveList);

			int result = maxScore(agent, nextState, stateMachine);
			if (result < score) {
				score = result;
			}
		}
		return score;
	}

	private int maxScore(Role role, MachineState state, StateMachine stateMachine)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);

		int score = 0;
		for(Move move : moves) {

			int result = minScore(opp, move, state, stateMachine);
			if (result > score) {
				score = result;
			}
		}
		return score;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub
		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		long finishBy = timeout - 1000;

		for(Role r : stateMachine.getRoles()){
			if (getRole().equals(r))
				agent = r;
			else
				opp = r;
		}

		assert(stateMachine.getRoles().size() == 1);

		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);

		// Go through all of the legal moves

		int score = 0;
		for(Move move : moves) {
			System.out.println(""+move);

			int result = minScore(opp, move, getCurrentState(), stateMachine);
			System.out.println("Maxscore: "+result);
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

		// Dont touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
