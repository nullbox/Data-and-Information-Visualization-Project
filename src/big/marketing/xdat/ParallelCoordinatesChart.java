/*
 *  Copyright 2013, Enguerrand de Rochefort
 * 
 * This file is part of xdat.
 *
 * xdat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xdat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xdat.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package big.marketing.xdat;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * A representation of all relevant settings for a Parallel coordinates chart which is displayed on
 * a ChartFrame.
 * The data that was imported by the user can be displayed on a Parallel coordinates Chart.
 * The Parallel coordinates Chart is built by as many vertical Axes as Parameters are present in the underlying DataSheet and
 * each Axis represents one of these Parameters.
 * The Designs (or, in other words, rows of the data table) are represented by lines that connect points
 * on the Axes. Each Design's line crosses each Axis exactly at the ordinate that corresponds to the
 * value for the respective Parameter in the Design. This allows for displaying the whole DataSheet in just
 * one Chart, irrespective of how many dimensions it has.
 * The Chart also provides interactivity through a pair of draggable Filters that are present on
 * each Axis. Depending on the positions of these filters, certain Designs are filtered from the
 * display. This means that they are either displayed in a different color or hidden completely.
 * 
 * 
 * @see gui.frames.ChartFrame
 * @see Axis
 * @see Filter
 * @see data.Parameter
 * @see data.DataSheet
 * @see data.Design
 */
public class ParallelCoordinatesChart extends Chart {

	static Logger logger = Logger.getLogger(ParallelCoordinatesChart.class);

	/** Padding at the bottom of the chart. */
	static final int BOTTOM_PADDING = 20;

	/** The background color of this Chart. */
	private Color backGroundColor = Color.WHITE;

	/**
	 * The top margin of the Chart.
	 * specifies the distance from the top edge of the Chart to the top of the Axis labels.
	 */
	private int topMargin = 10;

	/** Specifies whether axis labels should be vertically offset. */
	private boolean verticallyOffsetAxisLabels = true;

	/** The vertical distance between two axis labels that are vertically offset to prevent overlap. */
	private int axisLabelVerticalDistance = 10;

	/**
	 * The axes on this Chart.
	 * For each Parameter in the DataSheet there is exactly one Axis.
	 * */
	private Vector<Axis> axes = new Vector<Axis>(0, 1);

	/**
	 * The design label font size.
	 * The design IDs are shown as labels next to the left-most Axis. This field specifies the font size for these labels.
	 */
	private int designLabelFontSize = 10;

	/** Specifies the line thickness for designs. */
	private int lineThickness = 1;

	/** Specifies the line thickness for selected designs. */
	private int selectedDesignsLineThickness = 1;

	/**
	 * The active design color.
	 * All Designs that are not filtered and do not belong to any Clusters are displayed in this Color.
	 * New Clusters are also given this Color by default.
	 * @see data.Design
	 * @see data.Cluster
	 * */
	private Color activeDesignColor = Color.GREEN;

	/**
	 * The selected design color.
	 * All Designs that are selected in the data sheet table are displayed in this color, except if only selected designs
	 * are displayed, in which case they are displayed in the color in which they would otherwise have been displayed in unselected state.
	 * */
	private Color selectedDesignColor = Color.BLUE;

	/**
	 * The filtered design color.
	 * All Designs that are filtered are displayed in this Color.
	 * This is only relevant if {@link #showFilteredDesigns} is true.
	 * @see data.Design
	 * */
	private Color filteredDesignColor = Color.GRAY;

	/** The color in which the Filters are shown on this Chart. */
	private Color filterColor = Color.RED;

	/** Specifies whether the design IDs next to the left-most Axis should be shown. */
	private boolean showDesignIDs = false;

	/**
	 * Switch that enables displaying filtered designs.
	 * If this switch is true, designs are displayed in the Color specified by {@link #filteredDesignColor}
	 */
	private boolean showFilteredDesigns = false;

	/**
	 * Switch that reduces display of designs to the selected ones.
	 * If this switch is true, only selected designs are displayed
	 */
	private boolean showOnlySelectedDesigns = false;

	/** The height of the triangles that represent the filter in pixels. */
	private int filterHeight = 10;

	/**
	 * The width of one half triangle that represents a filter in pixels. In other words,
	 * the filter triangle will be twice as large as the value entered here.
	 */
	private int filterWidth = 7;

	/**
	 * Instantiates a new parallel coordinates chart.
	 * @param dataSheet the data sheet
	 */
	public ParallelCoordinatesChart(DataSheet dataSheet, Dimension dimensions) {
		super(dataSheet, dimensions);

		for (int i = 0; i < dataSheet.getParameterCount(); i++) {
			Axis axis = new Axis(dataSheet, this, dataSheet.getParameter(i));
			this.addAxis(axis);
		}
		for (int i = 0; i < dataSheet.getParameterCount(); i++) {
			this.axes.get(i).addFilters();
		}
	}

