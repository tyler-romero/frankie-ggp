package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class SimplePropNetStateMachine extends StateMachine {
	// This implementation uses backprop and is very very slow

	 /** The underlying proposition network  */
    private PropNet propNet;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description);
			//propNet.renderToFile("propnet.dot");
			roles = propNet.getRoles();
			initPropnetVars();
			for (Component c : propNet.getComponents()) {
				c.crystalize();
			}
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPropnetVars(){
		Collection<Proposition> bases = propNet.getBasePropositions().values();
		Collection<Proposition> inputs = propNet.getInputPropositions().values();
		Proposition init = propNet.getInitProposition();
		init.base = true;
		for (Proposition p : propNet.getPropositions()) {
			if (bases.contains(p) || inputs.contains(p)) {
				p.base = true;
			}
		}
    }


    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	clearpropnet();
    	markbases(state.getContents());
		return propNet.getTerminalProposition().propmark();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
    	clearpropnet();
    	markbases(state.getContents());
    	Set<Proposition> goals = propNet.getGoalPropositions().get(role);

    	for (Proposition p : goals) {
			if (p.propmark())
				return getGoalValue(p);
		}
		throw new GoalDefinitionException(state, role);
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	clearpropnet();
    	propNet.getInitProposition().setValue(true);
		MachineState initial = getStateFromBase();
		return initial;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
    	Map<Role, Set<Proposition>> legalPropositions = propNet.getLegalPropositions();
    	Set<Proposition> legals = legalPropositions.get(role);
    	List<Move> actions = new ArrayList<Move>();
    	for(Proposition p: legals){
   			actions.add(getMoveFromProposition(p));
    	}
		return actions;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
    	clearpropnet();
    	markbases(state.getContents());
    	Map<Role, Set<Proposition>> legalPropositions = propNet.getLegalPropositions();
    	Set<Proposition> legals = legalPropositions.get(role);
    	List<Move> actions = new ArrayList<Move>();
    	for(Proposition p: legals){
    		if(p.propmark()){
    			actions.add(getMoveFromProposition(p));
    		}
    	}
		return actions;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	clearpropnet();
    	markbases(state.getContents());
		markactions(toDoes(moves));
		Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();

		Set<GdlSentence> nexts = new HashSet<GdlSentence>();

		for (GdlSentence s : bases.keySet()) {
			if (bases.get(s).getSingleInputarr().propmark()) nexts.add(s);
		}
		return new MachineState(nexts);
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private Set<GdlSentence> toDoes(List<Move> moves){
    	Set<GdlSentence> doeses = new HashSet<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();
		for (int i = 0; i < roles.size(); i++) {
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p){
    	return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
	private int getGoalValue(Proposition goalProposition) {
    	GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return MachineState
     */
    public MachineState getStateFromBase() {
    	Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values()) {
            if (p.getSingleInputarr().propmark()){
                contents.add(p.getName());
            }
        }
        return new MachineState(contents);
    }

    // Helper Functions. Pseudo code from chapter 10
    private void markbases(Set<GdlSentence> contents){
		Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
		for (GdlSentence p : contents) {
			props.get(p).setValue(true);
		}
    }

    private void markactions(Set<GdlSentence> does){
		Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
		for (GdlSentence p : does) {
			props.get(p).setValue(true);
		}
    }

    private void clearpropnet(){
    	Set<Proposition> props = propNet.getPropositions();
    	for(Proposition prop: props){
    		prop.setValue(false);
    	}
    }
}
