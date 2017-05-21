package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class GameTreeSearch {
	StateMachine stateMachine;
	int search_depth;
	Timer timer;
	Role agent;

	GameTreeSearch(StateMachine sm, Role a, Timer t){
		stateMachine = sm;
		agent = a;
		timer = t;
	}

	public void setSearchDepth(int depth) {search_depth = depth;}

	public abstract Move getAction(List<Move> moves, MachineState currentState) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException;
}

//------------ AlphaBeta ---------------
// I dont think this is well defined for more than two players
class AlphaBeta extends GameTreeSearch{
	FrankieEvaluationFunction evalFn;

	AlphaBeta(StateMachine sm, Role agent, Timer t, FrankieEvaluationFunction eF) {
		super(sm, agent, t);
		evalFn = eF;
		System.out.println("AlphaBeta");
	}

	@Override
	public Move getAction(List<Move> moves, MachineState currentState) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		Move action = moves.get(0);

		// Go through all of the legal moves
		int score = 0;
		for(Move move : moves) {

			int alpha = 0;
			int beta = 100;

			int result = minScoreAlphaBeta(move, currentState, alpha, beta, 0);

			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(timer.isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				break;
			}
		}

		System.out.println("Action Score: " + score);
		return action;
	}

	private int maxScoreAlphaBeta(MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, agent);
		}
		if (depth >= search_depth) {
			return evalFn.value(agent, state);
		}
		if (timer.isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, agent);

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

	private int minScoreAlphaBeta(Move agentMove, MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		List<List<Move>> all_legal_moves = stateMachine.getLegalJointMoves(state, agent, agentMove);

		for(List<Move> moves : all_legal_moves) {
			int result = 0;

			MachineState nextState = stateMachine.getNextState(state, moves);
			result = maxScoreAlphaBeta(nextState, alpha, beta, depth + 1);

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
}

//------------ CompulsiveDeliberation ---------------

class CompulsiveDeliberation extends GameTreeSearch{
	FrankieEvaluationFunction evalFn;

	CompulsiveDeliberation(StateMachine sm, Role a, Timer t, FrankieEvaluationFunction eF) {
		super(sm, a, t);
		evalFn = eF;
		System.out.println("CompulsiveDeliberation");
	}

	@Override
	public Move getAction(List<Move> moves, MachineState currentState) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		assert(stateMachine.getRoles().size() == 1);

		// Go through all of the legal moves
		int score = 0;
		Move action = moves.get(0);
		for(Move move : moves) {

			List<Move> moveList = new ArrayList<Move>();

			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(currentState, moveList);

			int result = maxScoreDeliberation(agent, nextState, 0);

			if (result == 100) {
				action = move;
				return action;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(timer.isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				break;
			}
		}

		System.out.println("Action Score: " + score);
		return action;
	}

	private int maxScoreDeliberation(Role role, MachineState state, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		if (depth >= search_depth) {
			return evalFn.value(role, state);
		}
		if (timer.isOutOfTime()) {
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

}

