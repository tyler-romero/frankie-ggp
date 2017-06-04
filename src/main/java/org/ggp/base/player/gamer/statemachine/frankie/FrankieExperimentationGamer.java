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

public class FrankieExperimentationGamer extends FrankieGamer {

	// Settings and helpers
	private long buffer;

	// Agent components
	private Role agent;
	private List<Role> roles;
	private StateMachine stateMachine;
	private Timer timer;
	private GenericSearch searchFn;

	private int turn;
	boolean isSinglePlayer;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Configure Settings
		//buffer = 4500;
		buffer = 2000;
		long metagamebuffer = 6000;

		// Start timer
		long finishBy = timeout - metagamebuffer;
		timer = new Timer();
		timer.initTimer(timeout, finishBy);

		System.out.println("================= New Game =================");
		System.out.println(getName());	// Print the agent's name

		stateMachine = getStateMachine();
		agent = getRole();
		roles = stateMachine.getRoles();
		turn = 0;

		// Determine if game is single-player or multi-player and init MCTS
		if(roles.size() > 1){
			System.out.println("Multi-Player Game");
			isSinglePlayer = false;
		} else {
			System.out.println("Single-Player Game");
			isSinglePlayer = true;
		}

		if(smSpeed < 5.0){	// If there are tons of moves and we are super slow
			System.out.println("VERY SLOW GAME");
			List<FrankieHeuristic> heuristics = new ArrayList<FrankieHeuristic>();
			heuristics.add( new AgentMobilityHeuristic(0.0) );
			heuristics.add( new AgentFocusHeuristic(0.2) );
			if(!isSinglePlayer) {
				//heuristics.add( new OppMobilityHeuristic(0.0) );
				heuristics.add( new OppFocusHeuristic(0.8) );
			}
			FrankieEvaluationFunction evalFn = new FrankieEvaluationFunction(stateMachine, heuristics);

			if(roles.size() > 1){
				searchFn = new AlphaBeta(stateMachine, agent, timer, evalFn);
			} else {
				searchFn = new CompulsiveDeliberation(stateMachine, agent, timer, evalFn);
			}

		} else{
			searchFn = new RAVEMonteCarloTreeSearch(stateMachine, agent, timer);
		}

		// Start computing the game tree during meta game
		searchFn.metaGame(getCurrentState());

		if(timer.isExpired()){
			System.out.println("METAGAMING TIMER IS EXPIRED");
		}
	}

	// ----- Select Move ----- //
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		long finishBy = timeout - buffer;

		turn += 1;
		System.out.println(" ------------- Turn " + turn + " ------------- ");

		timer.initTimer(timeout, finishBy);

		// Get randomized list of moves
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), agent);
		moves = new ArrayList<Move>(moves);
		Collections.shuffle(moves);	// Randomize move order

		// Select an Action
		System.out.println("Number of Moves: " + moves.size());
		Move action = moves.get(0);

		action = searchFn.getAction(moves, getCurrentState());

		long stop = System.currentTimeMillis();		// Stop timer

		// Don't touch
		notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));

		if(timer.isExpired()){
			buffer += 500;	// Increase buffer if we timeout
			System.out.println("TIMER IS EXPIRED");
		}
		return action;
	}
}