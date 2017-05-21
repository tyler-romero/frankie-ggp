package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class MonteCarloTreeSearch {
	StateMachine stateMachine;
	Timer timer;
	Role agent;
	Random randomizer;
	Node root;
	List<Role> roles;
	boolean metagaming;


	MonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		stateMachine = sm;
		agent = a;
		timer = t;
		randomizer = new Random();
		root = null;	// Think about how to determine if its a min or max term
		roles = stateMachine.getRoles();
		metagaming = true;
	}

	Node getRoot(MachineState currentState) throws MoveDefinitionException {
		if(root == null){
			System.out.println("Creating new game tree root");
			root = new Node(currentState, null, null);
			return root;
		}

		if(metagaming){
			return root;	// Return the root that was expanded during metagaming
		}

		for(Node child: root.children) {
			if(child.state.equals(currentState)) {
				root = child;
				root.parent = null;
				return root;
			}
		}
		System.out.println("New root not found in children. Creating new game tree root.");
		root = new Node(currentState, null, null);
		return root;
	}

	public Move getAction(List<Move> moves, MachineState currentState) throws MoveDefinitionException, TransitionDefinitionException {
		root = getRoot(currentState);
		if(metagaming) metagaming = false;

		System.out.println("Num recycled depth charges: " + root.visits);

		// Search the tree
		MCTS(root);	// Uses timer to terminate

		// Select the best action from the children.
		Move bestAction = moves.get(0);
		double score;
		if(root.isMin(stateMachine, agent)){	// Min case: Only one possible action. Find min action value.
			score = 100.0;
			for(Node child : root.children) {
				double result = child.get_value();
				if (result < score)
					score = result;
			}
		} else {								// Max case: Find best action. Find max action value.
			score = 0;
			for(Node child : root.children) {
				double result = child.get_value();
				if (result > score) {
					score = result;
					bestAction = child.action;
				}
			}
		}

		//printTree(root, 1, 0);
		System.out.println("Num depth charges: " + root.visits);
		System.out.println("State Value: " + root.get_value());
		System.out.println("Action Value: " + score);
		return bestAction;
	}

	public void MCTS(Node root) throws MoveDefinitionException, TransitionDefinitionException {
		while(!timer.isOutOfTime()) {
			Node node_to_expand = select(root);
			if(timer.isOutOfTime()) break;
			Node node_to_evaluate = expand(node_to_expand);
			if(timer.isOutOfTime()) break;
			double score = simulate(node_to_evaluate);
			if(timer.isOutOfTime()) break;
			backprop(node_to_evaluate, score);
		}
	}

	Node select(Node node) throws MoveDefinitionException {
		//System.out.println("select");
		if(node.children.size() == 0) return node;
		if(node.visits == 0) return node;

		// Evenly search each child to an arbitrary threshold
		int threshold = 1;
		for(int i=0; i<threshold; i++) {
			for(Node child : node.children){
				if(child.visits<=i)
					return child;
			}
		}

		// If all children have been visited, select a child to recurse on
		Node result = null;
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

	protected abstract double selectfn(Node node) throws MoveDefinitionException;

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
		return node.children.get(randomizer.nextInt(node.children.size()));
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

	protected abstract void backprop(Node node, double score) throws MoveDefinitionException;

	void printTree(Node node, int max_depth, int depth) throws MoveDefinitionException {
		if(depth > max_depth) return;
		if(depth == 0 && max_depth != 0) System.out.println("-----");
		StringBuffer outputBuffer = new StringBuffer(depth);
		for (int i = 0; i < depth; i++){
		   outputBuffer.append("\t");
		}
		String tabs = outputBuffer.toString();

		String bool;
		if(node.isMin(stateMachine, agent)) bool = "min";
		else bool = "max";

		// pre-order
		System.out.println(tabs + "(" + node.get_value() + "/" + node.visits + ") " + bool);
		for(Node child : node.children){
			printTree(child, max_depth, depth+1);
		}

		if(depth == 0 && max_depth != 0) System.out.println("-----");
	}
}


