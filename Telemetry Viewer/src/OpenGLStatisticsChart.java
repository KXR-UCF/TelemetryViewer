import javax.swing.SwingUtilities;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import com.jogamp.opengl.GL2ES3;

/**
 * Displays a table of current values and statistics of one or more datasets.
 * 
 * User settings:
 *     How many datasets.
 *     The datasets.
 *     Show one or more of the following:
 *         Current value
 *         Minimum value
 *         Maximum value
 *         Mean
 *         Median
 *         Standard Deviation
 *         90th Percentile
 */
public class OpenGLStatisticsChart extends PositionedChart {
	
	// data
	int datasetsCount;
	Samples[] samplesFromDataset;
	int durationSampleCount;
	long durationMilliseconds;
	
	// settings
	boolean showCurrentValues;
	boolean showMinimums;
	boolean showMaximums;
	boolean showMeans;
	boolean showMedians;
	boolean showStandardDeviations;
	boolean showPercentile;
	boolean showDuration;
	String showAs;
	
	// control widgets
	WidgetTextfieldInteger datasetsCountWidget;
	WidgetDatasets datasetWidget;
	WidgetDuration durationWidget;
	WidgetCheckbox currentValuesWidget;
	WidgetCheckbox minimumWidget;
	WidgetCheckbox maximumWidget;
	WidgetCheckbox meanWidget;
	WidgetCheckbox medianWidget;
	WidgetCheckbox standardDeviationWidget;
	WidgetCheckbox percentileWidget;
	WidgetCheckbox showDurationWidget;
	
	@Override public String toString() {
		
		return "Statistics";
		
	}
	
	public OpenGLStatisticsChart(int x1, int y1, int x2, int y2) {
		
		super(x1, y1, x2, y2);
		
		samplesFromDataset = new Samples[1];
		samplesFromDataset[0] = new Samples();
		
		datasetsCountWidget = new WidgetTextfieldInteger("Number of Datasets", 1, 1, 12, newNumber -> {
		                                                                                 	samplesFromDataset = new Samples[newNumber];
		                                                                                 	for(int i = 0; i < newNumber; i++)
		                                                                                 		samplesFromDataset[i] = new Samples();
		                                                                                 	String[] labels = new String[newNumber];
		                                                                                 	for(int i = 0; i < newNumber; i++)
		                                                                                 		labels[i] = "";
		                                                                                 	datasetWidget.setDatasetLabels(labels);
		                                                                                 	if(datasetsCount != newNumber)
		                                                                                 		ConfigureView.instance.redrawIfUsedFor(this);
		                                                                                 	datasetsCount = newNumber;
		                                                                                 });
		
		datasetWidget = new WidgetDatasets(1,
		                                   new String[] {""},
		                                   newDatasets -> datasets = newDatasets);
		
		durationWidget = new WidgetDuration(1000,
		                                    1,
		                                    Integer.MAX_VALUE,
		                                    this,
		                                    newType -> {
		                                    	showAs = newType;
		                                    	SwingUtilities.invokeLater(() -> {
	                                    			sampleCountMode = newType.equals("Sample Count");
			                                    	if(sampleCountMode)
			                                    		durationSampleCount = durationWidget.getSampleCount();
			                                    	else
			                                    		durationMilliseconds = durationWidget.getMilliseconds();
		                                    	});

		                                    });
		
		currentValuesWidget     = new WidgetCheckbox("Show Current Value",      true, isSelected -> showCurrentValues = isSelected);
		minimumWidget           = new WidgetCheckbox("Show Minimum",            true, isSelected -> showMinimums = isSelected);
		maximumWidget           = new WidgetCheckbox("Show Maximum",            true, isSelected -> showMaximums = isSelected);
		meanWidget              = new WidgetCheckbox("Show Mean",               true, isSelected -> showMeans = isSelected);
		medianWidget            = new WidgetCheckbox("Show Median",             true, isSelected -> showMedians = isSelected);
		standardDeviationWidget = new WidgetCheckbox("Show Standard Deviation", true, isSelected -> showStandardDeviations = isSelected);
		percentileWidget        = new WidgetCheckbox("Show 90th Percentile",    true, isSelected -> showPercentile = isSelected);
		showDurationWidget      = new WidgetCheckbox("Show Duration Label",     true, isSelected -> showDuration = isSelected);

		widgets = new Widget[14];
		widgets[0]  = datasetsCountWidget;
		widgets[1]  = datasetWidget;
		widgets[2]  = null;
		widgets[3]  = durationWidget;
		widgets[4]  = null;
		widgets[5]  = currentValuesWidget;
		widgets[6]  = minimumWidget;
		widgets[7]  = maximumWidget;
		widgets[8]  = meanWidget;
		widgets[9]  = medianWidget;
		widgets[10] = standardDeviationWidget;
		widgets[11] = percentileWidget;
		widgets[12] = null;
		widgets[13] = showDurationWidget;		
	}
	
