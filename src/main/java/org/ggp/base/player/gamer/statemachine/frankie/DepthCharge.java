package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.concurrent.Callable;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DepthCharge implements Callable<Integer> {
	private StateMachine sm;
	private MachineState state;
	private Role agent;

	DepthCharge(StateMachine stateMachine, MachineState mState, Role myAgent){
		sm = stateMachine;
		state = mState;
		agent = myAgent;
	}

	@Override
	public Integer call() {
		try {
			MachineState finalState = sm.performDepthChargeLite(state);
			Integer reward = sm.findReward(agent, finalState);
			return reward;
		}
		catch (TransitionDefinitionException | MoveDefinitionException | GoalDefinitionException e) {
			System.out.println("Exception thrown in DepthCharge");
			e.printStackTrace();
			return null;
		}
	}
}
