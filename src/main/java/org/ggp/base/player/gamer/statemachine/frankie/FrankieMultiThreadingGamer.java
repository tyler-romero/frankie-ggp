package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieMultiThreadingGamer extends FrankieGamer {

	// Settings and helpers
	private long buffer;
	private int nThreads;

	// Agent components
	private Role agent;
	private List<Role> roles;
	private StateMachine stateMachine;
	private List<StateMachine> machines;
	private Timer timer;
	private MonteCarloTreeSearch searchFn;

	private int turn;

	// ----- Initialization and Pre-Computation ----- //
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("================= New Game =================");
		System.out.println(getName());	// Print the agent's name

		// Configure Settings
		buffer = 4500;
		long metagamebuffer = 6000;
		nThreads = 2;

		// Start Timer
		long finishBy = timeout - metagamebuffer;
		timer = new Timer();
		timer.initTimer(timeout, finishBy);

		// Additional Setup
		stateMachine = getStateMachine();
		agent = getRole();
		roles = stateMachine.getRoles();
		turn = 0;

		// Initialize statemachines for other threads
		ExecutorService executor = Executors.newFixedThreadPool(nThreads/2);
		CompletionService<StateMachine> completionService = new ExecutorCompletionService<StateMachine>(executor);

		machines = new ArrayList<StateMachine>();
		for(int i = 0; i<nThreads; i++){
			PropNetInitThread propThread = new PropNetInitThread(statemachinetype, getMatch().getGame().getRules());
			completionService.submit(propThread);
		}

		try {
            for (int i = 0;  i < nThreads; i++) {
                Future<StateMachine> fRave = completionService.take();	//take is a blocking method
                machines.add(fRave.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

		nThreads = machines.size();
		System.out.println("nThreads: " + nThreads);

		// Determine if game is single-player or multi-player and init MCTS
		if(roles.size() > 1) System.out.println("Multi-Player Game");
		else System.out.println("Single-Player Game");

		searchFn = new RootMultiThreadedMonteCarloTreeSearch(stateMachine, agent, timer, machines);

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