	/**
	 * Determines the width of this Chart.
	 * @return the width of this Chart
	 */
	public int getWidth() {
		int width = 0;
		if (this.getAxis(0).isActive()) {
			width = width + (int) (0.5 * this.getAxis(0).getWidth());
		}
		for (int i = 1; i < this.getAxisCount(); i++) {
			if (this.getAxis(i).isActive()) {
				width = width + (int) (this.getAxis(i).getWidth());
			}
		}
		return width;
	}

	public int getAxisWidth() {
		int width = 0;
		int axisCount = 0;

		// TODO Count more accurately taking in count the margins on the chart
		for (int i = 0; i < this.getDataSheet().getParameterCount(); i++) {
			axisCount++;
		}

		width = (int) (this.getDimensions().getWidth() / axisCount);
		return width;
	}

	/**
	 * Determines the height of this Chart.
	 * @return the height of this Chart
	 */
	public int getHeight() {
		int height = getAxisTopPos() + getAxisHeight();
		return height;
	}

	/**
	 * Gets the largest width of all Axis widths.
	 * @return the largest width of all Axis widths.
	 */
	public int getAxisMaxWidth() {
		int width = 0;
		for (int i = 0; i < this.getAxisCount(); i++) {
			if (this.getAxis(i).isActive()) {
				if (width < this.getAxis(i).getWidth()) {
					width = this.getAxis(i).getWidth();
				}
			}
		}
		return width;
	}

	/**
	 * Changes the axis width of all axes by the same amount.
	 * @param deltaWidth the axis width increment
	 */
	public void incrementAxisWidth(int deltaWidth) {
		for (int i = 0; i < axes.size(); i++) {
			axes.get(i).setWidth(Math.max(0, axes.get(i).getWidth() + deltaWidth));
		}
	}

	/**
	 * Sets the axis color.
	 * @param color the new axis color
	 */
	public void setAxisColor(Color color) {
		for (int i = 0; i < axes.size(); i++) {
			axes.get(i).setAxisColor(color);
		}
	}

	/**
	 * Gets the position in pixels of the top of the Axes of this Chart.
	 * @return the position in pixels of the top of the Axes of this Chart.
	 */
	public int getAxisTopPos() {
		int topPos;
		if (this.verticallyOffsetAxisLabels) {
			topPos = 2 * getMaxAxisLabelFontSize() + this.axisLabelVerticalDistance + this.getTopMargin() * 2 + this.getFilterHeight();
		} else {
			topPos = getMaxAxisLabelFontSize() + this.getTopMargin() * 2 + this.getFilterHeight();
		}
		return topPos;
	}

	/**
	 * Gets an Axis by its index.
	 * @param index the index
	 * @return the Axis with index index
	 */
	public Axis getAxis(int index) {
		return axes.get(index);
	}

	/**
	 * Gets an Axis by its name.
	 * @param parameterName the parameter name
	 * @return the Axis
	 */
	public Axis getAxis(String parameterName) {
		for (int i = 0; i < this.axes.size(); i++) {
			if (parameterName.equals(this.axes.get(i).getParameter().getName())) {
				return this.axes.get(i);
			}
		}
		throw new IllegalArgumentException("Axis " + parameterName + " not found");
	}

	/**
	 * Gets the largest Axis label font size on this Chart.
	 * @return the largest axis label font size on this Chart
	 */
	public int getMaxAxisLabelFontSize() {
		int maxAxisLabelFontSize = 0;
		for (int i = 0; i < axes.size(); i++) {
			if (maxAxisLabelFontSize < axes.get(i).getAxisLabelFontSize()) {
				maxAxisLabelFontSize = axes.get(i).getAxisLabelFontSize();
			}
		}
		return maxAxisLabelFontSize;
	}

	/**
	 * Gets the active axis count.
	 * @return the active axis count
	 */
	public int getActiveAxisCount() {
		int axesCount = 0;
		for (int i = 0; i < axes.size(); i++) {
			if (axes.get(i).isActive())
				axesCount++;
		}
		return axesCount;
	}

	/**
	 * Gets the axis count.
	 * @return the axis count
	 */
	public int getAxisCount() {
		return axes.size();
	}

	/**
	 * Gets the vertical distance between two axis labels that are vertically offset to prevent overlap.
	 * @return the axis count
	 */
	public int getAxisLabelVerticalDistance() {
		return axisLabelVerticalDistance;
	}

	/**
	 * Sets the vertical distance between two axis labels that are vertically offset to prevent overlap.
	 * @param axisLabelVerticalDistance the vertical axis label distance
	 */

	public void setAxisLabelVerticalDistance(int axisLabelVerticalDistance) {
		this.axisLabelVerticalDistance = axisLabelVerticalDistance;
	}

	/**
	 * Checks, whether axis labels should be vertically offset.
	 * @return true, if axis labels should be vertically offset
	 */
	public boolean isVerticallyOffsetAxisLabels() {
		return verticallyOffsetAxisLabels;
	}

