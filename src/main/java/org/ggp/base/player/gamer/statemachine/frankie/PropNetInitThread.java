package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;
import java.util.concurrent.Callable;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class PropNetInitThread implements Callable<StateMachine>{

	String statemachinetype;
	List<Gdl> gameRules;

	PropNetInitThread(String smt, List<Gdl> rules){
		statemachinetype = smt;
		gameRules = rules;
	}

	@Override
	public StateMachine call() throws Exception {
		StateMachine sm = null;

		System.out.println("Creating instance of "+ statemachinetype);
		if(statemachinetype == "PropNetStateMachine"){
			sm = new PropNetStateMachine();
		}
		else if (statemachinetype == "ProverStateMachine"){
			sm = new ProverStateMachine();
		}
		else if (statemachinetype == "SimplePropNetStateMachine"){
			sm = new SimplePropNetStateMachine();
		}
		else{
			System.out.println("STATE MACHINE TYPE NOT RECOGNIZED");
			assert(false);
		}

		sm.initialize(gameRules);
		return sm;
	}

}
