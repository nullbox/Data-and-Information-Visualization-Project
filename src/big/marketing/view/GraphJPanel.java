package big.marketing.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.ProcessingTarget;
import org.gephi.preview.types.DependantOriginalColor;
import org.openide.util.Lookup;

import processing.core.PApplet;
import big.marketing.controller.DataController;

public class GraphJPanel extends JPanel implements Observer {
	static Logger logger = Logger.getLogger(GraphJPanel.class);

	private static final long serialVersionUID = -7417639995072699909L;
	private final DataController controller;
	private PApplet applet;
	private ProcessingTarget target;

	public void setContent(ProcessingTarget target) {
		logger.info("Init applet");
		this.target = target;
		applet = target.getApplet();
		applet.init();
		//		removeAll();
		add(applet, BorderLayout.CENTER);
		controller.getGephiController().render(target);
	}

	public GraphJPanel(DataController controller) {
		this.controller = controller;
		setLayout(new BorderLayout());
		this.controller.getGephiController().setGraphPanel(this);
	}

	public void layoutGraph() {

		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		ForceAtlas2 layout = new ForceAtlas2(null);
		//		YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
		layout.setGraphModel(graphModel);
		layout.initAlgo();
		layout.resetPropertiesValues();
		//		layout.setOptimalDistance(200f);
		layout.setEdgeWeightInfluence(0.0);
		layout.setScalingRatio(50.0);
		layout.setLinLogMode(false);

		for (int i = 0; i < 100 && layout.canAlgo(); i++) {
			layout.goAlgo();
		}
		layout.endAlgo();

	}

	@Override
	public void update(Observable o, Object arg) {

		if (arg instanceof PreviewController) {

			layoutGraph();

			PreviewController previewController = (PreviewController) arg;
			PreviewModel previewModel = previewController.getModel();
			previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
			previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.WHITE));
			previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
			previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
			previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 10f);
			previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.BLACK);

			previewController.refreshPreview();

			if (target != null) {
				target.refresh();
				target.resetZoom();
			}
		}
	}

}
