package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSThread implements Callable<Integer> {
	Timer timer;
	Node root;
	StateMachine stateMachine;
	Role agent;
	Random randomizer = new Random();
	double C;

	MCTSThread(StateMachine sm, Role a, Timer t, Node r, double explorationConstant){
		stateMachine = sm;
		agent = a;
		timer = t;
		root = r;
		C = explorationConstant;
	}

	@Override
	public Integer call() {
		try{
			Integer numDepthCharges = 0;
			while(!timer.isOutOfTime()) {
				Node node_to_expand = select(root);
				if(timer.isOutOfTime()) break;
				Node node_to_evaluate = expand(node_to_expand);
				if(timer.isOutOfTime()) break;
				double score = simulate(node_to_evaluate);
				if(timer.isOutOfTime()) break;
				backprop(node_to_evaluate, score);
				numDepthCharges++;
			}

			return numDepthCharges;
		} catch(MoveDefinitionException | TransitionDefinitionException e) {
			System.out.println("MCTSThread encountered an error");
			e.printStackTrace();
			return 0;
		}
	}

	Node select(Node node) throws MoveDefinitionException{
		//System.out.println("select");
		if(node.children.size() == 0) return node;
		if(node.visits == 0) return node;

		// Evenly search each child to an arbitrary threshold
		for(Node child : node.children){
			if(child.visits==0)
				return child;
		}

		// Some epsilon greediness to help exploration
		Node result = null;
		if(randomizer.nextDouble() < 0.05){
			result = node.children.get(randomizer.nextInt(node.children.size()));
			select(result);
		}

		// If all children have been visited, select a child to recurse on
		double score = -Double.MAX_VALUE;
		for(Node child : node.children){
			double newscore = selectfn(child);
			if (newscore > score) {
				score = newscore;
				result = child;
			}
		}
		return select(result);
	}

	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Lower Confidence Bounds (How pessimistic we are when its our opponents turn)
		if(node.parent.isMin(stateMachine, agent)){
			return -1*(node.get_value() - C*Math.sqrt(Math.log(node.parent.visits)/node.visits));
		}
		// A formula based on Upper Confidence Bounds (How optimistic we are when its our turn)
		return node.get_value() + C*Math.sqrt(Math.log(node.parent.visits)/node.visits);
	}

	Node expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		// First check if this node is a terminal node. If it is, return it.
		if (stateMachine.isTerminal(node.state)) {
			return node;
		}

		// Create child nodes from all joint legal moves. NOTE: could be expensive
		List<Move> actions = stateMachine.findLegals(agent, node.state);

		synchronized(node.children) {
			for(Move action: actions) {
				List<List<Move>> joint_move_list = stateMachine.getLegalJointMoves(node.state, agent, action);
				for(List<Move> joint_move : joint_move_list) {
					MachineState newstate = stateMachine.getNextState(node.state, joint_move);
					Node newnode = new Node(newstate, node, action);
					node.children.add(newnode);
				}
			}
		}

		// Randomly return one of the child nodes
		return node.children.get(new Random().nextInt(node.children.size()));
	}

	double simulate(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		double reward = 0.0;
		MachineState mcFinalState = stateMachine.performDepthCharge(node.state, null);
		try{
			reward = stateMachine.findReward(agent, mcFinalState);
		}
		catch (GoalDefinitionException e){
			System.out.println("GoalDefinitionException in Monte Carlo");
		}
		return reward;
	}

	void backprop(Node node, double score) {
		synchronized(node.visits){
			node.visits = node.visits + 1;
		}
		synchronized(node.utility){
			node.utility = node.utility + score;
		}
		if(node.parent != null) {
			backprop(node.parent, score);
		}

	}
}


class AsyncMCTSThread implements Callable<Integer> {
	Timer timer;
	Node root;
	StateMachine stateMachine;
	Role agent;
	Random randomizer = new Random();
	double C;

	AsyncMCTSThread(StateMachine sm, Role a, Timer t, Node r, double explorationConstant){
		stateMachine = sm;
		agent = a;
		timer = t;
		root = r;
		C = explorationConstant;
	}

	@Override
	public Integer call() {
		try{
			Integer numDepthCharges = 0;
			while(!timer.isOutOfTime()) {
				Node node_to_expand = select(root);
				if(timer.isOutOfTime()) break;
				Node node_to_evaluate = expand(node_to_expand);
				if(timer.isOutOfTime()) break;
				double score = simulate(node_to_evaluate);
				if(timer.isOutOfTime()) break;
				backprop(node_to_evaluate, score);
				numDepthCharges++;
			}

			return numDepthCharges;
		} catch(MoveDefinitionException | TransitionDefinitionException e) {
			System.out.println("MCTSThread encountered an error");
			e.printStackTrace();
			return 0;
		}
	}

	Node select(Node node) throws MoveDefinitionException{
		//System.out.println("select");
		if(node.children.size() == 0) return node;
		if(node.visits == 0) return node;

		// Evenly search each child to an arbitrary threshold
		for(Node child : node.children){
			if(child.visits==0)
				return child;
		}

		// Some epsilon greediness to help exploration
		Node result = null;
		if(randomizer.nextDouble() < 0.05){
			result = node.children.get(randomizer.nextInt(node.children.size()));
			select(result);
		}

		// If all children have been visited, select a child to recurse on
		double score = -Double.MAX_VALUE;
		for(Node child : node.children){
			double newscore = selectfn(child);
			if (newscore > score) {
				score = newscore;
				result = child;
			}
		}
		return select(result);
	}

	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Lower Confidence Bounds (How pessimistic we are when its our opponents turn)
		if(node.parent.isMin(stateMachine, agent)){
			return -1*(node.get_value() - C*Math.sqrt(Math.log(node.parent.visits)/node.visits));
		}
		// A formula based on Upper Confidence Bounds (How optimistic we are when its our turn)
		return node.get_value() + C*Math.sqrt(Math.log(node.parent.visits)/node.visits);
	}

	Node expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		// First check if this node is a terminal node. If it is, return it.
		if (stateMachine.isTerminal(node.state)) {
			return node;
		}

		// Create child nodes from all joint legal moves. NOTE: could be expensive
		List<Move> actions = stateMachine.findLegals(agent, node.state);

		for(Move action: actions) {
			List<List<Move>> joint_move_list = stateMachine.getLegalJointMoves(node.state, agent, action);
			for(List<Move> joint_move : joint_move_list) {
				MachineState newstate = stateMachine.getNextState(node.state, joint_move);
				Node newnode = new Node(newstate, node, action);
				node.children.add(newnode);
			}
		}

		// Randomly return one of the child nodes
		return node.children.get(new Random().nextInt(node.children.size()));
	}

	double simulate(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		double reward = 0.0;
		MachineState mcFinalState = stateMachine.performDepthCharge(node.state, null);
		try{
			reward = stateMachine.findReward(agent, mcFinalState);
		}
		catch (GoalDefinitionException e){
			System.out.println("GoalDefinitionException in Monte Carlo");
		}
		return reward;
	}

	void backprop(Node node, double score) {
		node.visits = node.visits + 1;
		node.utility = node.utility + score;
		if(node.parent != null) {
			backprop(node.parent, score);
		}

	}
}
