package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.LegacyPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;



public class DiffPropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;

    /** The player roles */
    private List<Role> roles;

    /** The game description for PropNet */
	public List<Gdl> description;

	/**Vars from propnet */
	private Map<GdlSentence, Proposition> basePropMap;
	private Map<GdlSentence, Proposition> inputPropMap;
	private Proposition initProp;
	private Proposition termProp;

	/**Ordering to update graph propositions */
	private List<Proposition> propOrder;

    /**
     * Initializes the PropNetStateMachine.
     * - generates PropNet
     * - sets all vars
     * - compute the topological ordering
     */
    @Override
    public void initialize(List<Gdl> description) {
        //get propnet
		this.description = description;
		//propNet = OptimizingPropNetFactory.create(description);
		propNet = LegacyPropNetFactory.create(description);
		//get vars
		roles = propNet.getRoles();
		basePropMap = propNet.getBasePropositions();
		inputPropMap = propNet.getInputPropositions();
		initProp = propNet.getInitProposition();
		termProp = propNet.getTerminalProposition();

		//set ordering
		System.out.println("Ordering Network");
		propOrder = getPropOrdering();
		System.out.println("PLZ");
    }

    /**Put all prop in order to update graph correctly and only once*/
//    private List<Proposition> getPropOrdering(){
//    	//all props we need to put in order
//    	List<Proposition> unordered = new LinkedList<Proposition>(propNet.getPropositions());
//
//    	//empty ordered list we will fill as we empty the unordered
//    	List<Proposition> ordered = new LinkedList<Proposition>();
//
//    	while(!unordered.isEmpty()){
//    		System.out.println(unordered.size());
//    		Proposition p = unordered.remove(0);
//    		if (isCore(p)) continue;
//    		//Set<Proposition> propDependencies = getPropDepends(p);
//    		Collection<Component> propDependencies = p.getSingleInput().getInputs();
//    		boolean dependencyFree = true;
//    		for(Component dependency : propDependencies){
//    			if (!ordered.contains(dependency) || isCore(p)){
//    				dependencyFree = false;
//    				break;
//    			}
//    		}
//    		if(dependencyFree) ordered.add(p);
//    		else unordered.add(p);
//    	}
//    	return ordered;
//    }
    private List<Proposition> getPropOrdering()
    {
      final List<Proposition> lOrdered = new LinkedList<Proposition>();

      //--------------------------------------------------------------------------
      // Take a copy of the list of all propositions.  We modify this list in
      // this function and don't want to destroy the underlying list.  A shallow
      // copy suffices.  (We don't change the underlying propositions.)
      //--------------------------------------------------------------------------
      final List<Proposition> lUnordered = new LinkedList<Proposition>(
                                                  propNet.getPropositions());

      //--------------------------------------------------------------------------
      // Compute the ordered list.
      //
      // To do this, take an item from the list of those that are not yet
      // ordered.  See if all its dependencies have been met (i.e. the inputs to
      // its input component are already all in the set of ordered
      // propositions).  If all the dependencies are met, move it to the tail of
      // the ordered list.  If not, just put it to the back of the unordered
      // list.  We'll consider it again later.
      //
      // Repeat until the unordered list is empty.
      //
      // This algorithm relies on the PropNet graph being bipartite with all
      // edges connecting a proposition to a non-proposition.
      //--------------------------------------------------------------------------
      while (!lUnordered.isEmpty())
      {
        //------------------------------------------------------------------------
        // Remove the item at the head of the unordered list.
        //------------------------------------------------------------------------
        final Proposition lProp = lUnordered.remove(0);

        //------------------------------------------------------------------------
        // We don't want fundamental propositions (base/input/init props) to
        // appear in the final output.
        //------------------------------------------------------------------------
        if (isCore(lProp))
        {
          continue;
        }

        //------------------------------------------------------------------------
        // Get the (immediate) input propositions that affect this one.
        //
        // For unoptimized graphs, all propositions have a single input - from a
        // component that isn't a proposition.  Get the inputs to that component.
        // (They're all propositions because this is a bipartite graph.)
        //------------------------------------------------------------------------
        final Collection<Component> lPropsToCheck = lProp.getSingleInput().getInputs();

        //------------------------------------------------------------------------
        // All propositions have a single input - from a component that isn't a
        // proposition.  Get the inputs to that component.  (They're all
        // propositions because this is a bipartite graph.)
        //
        // We hope to be able to add the proposition to the end of the ordered
        // list.  If we can't (because one of the inputs isn't yet on the ordered
        // list) we'll update this within the loop below.
        //------------------------------------------------------------------------
        List<Proposition> lListToAdd = lOrdered;
        for (final Component lComponent : lPropsToCheck)
        {
          if (!((lOrdered.contains(lComponent)) ||
                (isCore((Proposition)lComponent))))
          {
            lListToAdd = lUnordered;
            break;
          }
        }
        lListToAdd.add(lProp);
      }

      return lOrdered;
    }
    /**Helper fnc for getting only the prop dependencies*/
    private Set<Proposition> getPropDepends(Proposition p){
    	Set<Proposition> depends = new HashSet<Proposition>();
    	List<Component> possible_depends = new LinkedList<Component>();
    	possible_depends.addAll(p.getInputs());
    	while(!possible_depends.isEmpty()){
    		Component c = possible_depends.remove(0);
    		if (c instanceof Proposition) depends.add((Proposition)c);
    		else possible_depends.addAll(c.getInputs());
    	}
    	return depends;
    }

    /**Helper fnc for checking for core components*/
    private boolean isCore(Proposition p){
    	return p.equals(initProp) || inputPropMap.containsValue(p) || basePropMap.containsKey(p);
    }

    /**TODO put in other class*/
    public int factor(){
    	int initial = propNet.getSize();
    	System.out.println("Initial Prop size: " + propNet.getSize());
    	propNet.renderToFile("initialProp.dot");
    	Proposition term = propNet.getTerminalProposition();
    	Proposition init = propNet.getInitProposition();
    	Set<Proposition> legals = propNet.getLegalPropositions().get(roles.get(0));
    	Proposition[] legArr = legals.toArray(new Proposition[legals.size()]);
    	Map<Proposition,Proposition> legIn = propNet.getLegalInputMap();
    	term.isValid = true;
    	Component curComp = term.getSingleInput();
    	if(curComp instanceof Or){
    		System.out.println("PRUNE OR");
    		curComp.isValid = true;
    		init.flood();
    		Component[] curComparr = curComp.getInputarr();
    		Component keeper = curComparr[1];
    		for (Component c : curComparr){
    			if (c.isValid) keeper = c;
    			c.isValid=true;
    		}
    		curComparr[1].isValid = false;
    		curComparr[1].flood();
    		for (Component c : curComparr){
    			c.isValid=false;
    		}
    		curComparr[1].isValid= true;
    		keeper.isValid = true;
    		System.out.println(curComp);
    	}
    	else{
    		curComp.flood();
    	}

    	for (Proposition l: legals){
    		l.flood();
    	}

    	Component[] comps = propNet.getComponents().toArray(new Component[propNet.getComponents().size()]);
    	for (Component c : comps) {
			if(!c.isValid) propNet.removeComponent(c);
		}
    	//i hate my life
    	for (Proposition l : legArr){
    		if(!legIn.containsKey(l)) propNet.removeComponent(l);
    	}
    	for (Component c : propNet.getComponents()) {
			c.crystalize();
		}
    	Component[] comps2 = propNet.getComponents().toArray(new Component[propNet.getComponents().size()]);
    	for (Component c :comps2){
    		if(c.getInputarr().length<1 && c instanceof Not) {
    			propNet.removeComponent(c);
    		}
    	}
    	for (Component c : propNet.getComponents()) {
			c.crystalize();
		}
    	propNet.renderToFile("factoredProp.dot");
    	System.out.println("Factored Size: " + propNet.getSize());
    	Set<Proposition> faclegals = propNet.getLegalPropositions().get(roles.get(0));
    	try {
			System.out.println("Factored moves: " + findActions(roles.get(0)) );
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return initial - propNet.getSize();
    }


    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	//System.out.println("isTerminal");
    	markBases(state.getContents());
    	markPropogate();
		return termProp.getValue();
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

    	markBases(state.getContents());
    	markPropogate();

    	Set<Proposition> goals = propNet.getGoalPropositions().get(role);
    	for (Proposition p : goals) {
			if (p.getValue())
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
    	markInitial();
    	markPropogate();
		Set<GdlSentence> state = new HashSet<GdlSentence>();
		for (GdlSentence s : basePropMap.keySet()) {
			if (basePropMap.get(s).getSingleInputarr().getValue()) state.add(s);
		}
		return  new MachineState(state);
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException {
    	List<Move> legals = new ArrayList<Move>();
    	Set<Proposition> poss_legals = propNet.getLegalPropositions().get(role);
    	for (Proposition l : poss_legals){
    		legals.add(getMoveFromProposition(l));
    	}
		return legals;
    }


    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
    	markBases(state.getContents());
    	markPropogate();

    	List<Move> legals = new ArrayList<Move>();
    	Set<Proposition> poss_legals = propNet.getLegalPropositions().get(role);
    	for (Proposition l : poss_legals){
    		if (l.getValue()) legals.add(getMoveFromProposition(l));
    	}
		return legals;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {

    	markBases(state.getContents());
		markActions(moves);
		markPropogate();

		for(Proposition base : basePropMap.values()){
			boolean nextBaseValue = base.getSingleInput().getValue();
			base.setValue(nextBaseValue);
		}
		markPropogate();

		Set<GdlSentence> newState = new HashSet<GdlSentence>();
		for(GdlSentence base : basePropMap.keySet()){
			if(basePropMap.get(base).getValue()) newState.add(base);
		}

		return new MachineState(newState);
    }


    //MarkBases with state
    private void markBases(Set<GdlSentence> contents){
		for (GdlSentence s : contents) {
			basePropMap.get(s).setValue(true);
		}
    }

    private void markActions(List<Move> actions){
    	for(GdlSentence t : toDoes(actions)){
    		inputPropMap.get(t).setValue(true);
    	}
    }
    private void markInitial(){
    	initProp.setValue(true);
    }

    private void markPropogate(){
    	for(Proposition p : propOrder){
    		p.setValue(p.getSingleInput().getValue());
    	}
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
     * @return PropNetMachineState
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

}
