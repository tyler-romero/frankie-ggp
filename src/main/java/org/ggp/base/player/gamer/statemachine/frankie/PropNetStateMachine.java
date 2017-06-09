package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
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

public class PropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The player roles */
    private List<Role> roles;

    // Takes advantage of locality between states
    private Set<GdlSentence> prevBases;
	private Set<GdlSentence> prevInputs;

	private Set<Component> constants = new HashSet<Component>();
	public List<Gdl> description;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
        	description = sanitizeDistinct(description);
			propNet = OptimizingPropNetFactory.create(description);

			for (Component c : propNet.getComponents()) {
				c.crystalize();
				if(c instanceof Constant && c.getInputarr().length<1) constants.add(c);
			}
			roles = propNet.getRoles();

			initPropnetVars();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPropnetVars(){
    	prevBases = new HashSet<GdlSentence>();
		prevInputs = new HashSet<GdlSentence>();
		Collection<Proposition> bases = propNet.getBasePropositions().values();
		Collection<Proposition> inputs = propNet.getInputPropositions().values();
		for (Proposition p : propNet.getPropositions()) {
			if (bases.contains(p) || inputs.contains(p)) {
				p.base = true;
			}
		}
    }

    public int factor(){
    	int initial = propNet.getSize();
    	System.out.println("Initial Prop size: " + propNet.getSize());
    	clearpropnet();	// Resets flooding, in case factor is called multiple times
    	propNet.renderToFile("initialProp.dot");
    	Proposition term = propNet.getTerminalProposition();
    	Set<Proposition> legals = propNet.getLegalPropositions().get(roles.get(0));
    	Proposition[] legArr = legals.toArray(new Proposition[legals.size()]);
    	Map<Proposition,Proposition> legIn = propNet.getLegalInputMap();
    	term.flood();
    	for (Proposition l: legals){
    		l.flood();
    	}
    	Component[] comps = propNet.getComponents().toArray(new Component[propNet.getComponents().size()]);
    	for (Component c : comps) {
			if(!c.isValid)
				propNet.removeComponent(c);
		}
    	for (Proposition l : legArr){
    		if(!legIn.containsKey(l)) propNet.removeComponent(l);
    	}
    	for (Component c : propNet.getComponents()) {
			c.crystalize();
		}
    	propNet.renderToFile("factoredProp.dot");
    	System.out.println("Factored Size: " + propNet.getSize());
    	Set<Proposition> faclegals = propNet.getLegalPropositions().get(roles.get(0));
    	initPropnetVars();
    	try {
			System.out.println("Factored moves: " + findActions(roles.get(0)) );
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return initial - propNet.getSize();
    }


    public int getNumComponents(){
    	return propNet.getSize();
    }


    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	//System.out.println("isTerminal");
    	markbases(state.getContents());
		return propNet.getTerminalProposition().getValue();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	markbases(state.getContents());
    	Set<Proposition> goals = propNet.getGoalPropositions().get(role);
    	for (Proposition p : goals) {
			if (p.getValue()) return Integer.parseInt(p.getName().get(1).toString());
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
    	//System.out.println("GetInitialState");
    	clearpropnet();
    	propNet.getInitProposition().setValue(true);
    	propNet.getInitProposition().propogate(true);

		Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
		Set<GdlSentence> nexts = new HashSet<GdlSentence>();
		for (GdlSentence s : bases.keySet()) {
			if (bases.get(s).getSingleInputarr().getValue())
				nexts.add(s);
		}
		MachineState initial = new MachineState(nexts);

		propNet.getInitProposition().setValue(false);
		propNet.getInitProposition().propogate(false);
		return initial;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
    	//System.out.println("findActions");
    	Set<Proposition> legals = propNet.getLegalPropositions().get(role);
    	List<Move> moves = new ArrayList<Move>(legals.size());
		for (Proposition p : legals) {
			moves.add(getMoveFromProposition(p));
		}
		return moves;
    }

    private boolean markpropv(Proposition p){
    	Component input = p.getSingleInput();
    	if(constants.contains(input)) return input.getValue();
    	return p.getValue();
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
    	clearpropnet();	// For some stupid reason, necessary for multithreading
    	markbases(state.getContents());
    	Map<Role, Set<Proposition>> legalPropositions = propNet.getLegalPropositions();
    	Set<Proposition> legals = legalPropositions.get(role);
		List<Move> moves = new ArrayList<Move>(legals.size());
		for (Proposition p : legals) {
			if (markpropv(p)) {
				moves.add(getMoveFromProposition(p));
			}
		}
		return moves;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	markbases(state.getContents());
		markactions(toDoes(moves));
		Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();

		Set<GdlSentence> nexts = new HashSet<GdlSentence>();

		for (GdlSentence s : bases.keySet()) {
			if (bases.get(s).getSingleInputarr().getValue()) nexts.add(s);
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
    private Set<GdlSentence> toDoes(List<Move> moves)
    {
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
    public static Move getMoveFromProposition(Proposition p)
    {
    	return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    @SuppressWarnings("unused")
	private int getGoalValue(Proposition goalProposition)
    {
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
    public MachineState getStateFromBase()
    {
    	Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }
        }
        return new MachineState(contents);
    }

    // Helper Functions. Pseudo code from chapter 10
    private void markbases(Set<GdlSentence> contents){
    	Set<GdlSentence> nowFalse = new HashSet<GdlSentence>(prevBases);
		Set<GdlSentence> nowTrue = new HashSet<GdlSentence>(contents);
		nowFalse.removeAll(contents);
		nowTrue.removeAll(prevBases);

		Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();

		for (GdlSentence p : nowFalse) {
			bases.get(p).setValue(false);
			bases.get(p).start();
		}
		for (GdlSentence p : nowTrue) {
			bases.get(p).setValue(true);
			bases.get(p).start();
		}
		prevBases = contents;
    }

    private void markactions(Set<GdlSentence> does){
    	Set<GdlSentence> nowFalse = new HashSet<GdlSentence>(prevInputs);
		Set<GdlSentence> nowTrue = new HashSet<GdlSentence>(does);
		nowFalse.removeAll(does);
		nowTrue.removeAll(prevInputs);
		Map<GdlSentence, Proposition> bases = propNet.getInputPropositions();
		for (GdlSentence p : nowFalse) {
			bases.get(p).setValue(false);
			bases.get(p).start();
		}
		for (GdlSentence p : nowTrue) {
			bases.get(p).setValue(true);
			bases.get(p).start();
		}
		prevInputs = does;
    }

    private void clearpropnet(){
    	Set<Component> nots = new HashSet<Component>();
		for (Component s : propNet.getComponents()) {
			s.reset();
			if (s instanceof Not) nots.add(s);
		}
		for (Component s : nots) {
			s.propogate(true);
		}
		prevBases = new HashSet<GdlSentence>();
		prevInputs = new HashSet<GdlSentence>();
    }

    // Helpers from piazza
    private void sanitizeDistinctHelper(Gdl gdl, List<Gdl> in, List<Gdl> out) {
        if (!(gdl instanceof GdlRule)) {
            out.add(gdl);
            return;
        }
        GdlRule rule = (GdlRule) gdl;
        for (GdlLiteral lit : rule.getBody()) {
            if (lit instanceof GdlDistinct) {
                GdlDistinct d = (GdlDistinct) lit;
                GdlTerm a = d.getArg1();
                GdlTerm b = d.getArg2();
                if (!(a instanceof GdlFunction) && !(b instanceof GdlFunction)) continue;
                if (!(a instanceof GdlFunction && b instanceof GdlFunction)) return;
                GdlSentence af = ((GdlFunction) a).toSentence();
                GdlSentence bf = ((GdlFunction) b).toSentence();
                if (!af.getName().equals(bf.getName())) return;
                if (af.arity() != bf.arity()) return;
                for (int i = 0; i < af.arity(); i++) {
                    List<GdlLiteral> ruleBody = new ArrayList<>();
                    for (GdlLiteral newLit : rule.getBody()) {
                        if (newLit != lit) ruleBody.add(newLit);
                        else ruleBody.add(GdlPool.getDistinct(af.get(i), bf.get(i)));
                    }
                    GdlRule newRule = GdlPool.getRule(rule.getHead(), ruleBody);
                    //System.out.println("new rule: " + newRule);
                    in.add(newRule);
                }
                return;
            }
        }
        out.add(rule);
    }

    private List<Gdl> sanitizeDistinct(List<Gdl> description) {
        List<Gdl> out = new ArrayList<>();
        for (int i = 0; i < description.size(); i++) {
            sanitizeDistinctHelper(description.get(i), description, out);
        }
        return out;
    }


}
