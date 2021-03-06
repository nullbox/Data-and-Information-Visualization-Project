package big.marketing.controller;

import org.apache.log4j.Logger;

import big.marketing.view.ControlsJPanel;

public class Player extends Thread {
	static Logger logger = Logger.getLogger(Player.class);
	private DataController dataController;
	private int currentTime, stepSize, sleepMillis;
	private volatile boolean isPlaying;

	public Player(DataController dc, int startTime, int stepSize, int sleepMillis) {
		super("PlayThread");
		this.dataController = dc;
		this.currentTime = Math.max(ControlsJPanel.QW_MIN, Math.min(startTime, ControlsJPanel.QW_MAX));
		this.stepSize = stepSize;
		this.sleepMillis = sleepMillis;
		this.isPlaying = false;
	}

	public void stopPlaying() {
		this.isPlaying = false;
	}

	public void startPlaying() {
		this.isPlaying = true;
		this.start();
	}

	@Override
	public void run() {
		dataController.playStateChanged();
		while (isPlaying) {
			long start = System.currentTimeMillis();
			logger.info("Playstep: Moving to time: " + currentTime);
			dataController.setTime(currentTime);
			currentTime += stepSize;
			if (currentTime > ControlsJPanel.QW_MAX) {
				logger.info("Reached maximum time, stopping...");
				isPlaying = false;
			} else if (currentTime < ControlsJPanel.QW_MIN) {
				logger.info("Reached minimum time, stopping...");
				isPlaying = false;
			}
			long sleepTime = sleepMillis - (System.currentTimeMillis() - start);
			//			sleepTime = sleepMillis;
			if (sleepTime > 0) {
				logger.info("Waiting " + sleepTime + " ms");
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		logger.info("Stopped playing");
		dataController.playStateChanged();
	}

}
