package org.ggp.base.player.gamer.statemachine.frankie;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.verifier.StateMachineVerifier;

public abstract class FrankieGamer extends StateMachineGamer {
	String statemachinetype = "";
	Double smSpeed = 0.0;


	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Currently, we do no MetaGaming
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	// This is the default State Machine
	@Override
	public StateMachine getInitialStateMachine() {
		StateMachine prover = new CachedStateMachine(new ProverStateMachine());
		prover.initialize(getMatch().getGame().getRules());
		StateMachine simplepropnet = new CachedStateMachine(new SimplePropNetStateMachine());
		simplepropnet.initialize(getMatch().getGame().getRules());
		StateMachine propnet = new CachedStateMachine(new PropNetStateMachine());
		propnet.initialize(getMatch().getGame().getRules());

		boolean isPropNetConsistant = StateMachineVerifier.checkMachineConsistency(prover, propnet, 100);
		boolean isSimplePropNetConsistant = StateMachineVerifier.checkMachineConsistency(prover, simplepropnet, 100);

		if(isPropNetConsistant && isSimplePropNetConsistant){
			System.out.println("PropNet Speed Test:");
			double propnetSpeed = propnet.performSpeedTest(2500);
			System.out.println("SimplePropNet Speed Test:");
			double simplePropnetSpeed = simplepropnet.performSpeedTest(2500);

			if(propnetSpeed > simplePropnetSpeed) {
				System.out.println("Using PropNetStateMachine");
				statemachinetype = "PropNetStateMachine";
				smSpeed = propnetSpeed;
				return propnet;
			} else{
				System.out.println("Using SimplePropNetStateMachine");
				statemachinetype = "SimplePropNetStateMachine";
				smSpeed = simplePropnetSpeed;
				return simplepropnet;
			}
		} else if (isPropNetConsistant) {
			System.out.println("SimplePropNetStateMachine is not consistant");
			System.out.println("Using PropNetStateMachine");
			System.out.println("PropNet Speed Test:");
			smSpeed = propnet.performSpeedTest(2500);
			statemachinetype = "PropNetStateMachine";
			return propnet;
		} else if (isSimplePropNetConsistant) {
			System.out.println("Using SimplePropNetStateMachine");
			System.out.println("SimplePropNet Speed Test:");
			smSpeed = simplepropnet.performSpeedTest(2500);
			statemachinetype = "SimplePropNetStateMachine";
			return simplepropnet;
		} else {
			System.out.println("Using ProverStateMachine");
			System.out.println("ProverStateMachine Speed Test:");
			smSpeed = prover.performSpeedTest(2500);
			statemachinetype = "ProverStateMachine";
			return prover;
		}
	}

	// This is the default Sample Panel
	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void stateMachineStop() {
		// Cleanup when the match ends normally
		try {
			int reward = getStateMachine().getGoal(getCurrentState(), getRole());
			System.out.println("Game over. Final Reward: " + reward);

		} catch (GoalDefinitionException e) {
			System.out.println("Goal Definition Exception: Failed to retrive final reward");
		}
	}

	@Override
	public void stateMachineAbort() {
		// Frankie does no special cleanup when the match ends abruptly.
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Frankie does no game previewing.
	}
}
