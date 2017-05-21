package org.ggp.base.player.gamer.statemachine.frankie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class DepthChargeManager {
	List<StateMachine> stateMachines;
	List<DepthCharge> charges;
	static int nThreads;
	Role agent;
	ExecutorService executor;
	CompletionService<Integer> completionService;
	ArrayList<Integer> rewards;

	DepthChargeManager(List<StateMachine> sMs, Role a){
		stateMachines = sMs;
		nThreads = stateMachines.size();
		agent = a;
		rewards = new ArrayList<Integer>(nThreads);

		// Use thread pooling naively to minimize overhead from thread creation
		executor = Executors.newFixedThreadPool(nThreads);
		completionService = new ExecutorCompletionService<Integer>(executor);
	}

	List<Integer> performDepthCharges(MachineState state) {
		// Start depth charges
		for(int i = 0; i<nThreads; i++){
			Callable<Integer> depthCharge = new DepthCharge(stateMachines.get(i), state, agent);
			completionService.submit(depthCharge);	//TODO the callable stuff
		}

		// Wait for threads to end and accumulate rewards
		rewards.clear();
		getRewards(completionService);

		return rewards;
	}

	private void getRewards(CompletionService<Integer> completionService) {
		try {
            for (int i = 0;  i < nThreads; i++) {
                Future<Integer> fCharge = completionService.take();	//take is a blocking method
                rewards.add(fCharge.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
	}
}
