package org.ggp.base.player.gamer.statemachine.frankie;

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

	// Settings and helpers
	private long buffer;

	// Agent components
	private Role agent;
	private List<Role> roles;
	private StateMachine stateMachine;
	private Timer timer;
	private AbstractMonteCarloTreeSearch searchFn;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Configure Settings
		buffer = 3000;
		long metagamebuffer = 5000;

		// Start timer
		long finishBy = timeout - metagamebuffer;
		timer = new Timer();
		timer.initTimer(finishBy);

		System.out.println("================= New Game =================");
		System.out.println(getName());	// Print the agent's name

		stateMachine = getStateMachine();
		agent = getRole();
		roles = stateMachine.getRoles();

		// Determine if game is single-player or multi-player and init MCTS
		if(roles.size() > 1){
			System.out.println("Multi-Player Game");
			searchFn = new MultiPlayerMonteCarloTreeSearch(stateMachine, agent, timer);
		}
		else {
			System.out.println("Single-Player Game");
			searchFn = new SinglePlayerMonteCarloTreeSearch(stateMachine, agent, timer);
		}

		// Start computing the game tree during meta game
		MachineState currentState = getCurrentState();
		assert(currentState != null);
		Node root = searchFn.getRoot(currentState);
		searchFn.MCTS(root);
		searchFn.printTree(root, 1, 0);
	}

	// ----- Select Move ----- //
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine stateMachine = getStateMachine();
		long start = System.currentTimeMillis();	// Start timer
		long finishBy = timeout - buffer;

		System.out.println(" ------------- Turn ------------- ");

		timer.initTimer(finishBy);

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
		return action;
	}


	@Override
	public void stateMachineStop() {
		// Cleanup when the match ends normally
		try {
			int reward = stateMachine.getGoal(getCurrentState(), agent);
			System.out.println("Game over. Final Reward: " + reward);

		} catch (GoalDefinitionException e) {
			System.out.println("Goal Definition Exception: Failed to retrive final reward");
		}
	}

}
