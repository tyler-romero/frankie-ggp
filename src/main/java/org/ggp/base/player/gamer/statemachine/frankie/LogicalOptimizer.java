package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlVariable;
/*
import com.google.common.collect.ImmutableList;

public class LogicalOptimizer {

	public static Gdl reorder(Gdl gdl){
		if(!(gdl instanceof GdlRule)){
			return gdl;
		}
		GdlRule rule = (GdlRule) gdl;

		System.out.println(rule.toString());
		//System.out.println(rule.getBody().toString());
		//System.out.println(rule.getHead().toString());
		Class c = rule.getClass();
		c.cast(rule);

		// Init list
		List<GdlLiteral> sl = rule.getBody();	// Existing Literals
		List<GdlLiteral> vl = new ArrayList<GdlLiteral>();
		List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();	// New body of Literals

		// while there are existing literals
		while (sl.size() != 0){
			GdlLiteral ans = getbest(sl,vl);	// Get the most optimal GdlLiteral
			newBody.add(ans);					// Add that literal to the body
			vl = varsexp(ans,vl);				// Add answer to list of answers
		}
		GdlRule newrule = new GdlRule(rule.getHead(), ImmutableList.copyOf(newBody));
		System.out.println(newrule.toString());
		System.out.println(newrule.getBody().toString());
		System.out.println(newrule.getHead().toString());
		return newrule;
	}

	private static GdlLiteral getbest(List<GdlLiteral> sl, List<GdlLiteral> vl){
		int bestNumUnbound = 10000;
		int best = 0;

		// For each GdlLiteral
		for(int i = 0; i < sl.size(); i++){
			int numUnbound = unboundvarnum(sl.get(i), vl);	// Find number of unbound variables
			if(numUnbound < bestNumUnbound) {
				bestNumUnbound = numUnbound;
				best = i;
			}
		}

		GdlLiteral ans = sl.get(best);	// Return the best Literal
		sl.remove(best);	// And remove it from the list
		return ans;
	}

	private static int unboundvarnum(GdlLiteral x, List<GdlLiteral> vs){
		return unboundvars(x, new ArrayList<GdlLiteral>(), vs).size();
	}

	private static ArrayList<GdlLiteral> unboundvars(GdlLiteral x,
			ArrayList<GdlLiteral> us, List<GdlLiteral> vs){

		if(isVar(x)){
			if(isSymbol(x)){
				return us;
			} else {
				return adjoin(x, us);
			}
		}
		if(isConstant(x)){
			return us;
		}
		for(int i=0; i<x.size(); i++){
			us = unboundvars(x.get(i), us, vs);
		}
		return us;
	}

	private static List<GdlLiteral> varsexp(GdlLiteral ans, List<GdlLiteral> vl) {
		// TODO Auto-generated method stub
		return null;
	}

	private static boolean isVar(GdlLiteral expression){
		return expression instanceof GdlVariable;
	}

	private static boolean isSymbol(GdlLiteral expression){
		return expression instanceof symbol;
	}

	private static ArrayList<GdlLiteral> adjoin(GdlLiteral x, ArrayList<GdlLiteral> usitem){
		//adds an item to a sequence if it is not already a member of that sequence
		if(!usitem.contains(x)){
			usitem.add(x);
		}
		return usitem;
	}
}*/
