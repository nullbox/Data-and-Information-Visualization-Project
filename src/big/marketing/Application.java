package big.marketing;

import org.apache.log4j.Logger;

import big.marketing.controller.DataController;
import big.marketing.view.ControlsJPanel;
import big.marketing.view.GraphJPanel;
import big.marketing.view.PCoordinatesJPanel;
import big.marketing.view.WindowFrame;

public class Application {
	static Logger logger = Logger.getLogger(Application.class);

	/**
	 * Application class implements main method and initializes the main parts
	 * of the application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		logger.info("Starting eyeNet application: initializing controller and views");

		Settings.loadConfig();

		DataController controller = new DataController();

		// All panels have a reference to controller so changes in selections
		// and data can be passed to other views
		GraphJPanel graphPanel = new GraphJPanel(controller);
		PCoordinatesJPanel pCoordinatesPanel = new PCoordinatesJPanel(controller);
		ControlsJPanel controlsPanel = new ControlsJPanel(controller);

		// DataController implements observer pattern and pushes changes in data
		// and selections to JPanels
		controller.addObserver(graphPanel);
		controller.addObserver(pCoordinatesPanel);
		controller.addObserver(controlsPanel);

		WindowFrame frame = new WindowFrame();

		frame.add(graphPanel);
		frame.add(pCoordinatesPanel);
		frame.add(controlsPanel);
		
		// loop for testing switched graphs
		while (true){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			controller.getGephiController().loadSampleFile();
		}
		
	}

}
