package big.marketing.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import big.marketing.Settings;
import big.marketing.controller.DataController;
import big.marketing.data.DataType;
import big.marketing.data.Node;

public class ControlsJPanel extends JPanel implements Observer {
	private static final long serialVersionUID = 7478563340170330453L;
	private JSlider qWindowSlider;
	private JPanel buttonPanel;
	private JButton playPauseButton, resetButton;
	private ChartPanel chartPanel;
	private JLabel currentTimeLabel;
	private JSpinner playSpeedSpinner;
	private JCheckBox adminBox, serverBox, workstationBox, layoutBox;
	private JComboBox<DataType> typeCombo;
	private Map<DataType, XYDataset> datasetCache;
	private JLabel nodeCountJLabel;

	private static SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM HH:mm", Locale.ENGLISH);
	static Logger logger = Logger.getLogger(ControlsJPanel.class);
	public static int QW_MIN = 0, QW_MAX = 1217384;
	public static boolean FAST_START = false;

	public ControlsJPanel(final DataController controller) {
		loadSettings();
		this.setLayout(new BorderLayout());
		datasetCache = new HashMap<DataType, XYDataset>();
		playPauseButton = new JButton("Play");
		playPauseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.info(playPauseButton.getText() + " button press");
				controller.playStopButtonPressed(qWindowSlider.getValue(), (Integer) playSpeedSpinner.getValue());
			}
		});
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		currentTimeLabel = new JLabel(" ");
		playSpeedSpinner = new JSpinner(new SpinnerNumberModel(3600, 60, Integer.MAX_VALUE, 100));
		buttonPanel.add(new JLabel("Play speed:"));
		buttonPanel.add(playSpeedSpinner);
		buttonPanel.add(playPauseButton);
		buttonPanel.add(currentTimeLabel);

		add(buttonPanel, BorderLayout.SOUTH);

		ActionListener selectionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				controller.selectNodesOfType(adminBox.isSelected(), serverBox.isSelected(), workstationBox.isSelected());

			}
		};

		adminBox = new JCheckBox("Admin");
		adminBox.setSelected(true);
		adminBox.addActionListener(selectionListener);
		buttonPanel.add(adminBox);

		serverBox = new JCheckBox("Servers");
		serverBox.setSelected(true);
		serverBox.addActionListener(selectionListener);
		buttonPanel.add(serverBox);

		workstationBox = new JCheckBox("Workstations");
		workstationBox.setSelected(true);
		workstationBox.addActionListener(selectionListener);
		buttonPanel.add(workstationBox);

		typeCombo = new JComboBox<DataType>(new DataType[] { DataType.FLOW, DataType.IPS });
		typeCombo.setEditable(true);
		typeCombo.addActionListener(new ActionListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JComboBox<DataType> source = (JComboBox<DataType>) arg0.getSource();
				DataType type = (DataType) source.getSelectedItem();
				XYDataset entry = datasetCache.get(type);
				if (entry == null) {
					entry = controller.getMongoController().getHistogramTCollection(type);
					datasetCache.put(type, entry);
				}
				XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
				plot.setDataset(entry);
			}
		});
		buttonPanel.add(typeCombo);
		resetButton = new JButton("Reset selection");
		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				controller.resetSelectedNodes();

			}
		});
		buttonPanel.add(resetButton);

		layoutBox = new JCheckBox("Keep node layout");
		layoutBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.setKeepLayout(layoutBox.isSelected());
			}
		});
		buttonPanel.add(layoutBox);

		nodeCountJLabel = new JLabel(" Displaying: All nodes");
		buttonPanel.add(nodeCountJLabel);

		TimeSeriesCollection data = new TimeSeriesCollection();
		if (!FAST_START)
			data = controller.getMongoController().getHistogramTCollection(DataType.FLOW);
		datasetCache.put(DataType.FLOW, data);
		chartPanel = new ChartPanel(showChart(data), WindowFrame.FRAME_WIDTH, 420, 300, 200, 1920, 600, false, false, false, false, false,
		      false);
		chartPanel.setLayout(new BorderLayout());
		qWindowSlider = new JSlider(JSlider.HORIZONTAL, QW_MIN, QW_MAX, QW_MIN + 1);
		qWindowSlider.setOpaque(false);
		qWindowSlider.setUI(new QuerySliderUI(qWindowSlider));

		qWindowSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int newTime = source.getValue();
				Date date = new Date(newTime * 1000L);
				currentTimeLabel.setText(formatter.format(date));
				if (!source.getValueIsAdjusting()) {
					controller.moveQueryWindow((int) source.getValue());
				}
			}
		});
		chartPanel.add(qWindowSlider, BorderLayout.CENTER);
		add(chartPanel);
		setPreferredSize(new Dimension(WindowFrame.FRAME_WIDTH, (int) (WindowFrame.FRAME_HEIGHT * 0.3)));
		if (FAST_START)
			qWindowSlider.setValue(1365629513);
		else
			qWindowSlider.setValue(QW_MIN);
	}

	public void setCurrentTime(int i) {
		// clamp time between QW_MIN and QW_MAX
		i = Math.min(QW_MAX, Math.max(QW_MIN, i));
		qWindowSlider.setValue(i);
	}

	private void switchPlayButtonName() {
		playPauseButton.setText(playPauseButton.getText().equals("Play") ? "Pause" : "Play");
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof IntervalXYDataset) {
			showChart((IntervalXYDataset) arg);
		} else if (arg instanceof Integer) {
			int newTime = (Integer) arg;
			setCurrentTime(newTime);
		} else if ("PlayStateChanged".equals(arg)) {
			switchPlayButtonName();
		} else if (arg instanceof Node[]) {
			Node[] selectedNodes = (Node[]) arg;
			nodeCountJLabel.setText(" Displaying: " + selectedNodes.length + " nodes");
		} else if ("ResetSelection".equals(arg)) {
			adminBox.setSelected(true);
			serverBox.setSelected(true);
			workstationBox.setSelected(true);
		}
	}

	public JFreeChart showChart(IntervalXYDataset dataset) {

		JFreeChart chart = ChartFactory.createHistogram("", "", "", dataset, PlotOrientation.VERTICAL, true, false, false);

		chart.setBackgroundPaint(Color.white);
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setRangeAxis(new LogarithmicAxis("packetCount"));

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setVisible(false);

		rangeAxis.setAutoTickUnitSelection(true);
		rangeAxis.setAutoRangeIncludesZero(true);
		DateAxis dAxis = new DateAxis();
		dAxis.setRange((long) ControlsJPanel.QW_MIN * 1000L, (long) ControlsJPanel.QW_MAX * 1000L);
		plot.setDomainAxis(dAxis);

		plot.setRenderer(new XYAreaRenderer());
		plot.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
		chart.setPadding(new RectangleInsets(0, 0, 0, 0));

		return chart;
	}

	private void loadSettings() {
		QW_MIN = Settings.getInt("qwindow.data.min");
		QW_MAX = Settings.getInt("qwindow.data.max");
		FAST_START = Settings.getInt("controller.startup.fast") == 1;
	}

}
