package org.ggp.base.player.gamer.statemachine.frankie;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieUniversalGamer extends FrankieGamer {

	private boolean isSinglePlayer;
	private boolean did_timeout;
	private boolean training;
	private boolean testing;
	private int iterative_deepening_search_depth;
	private int iterative_deepening_rate;
	private int iterative_deepening_counter;
	private long finishBy;
	private long buffer;
	private String saveFile;
	private String trainingSaveFile;
	private Role agent;
	private List<Role> roles;
	private StateMachine stateMachine;
	private FrankieEvaluationFunction evalFn;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("================= New Game =================");
		System.out.println(getName());	// Print the agent's name

		stateMachine = getStateMachine();
		agent = getRole();
		roles = stateMachine.getRoles();

		// Configure Settings
		training = true;	// Perform random search over heuristic weights
		testing = false;	// Use best set of weights found so far
		buffer = 3000;
		iterative_deepening_rate = 3;	// Number of turns in between updating depth
		iterative_deepening_counter = iterative_deepening_rate;

		// Determine if game is single-player or multi-player and init heuristics
		if(roles.size() > 1){
			System.out.println("Multi-Player Game");
			isSinglePlayer = false;
			saveFile = "frankie_weights_multi_player.txt";
			trainingSaveFile = "frankie_training_weights_multi_player.txt";
			iterative_deepening_search_depth = 4;
		}
		else {
			System.out.println("Single-Player Game");
			isSinglePlayer = true;
			saveFile = "frankie_weights_single_player.txt";
			trainingSaveFile = "frankie_training_weights_single_player.txt";
			iterative_deepening_search_depth = 6;
		}

		// Set up heuristics and evaluation function. Keep order the same.
		List<FrankieHeuristic> heuristics = new ArrayList<FrankieHeuristic>();
		heuristics.add( new MonteCarloHeuristic(0.33, 4) );
		heuristics.add( new AgentMobilityHeuristic(0.33) );
		heuristics.add( new AgentFocusHeuristic(0) );
		if(!isSinglePlayer) {
			heuristics.add( new OppMobilityHeuristic(0) );
			heuristics.add( new OppFocusHeuristic(0.33) );
		}
		evalFn = new FrankieEvaluationFunction(stateMachine, heuristics);

		// Load weights if specified
		if (training) {
			File f = new File(trainingSaveFile);
			if(f.exists() && !f.isDirectory()){
				try {
					evalFn.load(trainingSaveFile);
					System.out.println("Successfully loaded training evaluation function");
				} catch (IOException e) {
					System.out.println("Failed to load training evaluation function");
				}
			}
			else {
				System.out.println("No training evaluation function found, using default eval function");
			}
		}
		if (testing) {
			File f = new File(saveFile);
			if(f.exists() && !f.isDirectory()){
				try {
					evalFn.load(saveFile);
					System.out.println("Successfully loaded testing evaluation function");
				} catch (IOException e) {
					System.out.println("Failed to load testing evaluation function");
				}
			}
			else {
				System.out.println("No testing evaluation function found, using default eval function");
			}
		}
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

		System.out.println(" --- Turn --- ");

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
				if(iterative_deepening_rate == iterative_deepening_counter) {	// Only increase depth every so often
					iterative_deepening_search_depth += 1;
					System.out.println("Set search depth to: " + iterative_deepening_search_depth);
					iterative_deepening_counter = 0;
				}
				iterative_deepening_counter++;
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


	@Override
	public void stateMachineStop() {
		// Cleanup when the match ends normally
		try {
			int reward = stateMachine.getGoal(getCurrentState(), agent);
			System.out.println("Final Reward: " + reward);

			if(training){
				Serializer ser = new Serializer();
				FrankieTrainer trainer = ser.deserializeTrainier();
				trainer.updateEvaluateAndSave(evalFn, saveFile, trainingSaveFile, reward);
				trainer.print();
				ser.serializeTrainer(trainer);
			}
		} catch (GoalDefinitionException e) {
			System.out.println("Goal Definition Exception: Failed to retrive final reward");
		}


	}

}
