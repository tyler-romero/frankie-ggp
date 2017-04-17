package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FrankieDeliberationPlayer extends FrankieGamer {
	private Random rand = new Random();

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub
		StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = (moves.get(rand.nextInt(moves.size())));

		// Shuffle the moves into a random order, so we dont end up chosing first legal move
		Collections.shuffle(moves);

		// Go through all of the legal moves
		boolean reasonableMoveFound = false;
		int maxGoal = 0;
		for(Move moveUnderConsideration : moves) {
		    // Check to see if there's time to continue.
		    if(System.currentTimeMillis() > finishBy) break;

		    // If we've found a reasonable move, only spend at most two seconds trying
		    // to find a winning move.
		    if(System.currentTimeMillis() > start + 2000 && reasonableMoveFound) break;

		    MachineState nextState = theMachine.getNextState(getCurrentState(), theMachine.getRandomJointMove(getCurrentState(), getRole(), moveUnderConsideration));

		    // Does the move under consideration end the game? If it does, do we win
		    // or lose? If we lose, don't bother considering it. If we win, then we
		    // definitely want to take this move. If its goal is better than our current
		    // best goal, go ahead and tentatively select it
		    if(theMachine.isTerminal(nextState)) {
		        if(theMachine.getGoal(nextState, getRole()) == 0) {
		            continue;
		        } else if(theMachine.getGoal(nextState, getRole()) == 100) {
	                selection = moveUnderConsideration;
	                break;
		        } else {
		        	if (theMachine.getGoal(nextState, getRole()) > maxGoal)
		        	{
		        		selection = moveUnderConsideration;
		        		maxGoal = theMachine.getGoal(nextState, getRole());
		        	}
		        	continue;
		        }
		    }

		    // Check whether any of the legal joint moves from this state lead to
		    // a loss for us.
		    boolean forcedLoss = false;
		    for(List<Move> jointMove : theMachine.getLegalJointMoves(nextState)) {
		        MachineState nextNextState = theMachine.getNextState(nextState, jointMove);
		        if(theMachine.isTerminal(nextNextState)) {
		            if(theMachine.getGoal(nextNextState, getRole()) == 0) {
		                forcedLoss = true;
		                break;
		            }
		        }

		        // Check to see if there's time to continue.
		        if(System.currentTimeMillis() > finishBy) {
		            forcedLoss = true;
		            break;
		        }
		    }

		    // If we've verified that this move isn't going to lead us to a state where
		    // our opponent can defeat us in one move, we should keep track of it.
		    if(!forcedLoss) {
		        selection = moveUnderConsideration;
		        reasonableMoveFound = true;
		    }
		}

		long stop = System.currentTimeMillis();

		// Dont touch
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

}
