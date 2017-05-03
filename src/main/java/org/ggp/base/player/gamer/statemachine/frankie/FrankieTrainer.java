package org.ggp.base.player.gamer.statemachine.frankie;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public final class FrankieTrainer implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static int n;
	private ArrayList<Integer> scores;
	private double high_score;

	public FrankieTrainer(){
		n = 10;	// Number of games to evaluate on
		high_score = 0.0;
		scores = new ArrayList<Integer>();
	}

	public void reset(){
		scores = new ArrayList<Integer>();
	}

	private double calculateAvg(){
		if (scores.size() == 0) return 0.0;
		Integer sum = 0;
	    for (Integer s : scores) {
	        sum += s;
	    }
	    return  sum.doubleValue() / scores.size();
	}

	public void updateEvaluateAndSave(FrankieEvaluationFunction evalFn, String testingSaveFile, String trainingSaveFile, int newScore){

		scores.add(newScore);

		if(n == scores.size()) {
			// Calculate average
			double avg_score = calculateAvg();

		    // If this is the new best evalFunction
		    if(avg_score > high_score) {
		    	high_score = avg_score;
		    	System.out.println("New Best Evaluation Function!");

		    	try {
					evalFn.save(testingSaveFile);
					System.out.println("Successfully saved best evaluation function");
				} catch (FileNotFoundException e) {
					System.out.println("Failed to save best evaluation function");
				}
		    }

		    // Try a new evalFunction
		    evalFn.randomizeWeights();
		    reset();
		    System.out.println("Randomly generated new training eval function");
		}

		try {
			evalFn.save(trainingSaveFile);
			System.out.println("Successfully saved new evaluation function");
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save new evaluation function");
		}
	}

	public void print() {
		double avg_score = calculateAvg();
		System.out.println("----- Frankie Trainer -----");
		System.out.println("Game number " + scores.size() + " out of 10");
		System.out.println("Average Score for this Eval Function: " + avg_score);
		System.out.println("Best Score for any Eval Function: " + high_score);
		System.out.println("---------------------------");
	}
}


