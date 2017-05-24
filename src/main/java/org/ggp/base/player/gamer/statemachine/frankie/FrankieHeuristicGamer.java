package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieHeuristicGamer extends FrankieGamer {

	// Settings
	private boolean isSinglePlayer;
	private int search_depth;
	private int deepening_rate;
	private int deepening_counter;
	private long buffer;

	// Agent components
	private Role agent;
	private List<Role> roles;
	private StateMachine stateMachine;
	private Timer timer;
	private FrankieEvaluationFunction evalFn;
	private GameTreeSearch searchFn;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("================= New Game =================");
		System.out.println(getName());	// Print the agent's name

		stateMachine = getStateMachine();
		agent = getRole();
		roles = stateMachine.getRoles();
		timer = new Timer();

		// Configure Settings
		buffer = 3000;
		deepening_rate = 2;	// Number of turns in between updating depth
		deepening_counter = 0;

		// Determine if game is single-player or multi-player and init heuristics
		if(roles.size() > 1){
			System.out.println("Multi-Player Game");
			isSinglePlayer = false;
			search_depth = 4;
		}
		else {
			System.out.println("Single-Player Game");
			isSinglePlayer = true;
			search_depth = 6;
		}

		// Set up heuristics and evaluation function. Keep order the same.
		List<FrankieHeuristic> heuristics = new ArrayList<FrankieHeuristic>();
		heuristics.add( new MonteCarloHeuristic(0.6, 4) );
		heuristics.add( new AgentMobilityHeuristic(0.2) );
		heuristics.add( new AgentFocusHeuristic(0) );
		if(!isSinglePlayer) {
			heuristics.add( new OppMobilityHeuristic(0.2) );
			heuristics.add( new OppFocusHeuristic(0) );
		}
		evalFn = new FrankieEvaluationFunction(stateMachine, heuristics);


		// Set up game tree search
		if(isSinglePlayer){
			searchFn = new CompulsiveDeliberation(stateMachine, agent, timer, evalFn);
		} else {
			searchFn = new AlphaBeta(stateMachine, agent, timer, evalFn);
		}

	}

	// ----- Select Move ----- //
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		long finishBy = timeout - buffer;

		System.out.println(" --- Turn --- ");

		timer.initTimer(timeout, finishBy);


		// Get randomized list of moves
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), getRole());
		moves = new ArrayList<Move>(moves);
		Collections.shuffle(moves);	// Randomize move order

		// Select an Action
		System.out.println("Number of Moves: " + moves.size());
		Move action = moves.get(0);

		action = searchFn.getAction(moves, getCurrentState());

		// Iterative Deepening
		if(moves.size() != 1){	// Only increase depth if it was our turn
			if(!timer.did_timeout){
				if(deepening_rate == deepening_counter) {	// Only increase depth every so often
					search_depth += 1;
					searchFn.setSearchDepth(search_depth);
					System.out.println("Set search depth to: " + search_depth);
					deepening_counter = 0;
				}
				deepening_counter++;
			}
			else {	//There was a timeout
				search_depth = java.lang.Math.max(search_depth-1, 0);
				searchFn.setSearchDepth(search_depth);
				System.out.println("Out of time! Set search depth to: " + search_depth);
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

		} catch (GoalDefinitionException e) {
			System.out.println("Goal Definition Exception: Failed to retrive final reward");
		}
	}
}
