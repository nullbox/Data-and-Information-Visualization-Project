package big.marketing.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.apache.log4j.Logger;

import big.marketing.Settings;
import big.marketing.controller.gephi.GephiController;
import big.marketing.data.DataType;
import big.marketing.data.Node;
import big.marketing.data.QueryWindowData;
import big.marketing.reader.NetworkReader;
import big.marketing.reader.ZipReader;

public class DataController extends Observable implements Runnable {
	// http://docs.oracle.com/javase/7/docs/api/java/util/Observable.html

	static Logger logger = Logger.getLogger(DataController.class);
	private GephiController gc;
	private MongoController mc;

	// qWindow size in milliseconds, default value 1 hour
	public static int QUERYWINDOW_SIZE = 3600;

	// currentQueryWindow stores the data returned from mongo
	private QueryWindowData currentQueryWindow;
	private List<Node> network;

	private Node[] selectedNodes = null;

	private Thread readingThread, processingThread;
	private Player player;

	/*
	 * TODO I think we can clarify this code pretty much
	 * maybe we could use Map<Node, boolean> to denote which Nodes are selected currently
	 * or make getNetwork to return node[] and then convert it to Map<Node, boolean> network that works as a selected nodes
	 * Question? What is the purpose of ipMap
	 */

	public DataController() {
		loadSettings();
		this.mc = new MongoController();
		this.gc = new GephiController(this);
		network = mc.getNetwork();
		setSelectedNodes((Node[]) network.toArray(new Node[network.size()]));
		if (network.isEmpty()) {
			logger.warn("Loading Nodes from database failed!");
		}
		this.addObserver(gc);
	}

	public void selectNodesOfType(boolean selectAdmin, boolean selectServer, boolean selectWorkstations) {
		List<Node> selected = new ArrayList<Node>();
		for (Node n : network) {
			if (selectWorkstations && n.isWorkstation() || selectServer && n.isServer() || selectAdmin && n.isAdministator())
				selected.add(n);
		}
		selectedNodes = (Node[]) selected.toArray(new Node[selected.size()]);
		setChanged();
		notifyObservers(selectedNodes);
	}

	public void readData() {
		readingThread = new Thread(this, "DataReader");
		readingThread.start();
	}

	public void processData() {
		DataProcessor dp = new DataProcessor(this.mc, DataType.FLOW, DataType.IPS, DataType.HEALTH);
		processingThread = new Thread(dp, "ProcessingThread");
		processingThread.start();
	}

	public List<Node> getNetwork() {
		return network;
	}

	public void setTime(int newTime) {
		setChanged();
		notifyObservers(newTime);
	}

	public void playStopButtonPressed(int startTime, int stepSize) {
		if (player != null && player.isAlive()) {
			// actually playing
			player.stopPlaying();
			logger.info("Waiting for current query to finish, then stopping playing");
		} else {
			// no player yet or playing finished
			player = new Player(this, startTime, stepSize, 1000);
			player.startPlaying();
			logger.info("Started playing at " + startTime + " with stepSize " + stepSize);
		}
	}

	public void playStateChanged() {
		setChanged();
		notifyObservers("PlayStateChanged");
	}

	public void run() {

		NetworkReader nReader = new NetworkReader(this.mc);
		ZipReader zReader = new ZipReader(this.mc);
		// Handling all reader errors here
		try {
			network = nReader.readNetwork();
			zReader.read(DataType.FLOW, DataType.IPS, DataType.HEALTH);
			mc.flushBuffers();
			logger.info("Finished Reading Data");
		} catch (Exception err) {
			logger.error("Cannot read data", err);
		}
	}

	/**
	 * Moves QueryWindow to certain position in time and queries data to qWindow
	 * variables from mongo Hides mongo implementation details from views
	 * 
	 * @param date in milliseconds marking the center point of the query
	 * @return true if data was stored into queryWindow variables otherwise false
	 */
	public boolean moveQueryWindow(int time) {
		setChanged();
		notifyObservers("SkipNextNotify");
		resetSelectedNodes();
		int start = time - QUERYWINDOW_SIZE / 2, end = time + QUERYWINDOW_SIZE / 2;
		long startTime = System.currentTimeMillis();

		currentQueryWindow = new QueryWindowData(null, null, null, network);
		currentQueryWindow.setFlow(mc.getConstrainedEntries(DataType.FLOW, "time", start, end));
		currentQueryWindow.setIps(mc.getConstrainedEntries(DataType.IPS, "time", start, end));
		currentQueryWindow.setHealth(mc.getConstrainedEntries(DataType.HEALTH, "time", time - 60, time + 60));

		logger.info("Moved qWindow to " + time + ", Query took " + (System.currentTimeMillis() - startTime) + " ms,  Window size: "
		      + QUERYWINDOW_SIZE + " sec, Flow: " + currentQueryWindow.getFlowData().size() + " objects, Health: "
		      + currentQueryWindow.getHealthData().size() + " objects, IPS: " + currentQueryWindow.getIPSData().size() + " objects");

		setChanged();
		notifyObservers(currentQueryWindow);
		System.gc();
		return !currentQueryWindow.isEmpty();
	}

	/**
	 * This is the same as moveQueryWindow, but queries only certain type of data
	 * @param time date in milliseconds marking the center point of the query
	 * @param date in milliseconds marking the center point of the query
	 * @return true if data was stored into queryWindow variables otherwise false
	 */
	public boolean moveQueryWindow(int time, DataType t) {
		setChanged();
		notifyObservers("SkipNextNotify");
		resetSelectedNodes();
		logger.info("Moving qWindow to " + time);
		int start = time - QUERYWINDOW_SIZE / 2, end = time + QUERYWINDOW_SIZE / 2;
		long startTime = System.currentTimeMillis();
		List<Object> newEntries = mc.getConstrainedEntries(t, "time", start, end);
		switch (t) {
		case FLOW:
			currentQueryWindow.setFlow(newEntries);
			break;

		case HEALTH:
			currentQueryWindow.setHealth(newEntries);
			break;

		case IPS:
			currentQueryWindow.setIps(newEntries);
			break;

		case DESCRIPTION:
			return false;
		}

		logger.info("Moved qWindow to " + time + ", Query took " + (System.currentTimeMillis() - startTime) + " ms,  Window size: "
		      + QUERYWINDOW_SIZE + " sec, " + t.name() + ": " + newEntries.size() + " objects");
		setChanged();
		notifyObservers(currentQueryWindow);
		System.gc();
		return !currentQueryWindow.isEmpty();
	}

	private void loadSettings() {
		QUERYWINDOW_SIZE = Settings.getInt("controller.querywindow.size");
	}

	public void setKeepLayout(boolean keepLayout) {
		setChanged();
		notifyObservers(keepLayout);
	}

	public void resetSelectedNodes() {
		setSelectedNodes((Node[]) network.toArray(new Node[network.size()]));
		logger.info("Reset selection");
		setChanged();
		notifyObservers("ResetSelection");
	}

	public void setSelectedNodes(Node[] selected) {
		this.selectedNodes = selected;

		setChanged();
		notifyObservers(selectedNodes);
	}

	public Node[] getSelectedNodes() {
		return selectedNodes;
	}

	public QueryWindowData getCurrentQueryWindow() {
		return currentQueryWindow;
	}

	public MongoController getMongoController() {
		return mc;
	}

	public GephiController getGephiController() {
		return gc;
	}
}