	/**
	 * Sets whether axis labels should be vertically offset.
	 * @param verticallyOffsetAxisLabels specifies whether designs should be shown
	 */

	public void setVerticallyOffsetAxisLabels(boolean verticallyOffsetAxisLabels) {
		this.verticallyOffsetAxisLabels = verticallyOffsetAxisLabels;
	}

	/**
	 * Adds the axis.
	 * @param axis the axis
	 */
	public void addAxis(Axis axis) {
		this.axes.add(axis);
	}

	/**
	 * Gets the Axis height in pixels.
	 * @return the height
	 */
	public int getAxisHeight() {
		return this.getDimensions().height - this.getAxisTopPos() - BOTTOM_PADDING;
	}

	/**
	 * Gets the design label font size.
	 * @return the design label font size
	 */
	public int getDesignLabelFontSize() {
		return designLabelFontSize;
	}

	/**
	 * Sets the design label font size.
	 * @param designLabelFontSize the new design label font size
	 */
	public void setDesignLabelFontSize(int designLabelFontSize) {
		this.designLabelFontSize = designLabelFontSize;
	}

	/**
	 * Gets the line thickness.
	 * @return the line thickness
	 */
	public int getLineThickness() {
		return lineThickness;
	}

	/**
	 * Sets the line thickness.
	 * @param lineThickness the new line thickness
	 */
	public void setLineThickness(int lineThickness) {
		this.lineThickness = lineThickness;
	}

	/**
	 * Gets the selected design line thickness.
	 * @return the design line thickness
	 */
	public int getSelectedDesignsLineThickness() {
		return selectedDesignsLineThickness;
	}

	/**
	 * Sets the selected design line thickness.
	 * @param selectedDesignsLineThickness the selected design line thickness
	 */
	public void setSelectedDesignsLineThickness(int selectedDesignsLineThickness) {
		this.selectedDesignsLineThickness = selectedDesignsLineThickness;
	}

	/**
	 * Gets the default design color.
	 * @param designActive the design active
	 * @return the default design color
	 */
	public Color getDefaultDesignColor(boolean designActive) {
		if (designActive)
			return activeDesignColor;
		else
			return filteredDesignColor;
	}

	/**
	 * Sets the active design color.
	 * @param activeDesignColor the new active design color
	 */
	public void setActiveDesignColor(Color activeDesignColor) {
		this.activeDesignColor = activeDesignColor;
	}

	/**
	 * Sets the filtered design color.
	 * @param filteredDesignColor the new filtered design color
	 */
	public void setFilteredDesignColor(Color filteredDesignColor) {
		this.filteredDesignColor = filteredDesignColor;
	}

	/**
	 * Sets the selected design color.
	 */
	public Color getSelectedDesignColor() {
		return selectedDesignColor;
	}

	/**
	 * Checks if design IDs should be shown.
	 * @return true, if design IDs should be shown.
	 */
	public boolean isShowDesignIDs() {
		return showDesignIDs;
	}

	/**
	 * Checks whether filtered designs should be shown.
	 * @return true, if filtered designs should be shown.
	 */
	public boolean isShowFilteredDesigns() {
		return showFilteredDesigns;
	}

	/**
	 * Checks whether only selected designs should be shown.
	 * @return true, if only selected designs should be shown.
	 */
	public boolean isShowOnlySelectedDesigns() {
		return showOnlySelectedDesigns;
	}

	/**
	 * Gets the filter color.
	 * @return the filter color
	 */
	public Color getFilterColor() {
		return filterColor;
	}

	/**
	 * Gets the top margin.
	 * @return the top margin
	 */
	public int getTopMargin() {
		return topMargin;
	}

	/**
	 * Gets the back ground color.
	 * @return the back ground color
	 */
	public Color getBackGroundColor() {
		return backGroundColor;
	}

	/**
	 * Gets the filter height.
	 * @return the filter height
	 */
	public int getFilterHeight() {
		return filterHeight;
	}

	/**
	 * Sets the filter height.
	 * @param filterHeight the new filter height
	 */
	public void setFilterHeight(int filterHeight) {
		this.filterHeight = filterHeight;
	}

	/**
	 * Gets the filter width.
	 * @return the filter width
	 */
	public int getFilterWidth() {
		return filterWidth;
	}

	/**
	 * Sets the filter width.
	 * @param filterWidth the new filter width
	 */
	public void setFilterWidth(int filterWidth) {
		this.filterWidth = filterWidth;
	}

	/**
	 * Apply all filters.
	 */
	public void applyAllFilters() {
		for (int i = 0; i < this.getDataSheet().getParameterCount(); i++) {
			this.axes.get(i).applyFilters();
		}
	}

	/**
	 * Autofits all axes.
	 */
	public void autofitAllAxes() {
		for (int i = 0; i < this.axes.size(); i++) {
			this.axes.get(i).autofit();
		}
	}
}