// --------------- Implementations --------------------

class MultiPlayerMonteCarloTreeSearch extends MonteCarloTreeSearch{
	// The general idea:
		// On explore based on confidence bounds. Aka explore moves that are more likely to be taken more often.
		// Act pessimistic for our opponents moves, and optimisitic for our own.
		// During backprop, on min turns, return the min value of the nodes children on min states.

	MultiPlayerMonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		super(sm, a, t);
		System.out.println("MultiPlayerMonteCarloTreeSearch");
	}

	@Override
	protected
	void backprop(Node node, double score) {
		node.visits = node.visits + 1;
		node.utility = node.utility + score;
		if(node.parent != null) {
			backprop(node.parent, score);
		}
	}

	@Override
	protected
	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Lower Confidence Bounds (How pessimistic we are when its our opponents turn)
		if(node.parent.isMin(stateMachine, agent)){
			return -1*(node.get_value() - Math.sqrt(Math.log(2*node.parent.visits)/node.visits));
		}
		// A formula based on Upper Confidence Bounds (How optimistic we are when its our turn)
		return node.get_value() + Math.sqrt(Math.log(2*node.parent.visits)/node.visits);
	}
}


class MultiThreadedMultiPlayerMonteCarloTreeSearch extends MonteCarloTreeSearch{
	// Depending on the game could be faster or slower than single threaded version.
	DepthChargeManager dmManager;

	MultiThreadedMultiPlayerMonteCarloTreeSearch(StateMachine sm, Role a, Timer t, List<StateMachine> machines) {
		super(sm, a, t);
		System.out.println("ExperimentalMultiPlayerMonteCarloTreeSearch");
		dmManager = new DepthChargeManager(machines, agent);
	}

	@Override
	public void MCTS(Node root) throws MoveDefinitionException, TransitionDefinitionException {
		while(!timer.isOutOfTime()) {
			Node node_to_expand = select(root);
			Node node_to_evaluate = expand(node_to_expand);
			List<Integer> scores = simulateParallel(node_to_evaluate);

			int scoreSum = 0;
			for(int score: scores){
				scoreSum += score;
			}

			backpropParallel(node_to_evaluate, scoreSum, scores.size());
		}
	}

	List<Integer> simulateParallel(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		return dmManager.performDepthCharges(node.state);
	}

	void backpropParallel(Node node, int scoreSum, int newVisits) {
		node.visits = node.visits + newVisits;
		node.utility = node.utility + scoreSum;
		if(node.parent != null)
			backpropParallel(node.parent, scoreSum, newVisits);
	}

	@Override
	protected
	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Lower Confidence Bounds (How pessimistic we are when its our opponents turn)
		if(node.parent.isMin(stateMachine, agent)){
			return -1*(node.get_value() - Math.sqrt(Math.log(2*node.parent.visits)/node.visits));
		}
		// A formula based on Upper Confidence Bounds (How optimistic we are when its our turn)
		return node.get_value() + Math.sqrt(Math.log(2*node.parent.visits)/node.visits);
	}

	@Override
	protected void backprop(Node node, double score) throws MoveDefinitionException {
		assert(false);
	}

}


class SinglePlayerMonteCarloTreeSearch extends MonteCarloTreeSearch{
	// Straightforward implementation of MCTS

	SinglePlayerMonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		super(sm, a, t);
		System.out.println("SinglePlayerMonteCarloTreeSearch");
	}

	@Override
	protected
	void backprop(Node node, double score) {
		node.visits = node.visits + 1;
		node.utility = node.utility + score;
		if(node.parent != null) {
			backprop(node.parent, score);
		}
	}

	@Override
	protected
	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Upper Confidence Bounds (How optimistic we are)
		return node.get_value() + Math.sqrt(Math.log(2*node.parent.visits)/node.visits);
	}
}