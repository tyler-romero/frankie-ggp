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
		iterative_deepening_search_depth = 8;	// 0 indicates no maximum


		buffer = 4000;

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
	private int evalFn(Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		//int focus_value = focus(role, state);
		int mobility_value = mobility(role,state);
		//int opp_mobility_value = mobility_value;
		//int opp_focus_value = focus_value;
		/*
		if (!isSinglePlayer) {
			//opp_mobility_value = oppMobility(role,state);
			opp_focus_value = oppFocus(role, state);
		}
		*/
		//int goal_prox_value = 0;	//goalProx(role,state);
		int heuristic_value = (int) (1*mobility_value);
		//System.out.println("EvalFn: " + heuristic_value);
		return heuristic_value;
	}

	// One step mobility function
	private int mobility(Role role, MachineState state) throws MoveDefinitionException {
		List<Move> legal_actions = stateMachine.getLegalMoves(state, role);
		List<Move> all_actions = stateMachine.findActions(role);
		return legal_actions.size()/all_actions.size() * 100;
	}

	private int focus(Role role, MachineState state) throws MoveDefinitionException {
		List<Move> legal_actions = stateMachine.getLegalMoves(state, role);
		List<Move> all_actions = stateMachine.findActions(role);
		return 100 - legal_actions.size()/all_actions.size() * 100;
	}

	private int oppMobility(Role role, MachineState state) throws MoveDefinitionException {
		int total_legal_actions = 0;
		int total_all_actions = 0;
		for (Role r : roles){
			if (r.equals(role))
				continue;
			total_legal_actions = total_legal_actions + stateMachine.getLegalMoves(state, r).size();
			total_all_actions = total_all_actions + stateMachine.findActions(r).size();
		}
		return total_legal_actions/total_all_actions * 100;
	}

	private int oppFocus(Role role, MachineState state) throws MoveDefinitionException {
		int total_legal_actions = 0;
		int total_all_actions = 0;
		for (Role r : roles){
			if (r.equals(role))
				continue;
			total_legal_actions = total_legal_actions + stateMachine.getLegalMoves(state, r).size();
			total_all_actions = total_all_actions + stateMachine.findActions(r).size();
		}
		return 100 - total_legal_actions/total_all_actions * 100;
	}

	private int goalProx(Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
//		MachineState terminalState = stateMachine.performDepthCharge(state, new int[1]);
//		int terminalReward = 0;
//		try{
//			terminalReward = stateMachine.findReward(role, terminalState);
//		}
//		catch (GoalDefinitionException e){
//			System.out.println("weird");
//		}
//		Set<GdlSentence> terminalGDL = terminalState.getContents();
//		Set<GdlSentence> currentGDL = state.getContents();
//		Set<GdlSentence> missingGDL = terminalGDL;
//		missingGDL.removeAll(currentGDL);
//		float fracTerm = (terminalGDL.size()-missingGDL.size())/terminalGDL.size();
//		return (int) (terminalReward * fracTerm);
		try{
			return stateMachine.findReward(role, state);
		}
		catch (GoalDefinitionException e){
			return 0;
		}
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
			return evalFn(role, state);
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
			return evalFn(role, state);
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
				iterative_deepening_search_depth -= 1;
				System.out.println("Out of time! Set search depth to: " + iterative_deepening_search_depth);
			}
		}


		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
	}

}
