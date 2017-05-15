package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class GameTreeSearch {
	StateMachine stateMachine;
	int search_depth;
	Timer timer;
	Role agent;

	GameTreeSearch(StateMachine sm, Role a, Timer t){
		stateMachine = sm;
		agent = a;
		timer = t;
	}

	public void setSearchDepth(int depth) {search_depth = depth;}

	public abstract Move getAction(List<Move> moves, MachineState currentState) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException;
}

//------------ AlphaBeta ---------------
// I dont think this is well defined for more than two players
class AlphaBeta extends GameTreeSearch{
	FrankieEvaluationFunction evalFn;

	AlphaBeta(StateMachine sm, Role agent, Timer t, FrankieEvaluationFunction eF) {
		super(sm, agent, t);
		evalFn = eF;
		System.out.println("AlphaBeta");
	}

	@Override
	public Move getAction(List<Move> moves, MachineState currentState) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		Move action = moves.get(0);

		// Go through all of the legal moves
		int score = 0;
		for(Move move : moves) {

			int alpha = 0;
			int beta = 100;

			int result = minScoreAlphaBeta(move, currentState, alpha, beta, 0);

			if (result == 100) {
				action = move;
				break;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(timer.isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				break;
			}
		}

		System.out.println("Action Score: " + score);
		return action;
	}

	private int maxScoreAlphaBeta(MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, agent);
		}
		if (depth >= search_depth) {
			return evalFn.value(agent, state);
		}
		if (timer.isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, agent);

		for(Move move : moves) {

			int result = minScoreAlphaBeta(move, state, alpha, beta, depth);
			alpha = java.lang.Math.max(alpha, result);

			if(alpha == 100){
				return alpha;
			}
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private int minScoreAlphaBeta(Move agentMove, MachineState state, int alpha, int beta, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		List<List<Move>> all_legal_moves = stateMachine.getLegalJointMoves(state, agent, agentMove);

		for(List<Move> moves : all_legal_moves) {
			int result = 0;

			MachineState nextState = stateMachine.getNextState(state, moves);
			result = maxScoreAlphaBeta(nextState, alpha, beta, depth + 1);

			beta = java.lang.Math.min(beta, result);
			if(beta == 0) {
				return beta;
			}
			if (beta <= alpha){
				return alpha;
			}
		}
		return beta;
	}
}

//------------ CompulsiveDeliberation ---------------

class CompulsiveDeliberation extends GameTreeSearch{
	FrankieEvaluationFunction evalFn;

	CompulsiveDeliberation(StateMachine sm, Role a, Timer t, FrankieEvaluationFunction eF) {
		super(sm, a, t);
		evalFn = eF;
		System.out.println("CompulsiveDeliberation");
	}

	@Override
	public Move getAction(List<Move> moves, MachineState currentState) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		assert(stateMachine.getRoles().size() == 1);

		// Go through all of the legal moves
		int score = 0;
		Move action = moves.get(0);
		for(Move move : moves) {

			List<Move> moveList = new ArrayList<Move>();

			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(currentState, moveList);

			int result = maxScoreDeliberation(agent, nextState, 0);

			if (result == 100) {
				action = move;
				return action;
			}
			if (result > score) {
				score = result;
				action = move;
			}

			// Return early if out of time
			if(timer.isOutOfTime()) {
				System.out.println("Out of time! Returning best action so far.");
				break;
			}
		}

		System.out.println("Action Score: " + score);
		return action;
	}

	private int maxScoreDeliberation(Role role, MachineState state, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{

		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		if (depth >= search_depth) {
			return evalFn.value(role, state);
		}
		if (timer.isOutOfTime()) {
			return 0;
		}

		List<Move> moves = stateMachine.getLegalMoves(state, role);

		int score = 0;
		for(Move move : moves) {
			List<Move> moveList = new ArrayList<Move>();
			moveList.add(move);
			MachineState nextState = stateMachine.getNextState(state, moveList);

			int result = maxScoreDeliberation(role, nextState, depth + 1);

			if (result == 100) {
				score = result;
				return score;
			}
			if (result > score) {
				score = result;
			}
		}

		return score;
	}

}

// ------------ MonteCarloTreeSearch ---------------
abstract class AbstractMonteCarloTreeSearch extends GameTreeSearch{
	Random randomizer;
	Node root;
	List<Role> roles;
	boolean metagaming;

	AbstractMonteCarloTreeSearch(StateMachine sm, Role a, Timer t) {
		super(sm, a, t);
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
				return root;
			}
		}
		System.out.println("New root not found in children. Creating new game tree root.");
		root = new Node(currentState, null, null);
		return root;
	}

	@Override
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

		printTree(root, 1, 0);
		System.out.println("Num depth charges: " + root.visits);
		System.out.println("Action Value: " + score);
		return bestAction;
	}

	public void MCTS(Node root) throws MoveDefinitionException, TransitionDefinitionException {
		while(!timer.isOutOfTime()) {
			Node node_to_expand = select(root);
			Node node_to_evaluate = expand(node_to_expand);
			double score = simulate(node_to_evaluate);
			backprop(node_to_evaluate, score);
		}
	}

	Node select(Node node) throws MoveDefinitionException {
		//System.out.println("select");
		if(node.children.size() == 0) return node;
		if(node.visits == 0) return node;

		// If a child has not been searched, search it
		for(Node child : node.children){
			if(child.visits==0)
				return child;
		}

		// If all children have been visited, select a child to recurse on
		double score = -Double.MAX_VALUE;
		Node result = null;
		for(Node child : node.children){
			double newscore = selectfn(child);
			if (newscore > score) {
				score = newscore;
				result = child;
			}
		}

		// Some epsilon greediness to encourage more exploration
		// If max, use epsilon greedy, if min, explore randomly
		if(randomizer.nextDouble() < 0.1){
			result = node.children.get(randomizer.nextInt(node.children.size()));
		}

		return select(result);
	}

	protected abstract double selectfn(Node node) throws MoveDefinitionException;

	double simulate(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		//System.out.println("simulate");
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


class MultiPlayerMonteCarloTreeSearch extends AbstractMonteCarloTreeSearch{
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
	void backprop(Node node, double score) throws MoveDefinitionException {
		//System.out.println("backprop");
		// Propagated value is the minimum of the values for all opponent actions
		node.visits = node.visits + 1;

		if(node.parent != null) {
			if(node.children.size() != 0 && node.isMin(stateMachine, agent)){
				score = 100.0;
				for(Node child: node.children){
					// There should always be one child with at least one visit
					if(child.visits > 0 && score > child.get_value()){
						score = child.get_value();
					}
				}
			}
			node.utility = node.utility + score;
			backprop(node.parent, score);
		}
		else {
			node.utility = node.utility + score;
		}
	}

	@Override
	protected
	double selectfn(Node node) throws MoveDefinitionException{
		// A formula based on Lower Confidence Bounds (How pessimistic we are when its our opponents turn)
		if(node.parent.isMin(stateMachine, agent)){
			return -1*(node.get_value() - 2*Math.sqrt(Math.log(node.parent.visits)/node.visits));
		}
		// A formula based on Upper Confidence Bounds (How optimistic we are when its our turn)
		return node.get_value() + 2*Math.sqrt(Math.log(node.parent.visits)/node.visits);
	}
}


class SinglePlayerMonteCarloTreeSearch extends AbstractMonteCarloTreeSearch{
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
		return node.get_value() + 2*Math.sqrt(Math.log(node.parent.visits)/node.visits);
	}
}