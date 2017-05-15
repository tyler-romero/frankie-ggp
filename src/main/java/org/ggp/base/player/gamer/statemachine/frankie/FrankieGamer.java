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
		StateMachine propnet = new CachedStateMachine(new PropNetStateMachine());
		propnet.initialize(getMatch().getGame().getRules());

		boolean isConsistant = StateMachineVerifier.checkMachineConsistency(prover, propnet, 1000);

		if(isConsistant) {
			System.out.println("Using PropNetStateMachine");
			return propnet;
		} else {
			System.out.println("Using ProverStateMachine");
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
		// Frankie does no special cleanup when the match ends normally.
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
