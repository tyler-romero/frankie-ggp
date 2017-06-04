package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class GenericSearch {
	StateMachine stateMachine;
	Timer timer;
	Role agent;

	GenericSearch(StateMachine sm, Role a, Timer t){
		stateMachine = sm;
		agent = a;
		timer = t;
	}

	public abstract Move getAction(List<Move> moves, MachineState currentState) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException;

	public abstract void metaGame(MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException;
}