	@Override public EventHandler drawChart(GL2ES3 gl, float[] chartMatrix, int width, int height, long endTimestamp, int endSampleNumber, double zoomLevel, int mouseX, int mouseY) {
		
		EventHandler handler = null;
		
		// get the samples
		int trueLastSampleNumber = datasets.get(0).connection.getSampleCount() - 1;
		int lastSampleNumber = -1;
		int firstSampleNumber = -1;
		if(sampleCountMode) {
			lastSampleNumber = Integer.min(endSampleNumber, trueLastSampleNumber);
			firstSampleNumber = endSampleNumber - (int) Math.round(durationSampleCount * zoomLevel) + 1;
		} else {
			lastSampleNumber = datasets.get(0).controller.getClosestSampleNumberAtOrBefore(endTimestamp, trueLastSampleNumber);
			firstSampleNumber = datasets.get(0).controller.getClosestSampleNumberAfter(endTimestamp - Math.round(durationMilliseconds * zoomLevel));
		}

		int sampleCount = 0;
		if(firstSampleNumber < 0)
			firstSampleNumber = 0;
		if(firstSampleNumber > lastSampleNumber)
			firstSampleNumber = lastSampleNumber;
		sampleCount = lastSampleNumber - firstSampleNumber + 1;
		if(lastSampleNumber < 0)
			sampleCount = 0;
		String durationLabel = sampleCountMode             ? "(" + sampleCount + " Samples)" :
		                       showAs.equals("Timestamps") ? "(" + SettingsController.formatTimestampToMilliseconds(datasets.get(0).controller.getTimestamp(firstSampleNumber)).replace('\n', ' ') + " to " + SettingsController.formatTimestampToMilliseconds(datasets.get(0).controller.getTimestamp(lastSampleNumber)).replace('\n', ' ') + ")" :
		                                                     "(" + (datasets.get(0).controller.getTimestamp(lastSampleNumber) - datasets.get(0).controller.getTimestamp(firstSampleNumber)) + " ms)";
		
		if(sampleCount > 0)
			for(int i = 0; i < datasetsCount; i++)
				datasets.get(i).getSamples(firstSampleNumber, lastSampleNumber, samplesFromDataset[i]);
		
		// determine the text to display
		int lineCount = 1; // always show the dataset labels
		if(showCurrentValues)      lineCount++;
		if(showMinimums)           lineCount++;
		if(showMaximums)           lineCount++;
		if(showMeans)              lineCount++;
		if(showMedians)            lineCount++;
		if(showStandardDeviations) lineCount++;
		if(showPercentile)         lineCount++;
		String[][] text = new String[datasetsCount + 1][lineCount];
		
		// first column of text are the labels, but don't label the dataset name or current value because that's obvious
		int line = 0;
		text[0][line++] = "";
		if(showCurrentValues)      text[0][line++] = "";
		if(showMinimums)           text[0][line++] = "Minimum";
		if(showMaximums)           text[0][line++] = "Maximum";
		if(showMeans)              text[0][line++] = "Mean";
		if(showMedians)            text[0][line++] = "Median";
		if(showStandardDeviations) text[0][line++] = "Std Dev";
		if(showPercentile)         text[0][line++] = "90th Pctl";
		
		// subsequent columns of text are the dataset names and numeric values
		if(sampleCount > 0)
			for(int datasetN = 0; datasetN < datasetsCount; datasetN++) {
				Dataset dataset = datasets.get(datasetN);
				Samples samples = samplesFromDataset[datasetN];
				double[] doubles = new double[samples.buffer.capacity()];
				for(int i = 0; i < samples.buffer.capacity(); i++)
					doubles[i] = (double) samples.buffer.get(i);
				DescriptiveStatistics stats = new DescriptiveStatistics(doubles);
				
				int column = datasetN + 1;
				line = 0;
				text[column][line++] = dataset.name;
				if(showCurrentValues)      text[column][line++] = ChartUtils.formattedNumber(samples.buffer.get(samples.buffer.capacity() - 1), 5) + " " + dataset.unit;
				if(showMinimums)           text[column][line++] = ChartUtils.formattedNumber(samples.min, 5) + " " + dataset.unit;
				if(showMaximums)           text[column][line++] = ChartUtils.formattedNumber(samples.max, 5) + " " + dataset.unit;
				if(showMeans)              text[column][line++] = ChartUtils.formattedNumber(stats.getMean(), 5) + " " + dataset.unit;
				if(showMedians)            text[column][line++] = ChartUtils.formattedNumber(stats.getPercentile(50), 5) + " " + dataset.unit;
				if(showStandardDeviations) text[column][line++] = ChartUtils.formattedNumber(stats.getStandardDeviation(), 5) + " " + dataset.unit;
				if(showPercentile)         text[column][line++] = ChartUtils.formattedNumber(stats.getPercentile(90), 5) + " " + dataset.unit;
			}
		
		// determine the width of each piece of text, and track the max for each column
		float[] columnWidth = new float[datasetsCount + 1];
		float[][] textWidth = new float[datasetsCount + 1][lineCount];
		
		line = 0;
		columnWidth[0] = 0;
		textWidth[0][line++] = 0; // no label for the dataset name
		if(showCurrentValues)
			textWidth[0][line++] = 0; // no label for the current value
		if(showMinimums) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		if(showMaximums) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		if(showMeans) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		if(showMedians) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		if(showStandardDeviations) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		if(showPercentile) {
			textWidth[0][line] = OpenGL.smallTextWidth(gl, text[0][line]);
			if(columnWidth[0] < textWidth[0][line])
				columnWidth[0] = textWidth[0][line];
			line++;
		}
		
		if(sampleCount > 0)
			for(int datasetN = 0; datasetN < datasetsCount; datasetN++) {
				int column = datasetN + 1;
				textWidth[column][0] = OpenGL.smallTextWidth(gl, text[column][0]);
				columnWidth[column] = textWidth[column][0];
				if(showCurrentValues) {
					textWidth[column][1] = OpenGL.mediumTextWidth(gl, text[column][1]);
					if(columnWidth[column] < textWidth[column][1])
						columnWidth[column] = textWidth[column][1];
				}
				for(line = showCurrentValues ? 2 : 1; line < lineCount; line++) {
					textWidth[column][line] = OpenGL.smallTextWidth(gl, text[column][line]);
					if(columnWidth[column] < textWidth[column][line])
						columnWidth[column] = textWidth[column][line];
				}
			}
		
		// determine the gaps to leave above and to the left of the text
		boolean showingLabels = columnWidth[0] > 0;
		int occupiedHeight = 0;
		                           occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight; // dataset name
		if(showCurrentValues)      occupiedHeight += Theme.tilePadding + OpenGL.mediumTextHeight;
		if(showMinimums)           occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showMaximums)           occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showMeans)              occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showMedians)            occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showStandardDeviations) occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showPercentile)         occupiedHeight += Theme.tilePadding + OpenGL.smallTextHeight;
		if(showDuration)           occupiedHeight += 2 * (Theme.tilePadding + OpenGL.smallTextHeight);
		occupiedHeight += Theme.tilePadding;
		int occupiedWidth = (int) columnWidth[0];
		for(int i = 1; i < columnWidth.length; i++) {
			if(occupiedWidth > 0)
				occupiedWidth += (int) (2*Theme.tilePadding);
			occupiedWidth += columnWidth[i];
		}
		occupiedWidth += 2*Theme.tilePadding;
		int xOffset = (width - occupiedWidth) / 2;
		if(xOffset < 0)
			xOffset = 0;
		int yOffset = (height - occupiedHeight) / 2;
		if(yOffset < 0)
			yOffset = 0;
		
		// draw the labels
		int x = (int) Theme.tilePadding + xOffset;
		int y = height - yOffset;
		y -= (int) Theme.tilePadding + OpenGL.smallTextHeight; // no label for dataset name
		line = 1;
		if(showCurrentValues) {
			y -= (int) (Theme.tilePadding + OpenGL.mediumTextHeight); // no label for current value
			line++;
		}
		if(showMinimums) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		if(showMaximums) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		if(showMeans) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		if(showMedians) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		if(showStandardDeviations) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		if(showPercentile) {
			y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
			OpenGL.drawSmallText(gl, text[0][line++], x, y, 0);
		}
		x += columnWidth[0];
		
		// draw the dataset names and numbers
		if(sampleCount > 0)
			for(int datasetN = 0; datasetN < datasetsCount; datasetN++) {
				
				int column = datasetN + 1;
				
				x += (int) (2*Theme.tilePadding);
				if(!showingLabels && datasetN == 0)
					x = (int) Theme.tilePadding + xOffset;
				
				y = height - yOffset;
				line = 0;
				
				y -= (int) Theme.tilePadding + OpenGL.smallTextHeight;
				OpenGL.drawSmallText(gl, text[column][line++], (int) x, y, 0);
				
				if(showCurrentValues) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.mediumTextHeight);
					OpenGL.drawMediumText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showMinimums) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showMaximums) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showMeans) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showMedians) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showStandardDeviations) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				if(showPercentile) {
					int xRightJustified = (int) (x + columnWidth[column] - textWidth[column][line]);
					y -= (int) (Theme.tilePadding + OpenGL.smallTextHeight);
					OpenGL.drawSmallText(gl, text[column][line++], xRightJustified, y, 0);
				}
				
				x += columnWidth[column];
				
			}
		
		// draw the duration if enabled and enough space
		if(showDuration) {
			y -= 2 * (Theme.tilePadding + OpenGL.smallTextHeight);
			float durationLabelWidth = OpenGL.smallTextWidth(gl, durationLabel);
			if(y > 0 && durationLabelWidth < width - 2*Theme.tilePadding) {
				x = (int) ((width / 2) - (durationLabelWidth / 2));
				if(x < Theme.tilePadding)
					x = (int) Theme.tilePadding;
				OpenGL.drawSmallText(gl, durationLabel, x, y, 0);
			}
		}
		
		return handler;
		
	}

}
