package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class Node {
	public int visits;
	public Node parent;
	public List<Node> children;
	public double utility;
	public MachineState state;
	public Move action;

	public Node(MachineState s, Node p, Move a){
		visits = 0;
		utility = 0.0;
		parent = p;	// The parent of the current node
		state = s;	// The state the current note represents
		children = new ArrayList<Node>();
		action = a;	// The action taken from the parent to reach this state
	}

	public double get_value() {
		if(visits != 0) return utility/visits;
		else return 0.0;
	}

	boolean isMin(StateMachine sm, Role agent) throws MoveDefinitionException {
		// We treat the node as a min if our agent only has one action.
			// This should work even if it is our agents turn and our agent only has one action
			// because there will only be one possible state to follow anyways.
		return (sm.findLegals(agent, state).size() == 1);
	}
}
