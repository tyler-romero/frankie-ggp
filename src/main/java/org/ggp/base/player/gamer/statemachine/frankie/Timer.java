package org.ggp.base.player.gamer.statemachine.frankie;

public class Timer {
	private long finishBy;
	private long timeout;
	public boolean did_timeout;

	public Timer(){
		finishBy = 0;
		timeout = 0;
		did_timeout = false;
	}

	public void initTimer(long timeout_, long finishBy_) {
		timeout = timeout_;
		finishBy = finishBy_;
		did_timeout = false;
	}

	public boolean isOutOfTime(){
		long currentTime = System.currentTimeMillis();
		if(currentTime > finishBy) {
			did_timeout = true;
			return true;
		}
		else return false;
	}

	public boolean isExpired(){
		long currentTime = System.currentTimeMillis();
		if(currentTime > timeout) return true;
		else return false;
	}
}
