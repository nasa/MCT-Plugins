package gov.nasa.arc.mct.chronology.timeline.view;

import gov.nasa.arc.mct.chronology.Chronology;
import gov.nasa.arc.mct.chronology.ChronologyDomain;
import gov.nasa.arc.mct.chronology.event.ChronologicalEvent;
import gov.nasa.arc.mct.chronology.event.ChronologicalInstant;
import gov.nasa.arc.mct.chronology.event.ChronologicalInterval;
import gov.nasa.arc.mct.chronology.event.UNIXTimeInstant;
import gov.nasa.arc.mct.chronology.reference.view.ReferenceView;
import gov.nasa.arc.mct.chronology.timeline.component.TimelineComponent;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.platform.spi.PlatformAccess;
import gov.nasa.arc.mct.roles.events.AddChildEvent;
import gov.nasa.arc.mct.roles.events.PropertyChangeEvent;
import gov.nasa.arc.mct.roles.events.ReloadEvent;
import gov.nasa.arc.mct.roles.events.RemoveChildEvent;
import gov.nasa.arc.mct.services.activity.TimeService;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;

/**
 * A Timeline View presents chronological data, made available by components in the 
 * MCT hierarchy, in a time-aligned horizontal fashion.
 * @author vwoeltje
 *
 */
public class TimelineView extends View {
	private static final long serialVersionUID = 5371168814927341472L;

	public static final String VIEW_ROLE_NAME = "Timeline";
	private static final int DAY = 24*60*60*1000;
	private static final int MIN = 60*1000;

	private ChronologyInfo<?>        chronologyInfo;

	private int timelineWidth  = 1200;  
	private int labelWidth     = 0;

	private JPanel labelPanel;
	private JPanel timelinePanel;
	private JComponent timePanel;
    private JComponent mainPanel;
    private TimelineTimeLabel nowLabel;
    
    //TODO: Find a better way of inserting / handling drag-drop / etc
    private List<View> references = new ArrayList<View>();
    private TimelinePlotManager plotManager = null;
    private Map<AbstractComponent, JComponent> timelineAreas = new HashMap<AbstractComponent, JComponent>();
    
    private TimelineSettings settings = new TimelineSettings(this);
    
    final private JPanel upperArea = new JPanel();
    final private JPanel lowerArea = new JPanel();
	final private JScrollPane scrollPane = new JScrollPane();
	final private JCheckBox   eventLineCheckBox = new JCheckBox();
	
	private JComponent currentTimeMarker = new JComponent() {
		private static final long serialVersionUID = 8531222304365948260L;

		public void paint(Graphics g) {
			setOpaque(false);
			super.paint(g);
			if (chronologyInfo.domain == UNIXTimeInstant.DOMAIN) {
				double now = UNIXTimeInstant.DOMAIN.locateBetween(
						getTimeRelativeToNow(0),
						(UNIXTimeInstant) chronologyInfo.interval.getStart(), 
						(UNIXTimeInstant) chronologyInfo.interval.getEnd());
				int x = (int) (now * timelineWidth);
				g.setColor(new Color(120, 240, 200, 80));
				g.fillRect(x-1, 0, 3, getHeight());
				g.drawLine(x,   0, x, getHeight());
			}
			if (chronologyInfo.primary != null && eventLineCheckBox.isSelected()) {
				paintChronologyLines(chronologyInfo, g);
			}
		}
		
		@SuppressWarnings("unchecked")
		private <T extends ChronologicalInstant> void paintChronologyLines(ChronologyInfo<T> info, Graphics g) {
			for (ChronologicalInterval<?> i : chronologyInfo.primary.getEvents()) {
				double when = ((ChronologyDomain<T>)chronologyInfo.domain).locateBetween(
						(T) i.getStart(), 
						(T) chronologyInfo.interval.getStart(), 
						(T) chronologyInfo.interval.getEnd());
				int x = (int) (when * timelineWidth);
				g.setColor(new Color(240, 200, 120, 80));
				g.fillRect(x-1, 0, 3, getHeight());
				g.drawLine(x,   0, x, getHeight());
			}
		}

		@Override
		public synchronized MouseListener[] getMouseListeners() {
			return super.getMouseListeners();
		}		
	};

	private TimelineControlPanel  controlPanel = null;

	private GridBagConstraints gridConstraints;
	
	private TimelineLayout layout;// = new TimelineLayout();
	
	private SpringLayout   springLayout = new SpringLayout();

	private static final int MAX_DEPTH = 2;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimelineView(AbstractComponent ac, ViewInfo vi) {
		super(ac, vi);

		Chronology<?> c = findFirstChronology(ac); 
		if (c == null || c.getEvents().isEmpty()) {
			// Default domain / interval (UNIX time)
			chronologyInfo = new DefaultChronologyInfo();
		} else {
			chronologyInfo = new ChronologyInfo(c);
		}
		
		initializeFromSettings();
		
		setLayout (new GridLayout());
		mainPanel = new JPanel() {
			private static final long serialVersionUID = -3912477546455018951L;

			public boolean isOptimizedDrawingEnabled() {
				return false;
			}
		};
		mainPanel.setLayout(springLayout);

		setupMarkers();
		setupView();
		setupSlider();
		
		mainPanel.add(upperArea);
		mainPanel.add(lowerArea);
		mainPanel.add(scrollPane);

		// Layout upperArea, then scrollPane, then lowerArea vertically
		springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.SOUTH, upperArea );
		springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, 0, SpringLayout.NORTH, lowerArea );
		springLayout.putConstraint(SpringLayout.SOUTH, lowerArea,  0, SpringLayout.SOUTH, mainPanel);
		
		// upperArea, scrollPane, and lowerArea should expand to fill horizontally
		springLayout.putConstraint(SpringLayout.WEST,  upperArea,    0, SpringLayout.WEST,  mainPanel);
		springLayout.putConstraint(SpringLayout.EAST,  upperArea,  -16, SpringLayout.EAST,  mainPanel);
		springLayout.putConstraint(SpringLayout.WEST,  lowerArea,    0, SpringLayout.WEST,  mainPanel);
		springLayout.putConstraint(SpringLayout.EAST,  lowerArea,  -16, SpringLayout.EAST,  mainPanel);
		springLayout.putConstraint(SpringLayout.WEST,  scrollPane,   0, SpringLayout.WEST,  mainPanel);
		springLayout.putConstraint(SpringLayout.EAST,  scrollPane,   0, SpringLayout.EAST,  mainPanel);

		// Stretch the vertical time line to fill the scroll pane's area
		springLayout.putConstraint(SpringLayout.WEST,   currentTimeMarker, 0,  SpringLayout.WEST,   scrollPane);
		springLayout.putConstraint(SpringLayout.EAST,   currentTimeMarker, 0,  SpringLayout.EAST,   scrollPane);
		springLayout.putConstraint(SpringLayout.NORTH,  currentTimeMarker, 0,  SpringLayout.NORTH,  scrollPane);
		springLayout.putConstraint(SpringLayout.SOUTH,  currentTimeMarker, 0,  SpringLayout.SOUTH,  scrollPane);
		
		// Set up pan buttons. TODO: Move to some private method?
		JComponent leftButton  = new JLabel(TimelineIcon.LEFT.getIcon());
		JComponent rightButton = new JLabel(TimelineIcon.RIGHT.getIcon());
		leftButton.addMouseListener(new PanButtonListener(-0.5));
		rightButton.addMouseListener(new PanButtonListener( 0.5)); 
		mainPanel.add(leftButton);
		mainPanel.add(rightButton);
		
		// Place the buttons at left and right edges of the lowerArea
		springLayout.putConstraint(SpringLayout.WEST, leftButton,   2, SpringLayout.WEST, lowerArea);
		springLayout.putConstraint(SpringLayout.EAST, rightButton, -2, SpringLayout.EAST, lowerArea);
		springLayout.putConstraint(SpringLayout.VERTICAL_CENTER, leftButton, 0, SpringLayout.VERTICAL_CENTER, lowerArea);
		springLayout.putConstraint(SpringLayout.VERTICAL_CENTER, rightButton, 0, SpringLayout.VERTICAL_CENTER, lowerArea);
		
		mainPanel.add(currentTimeMarker);
		
		// Explicitly set the drawing order, since there are multiple overlays
		mainPanel.setComponentZOrder(scrollPane,        5);
		mainPanel.setComponentZOrder(lowerArea,         4);
		mainPanel.setComponentZOrder(upperArea,         3);
 		mainPanel.setComponentZOrder(currentTimeMarker, 2);
		mainPanel.setComponentZOrder(leftButton,        1);
		mainPanel.setComponentZOrder(rightButton,       0);
		
		if (getManifestedComponent().getWorkUnitDelegate() != null) {
			mainPanel.remove(currentTimeMarker);
		}
		
		mainPanel.setDoubleBuffered(true);
		mainPanel.setBackground(Color.GRAY); //TODO: Don't hardcode colors!		
		add(mainPanel);

		new Timer(100, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (chronologyInfo.domain.equals(UNIXTimeInstant.DOMAIN)) {
					nowLabel.setTime(getTimeRelativeToNow(0));
					if (plotManager != null) plotManager.update();
				} else {
					nowLabel.setText("");
				}
				layout.setWidth(timelineWidth);
				revalidate();
				repaint();
			}
		}).start();
	}

	/**
	 * Check to see if some string (most likely user input) can be converted
	 * to a time (an "instant") within this timeline's domain
	 * @param instant the value which will be converted
	 * @return true if the string can be parsed to a valid instant, otherwise false
	 */
	public boolean validateInstant(String instant) {
		try { 
			//TODO: Switch to chronologyInfo.DOMAIN!
			UNIXTimeInstant.DOMAIN.convertToInstant(instant);
			return true;
		} catch (ParseException parseEx) {
			return false;
		}
	}

	/**
	 * Check to see if an interval can be parsed in the current timeline's domain. This 
	 * interval is expressed as a start and an end time in some string format (usually 
	 * user input); the ability to interpret such strings may vary among domains. A value 
	 * of true indicates that both start and end can be parsed, and that together 
	 * they define a coherent interval in the timeline's domain
	 * @param start a string representation of the start of the interval
	 * @param end a string representation of the end of the interval
	 * @return true if these strings can be parsed to a valid interval
	 */
	public boolean validateInterval(String start, String end) {
		return start != null &&
		       end   != null &&
		       chronologyInfo.makeInterval(start, end) != null;
	}

	@Override
	public JComponent initializeControlManifestation() {
		if (controlPanel == null) controlPanel = new TimelineControlPanel(this);
		return controlPanel;
	}

	/**
	 * Get the current bounds for the chronology - that is, the interval representing 
	 * the lowest and highest extents which this view has loaded.
	 * @return an interval which contains all loaded time data in this time line
	 */
	public ChronologicalInterval<? extends ChronologicalInstant> getChronologyBounds() {
		return chronologyInfo.boundary;
	}

	/**
	 * Attempt to set the interval in which data is loaded for this particular time line. 
	 * This is contingent upon the time domain's ability to parse these end points. 
	 * @param start a string representation of the start of the interval
	 * @param end a string representation of the end of the interval
	 * @return true if the interval was updated
	 */
	public boolean setChronologyBounds (String start, String end) {
		if ( chronologyInfo.setBoundaryAndInterval(start, end) ) {
			chronologyInfo.setInterval(0.0, 1.0);
			settings.setBounds(start, end);
			settings.save();
			return true;
		} else {
			return false;
		}

	}
	
	/**
	 * Shift the chronology by a proportion of the currently displayed subinterval.
	 * @param proportion
	 */
	public void shiftChronologyBounds (double proportion) {
		chronologyInfo.shiftBounds(proportion);
	}
	
	/**
	 * Get the timeline-specific layout manager associated with this View. (Other components 
	 * which choose to use the same layout manager will be layed out along the same time 
	 * scale.)
	 */
	public TimelineLayout getLayout() {
		return layout;
	}
	
	/**
	 * Set up the upper area of the view, which contains tick marks & 
	 * some controls.
	 */
	private void setupMarkers() {
		timePanel     = chronologyInfo.makeMarker(TimelineMarker.Alignment.TOP); 
		upperArea.setLayout(new BoxLayout(upperArea, BoxLayout.Y_AXIS));
		if (upperArea.getComponentCount() > 0) upperArea.removeAll();
		upperArea.add(makeControlBar());
		upperArea.add(timePanel);				
	}

	/**
	 * Set up the main viewing area - the central pane in which the time line itself is
	 * displayed
	 */
	@SuppressWarnings("unchecked")
	public void setupView() {
		layout = new TimelineLayout();
		layout.setWindow(chronologyInfo.getWindowLow(), chronologyInfo.getWindowHigh());

		timelineAreas.clear();
		
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
	    labelPanel    = new JPanel();
	    labelPanel.setLayout(new GridBagLayout());
		timelinePanel = new JPanel();
		timePanel.setBackground(Color.GRAY);
		timePanel.setForeground(new Color(220, 220, 220));
		timelinePanel.setLayout(new GridBagLayout());
		
		// Set up the grid constraints; populateFrom will increase y position
		gridConstraints = new GridBagConstraints();
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 0;
		gridConstraints.weightx = 1.0;
		gridConstraints.fill    = GridBagConstraints.HORIZONTAL;

		// Pull in time lines and plots from the MCT component graph
		references.clear();
		if (chronologyInfo.getDomain().equals(UNIXTimeInstant.DOMAIN)) {
			plotManager = new TimelinePlotManager((TimelineInterval<UNIXTimeInstant>) chronologyInfo);
		} else {
			plotManager = null;
		}
		populateFrom(getManifestedComponent(), 0);
		updateView();
		
		gridConstraints.gridy++;
		gridConstraints.weighty = 1.0;
		timelinePanel.add(Box.createVerticalGlue(), gridConstraints);
		
		labelPanel.validate();

		scrollPane.setViewportView(timelinePanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		labelPanel.setBackground(Color.DARK_GRAY);
		timelinePanel.setBackground(Color.DARK_GRAY);
	
		scrollPane.setDropTarget(new TimelineDropTarget());
	}

	/**
	 * Set up the lower area, including the interval window slider & lower ticks.
	 */
	private void setupSlider() {
		final MultiSlider slider = new MultiSlider((float) chronologyInfo.getWindowLow(), (float) chronologyInfo.getWindowHigh());
		slider.setPreferredSize(new Dimension(timelineWidth, 24));
		slider.setBackground(Color.GRAY);
		slider.setForeground(new Color(168, 220, 250));
		slider.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float low     = slider.getLowProportion();
				float high    = slider.getHighProportion();
				
				chronologyInfo.setSubInterval(low, high);
				layout.setWindow(chronologyInfo.getWindowLow(), chronologyInfo.getWindowHigh());
				updateView();			
				mainPanel.repaint();
			}
		});
		
		slider.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(96,96,128)),
				BorderFactory.createEmptyBorder(4, 4, 4, 4) 
				));
		
		lowerArea.setLayout(new BoxLayout(lowerArea, BoxLayout.Y_AXIS));
		if (lowerArea.getComponentCount() > 0) lowerArea.removeAll();
		JComponent otherTimePanel     = chronologyInfo.makeMarker(TimelineMarker.Alignment.BOTTOM);  
		otherTimePanel.setBackground(timePanel.getBackground());
		otherTimePanel.setForeground(timePanel.getForeground());
		lowerArea.add(otherTimePanel);
		lowerArea.add(slider);
	}

	/**
	 * Rebuild all view elements, due to some fundamental settings change
	 */
	private void rebuildView() {
		chronologyInfo.markers.clear();
		setupMarkers();
		setupView();
		setupSlider();
		revalidate();
		repaint();
	}
	
	/**
	 * Update the view - that is, change time interval for certain 
	 * view elements, but do not rebuild
	 */
	private void updateView() {
		if (plotManager != null) plotManager.update();
		chronologyInfo.updateMarkers();
		revalidate();
		repaint(); // Schedule a repaint
	}
	
	
	
	@Override
	public void updateMonitoredGUI() {
		rebuildView();
	}

	@Override
	public void updateMonitoredGUI(AddChildEvent event) {
		updateMonitoredGUI();
	}

	@Override
	public void updateMonitoredGUI(ReloadEvent event) {
		updateMonitoredGUI();
	}

	@Override
	public void updateMonitoredGUI(RemoveChildEvent event) {
		updateMonitoredGUI();
	}

	@Override
	public void updateMonitoredGUI(PropertyChangeEvent event) {
		updateMonitoredGUI();
	}

	@Override
	public void doLayout() {
		int desiredWidth = getParent().getSize().width - labelWidth - 20;
		if (timelineWidth != desiredWidth) {
			timelineWidth = desiredWidth;
			updateView();
			layout.setWidth(timelineWidth);
		}
		super.doLayout();
	}
	

	/**
	 * Fill the main viewing pane with time line representations of underlying components
	 * @param ac the component we wish to represent
	 * @param depth the number of children deep in the MCT component graph to represent
	 */
	private void populateFrom(AbstractComponent ac, int depth) {
		Chronology<?> chrono = ac.getCapability(Chronology.class);
		
		JComponent timelineArea = null;
		if (chrono != null) {
			if (!chrono.getDomain().equals(chronologyInfo.domain)) {
				chrono = makeAdapter(chrono, ac.getComponentId());
			}
			timelineArea = makeTimelineAreaFor(chrono, chronologyInfo);
		} else if (ac.getCapability(FeedProvider.class) != null &&
				chronologyInfo.domain.equals(UNIXTimeInstant.DOMAIN)) {
			// Plots for feed providers
			for (ViewInfo vi : ac.getViewInfos(ViewType.EMBEDDED)) {
				if (vi.getViewName().toLowerCase().contains("plot")) {
					AbstractComponent clone = ac.clone();
					clone.setDisplayName("*");
					View v = vi.createView(clone);
					@SuppressWarnings("unchecked")
					ChronologicalInterval<UNIXTimeInstant> unixInterval = 
						(ChronologicalInterval<UNIXTimeInstant>) chronologyInfo.interval;
					//TODO : Move this to TimelinePlotManager?
					v.getViewProperties().setProperty("TimeMin", 
							Long.toString(unixInterval.getStart().getTimeMillis()));
					v.getViewProperties().setProperty("TimeMax", 
							Long.toString(unixInterval.getEnd().getTimeMillis()));
					v.getViewProperties().setProperty("PinTimeAxis", "true");
					v.getViewProperties().setProperty("PlotTimeAxisSubsequentSetting", "FIXED");
					v.getViewProperties().setProperty("PlotTimeAxisSetting", "X_AXIS_AS_TIME");
					v.getViewProperties().setProperty("PlotXAxisMaximumLocation", "MAXIMUM_AT_RIGHT");
					v.getViewProperties().setProperty("PlotYAxisMaximumLocation", "MAXIMUM_AT_TOP");
					v.getViewProperties().setProperty("PlotNonTimeAxisSubsequentMinSetting", "AUTO");
					v.getViewProperties().setProperty("PlotNonTimeAxisSubsequentMaxSetting", "AUTO");
					v.getViewProperties().setProperty("TimeSystem", "GMT");
					v.getViewProperties().setProperty("TimeFormat", "DDD/HH:mm:ss");
					v.getViewProperties().setProperty("NonTimeMin", "0.0");
					v.getViewProperties().setProperty("NonTimeMax", "1.0");
					v.getViewProperties().setProperty("TimePadding", "1.0");
					v.getViewProperties().setProperty("NonTimeMinPadding", "1.0");
					v.getViewProperties().setProperty("NonTimeMaxPadding", "1.0");
					v.getViewProperties().setProperty("GroupByOrdinalPosition", "true");
					v.getViewProperties().setProperty("PinTimeAxis", "true");
					v.getViewProperties().setProperty("PlotLineConnectionType", "STEP_X_THEN_Y");
					v.getViewProperties().setProperty("PlotLineDrawLines", "true");
					v.getViewProperties().setProperty("PlotLineDrawMarkers", "false");
					clone.save();
					v = vi.createView(clone);
					plotManager.addPlot(v);
					timelineArea = v;
				}
			}
		} else if (!(ac instanceof TimelineComponent)){
			timelineArea = ac.getViewInfos(ViewType.EMBEDDED).iterator().next().createView(ac);
		}
		
		if (timelineArea != null) {
			timelineAreas.put(ac, timelineArea);
			gridConstraints.gridy++;
			gridConstraints.weightx = 0.0;
			gridConstraints.fill = GridBagConstraints.HORIZONTAL;
			timelinePanel.add(makeLabelAreaFor(ac, depth), gridConstraints);			

			gridConstraints.gridy++;
			gridConstraints.gridx = 0;
			gridConstraints.anchor = GridBagConstraints.NORTH;
			gridConstraints.weighty = 0.0;
			gridConstraints.weightx = 1.0;
			gridConstraints.fill = GridBagConstraints.HORIZONTAL;
			timelinePanel.add(timelineArea, gridConstraints);
		}
		
		if (!ac.isLeaf() && depth < MAX_DEPTH) {
			for (AbstractComponent child : ac.getComponents()) {
				populateFrom(child, depth + 1);
			}
		}
		
		// Potentially add more labeling info to indicate children?
	}
	
	private <T extends ChronologicalInstant> JPanel makeTimelineAreaFor(Chronology<?> chrono, ChronologyInfo<T> info) {
		//TODO: Detect exceptions here (should have already been prepared for?)
		return new TimelineArea<T>(info.castChronology(chrono), info, this);
	}
	
	private JPanel makeLabelAreaFor(final AbstractComponent comp, int depth) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		ReferenceView referenceView = new ReferenceView(comp, ReferenceView.VIEW_INFO);
		referenceView.setForeground(Color.decode("#F5DEB3"));
		references.add(referenceView);
		panel.add(referenceView, BorderLayout.WEST);

		panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		
		Color background = new Color(140 - depth * 40, 140 - depth * 30, 140 - depth * 20);
		
		referenceView.setBackground(background);
		panel.setBackground(referenceView.getBackground());
		
		final JButton collapser = new JButton("-");
		collapser.setContentAreaFilled(false);
		collapser.setBorderPainted(false);
		collapser.setMargin(new Insets(0,0,0,0));
		collapser.setForeground(Color.decode("#F5DEB3"));
		collapser.setFont(collapser.getFont().deriveFont(Font.BOLD));
		collapser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				collapser.setText(collapser.getText().equals("-") ? "+" : "-");
				toggle(comp);
			}
		});
		panel.add(collapser, BorderLayout.EAST);
		
		return panel;
	}
	
	private void toggle(AbstractComponent component) {
		JComponent area = timelineAreas.get(component);
		if (area != null) {
			area.setVisible(!area.isVisible());
			repaint();
		}
	}
	
	private Component makeControlBar() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout(8, 0));
		p.add(new JComboBox(Arrays.asList("GMT").toArray()), BorderLayout.WEST);
		nowLabel = new TimelineTimeLabel();
		
		p.setBackground(Color.GRAY);
		nowLabel.setForeground(new Color(220, 220, 220));
		p.add(nowLabel, BorderLayout.CENTER);
		
		eventLineCheckBox.setBackground(p.getBackground());
		//p.add(eventLineCheckBox, BorderLayout.EAST);
		
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(96,96,128)),
				BorderFactory.createEmptyBorder(4, 4, 4, 4) 
				));
		
		return p;
	}
	
	private Chronology<?> findFirstChronology(AbstractComponent ac) {
		Chronology<?> c = ac.getCapability(Chronology.class);
		if (c != null) return c;
		if (ac.isLeaf()) return null;
		for (AbstractComponent comp : ac.getComponents()) {
			c = comp.getCapability(Chronology.class);
			if (c != null) return c;			
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <S extends ChronologicalInstant> Chronology<?> makeAdapter(Chronology<S> chrono, String key) {
		ChronologyInfo<S> adapterInfo = new ChronologyInfo<S>(chrono);
		
		String start = settings.getStart(key);
		String end   = settings.getEnd  (key);
		if (validateInterval(start, end)) {
		
			return new TimelineChronologyAdapter(chronologyInfo.domain,
                    chrono,
                    chronologyInfo.makeInterval(start, end),
                    adapterInfo.boundary);
		} else { 
			return new TimelineChronologyAdapter(chronologyInfo.domain,
				                             chrono,
				                             chronologyInfo.boundary,
				                             adapterInfo.boundary);
		}
	}
	
	private void initializeFromSettings() {
		String start = settings.getStart();
		String end   = settings.getEnd();
		if (start != null && end != null) {
			chronologyInfo.setBoundaryAndInterval(start, end);
		}
	}
	
	private UNIXTimeInstant getTimeRelativeToNow(long offset) {
		TimeService timeService = PlatformAccess.getPlatform().getTimeService();
		return new UNIXTimeInstant( offset + 
				((timeService != null) ? timeService.getCurrentTime() : System.currentTimeMillis()));
	}
	

	
	private class TimelineDropTarget extends DropTarget {
		private static final long serialVersionUID = 1975367765298995708L;
		
		private void addComponents(AbstractComponent before, List<AbstractComponent> childComponents) {
			// Find the index of the "before" component, if necessary
			int index = -1;
			if (before != null) {
				int i = 0;
				for (AbstractComponent child : getManifestedComponent().getComponents()) {
					if (child.getComponentId().equals(before.getComponentId())) {
						index = i; 
					}
					i++;
				}
				if (index == -1) before = null; // Not a direct child
			}
			
			if (before != null && index < getManifestedComponent().getComponents().size()) {
				getManifestedComponent().addDelegateComponents(index + 1, childComponents);
			} else {
				getManifestedComponent().addDelegateComponents(childComponents);
			}
			getManifestedComponent().save();
		}
		
		private void showAdapterDialog(final AbstractComponent before, final List<AbstractComponent> needAdapters) {
			Window w  = SwingUtilities.getWindowAncestor(TimelineView.this);
			JDialog d = new TimelineAdapterDialog(w, needAdapters.get(0).getDisplayName(), TimelineView.this, 
					new TimelineAdapterDialog.Callback() {
				@Override
				public void run(String start, String end) {
					if (validateInterval(start, end)) {
						for (AbstractComponent child : needAdapters) {
							settings.addAdapter(child.getComponentId(), start, end);
							addComponents(before, needAdapters);
							setupView();
							doLayout();
						}
					}
				}
			});
			d.setSize(600, 200);
			d.setLocationRelativeTo(w);
			d.setVisible(true);
		}
		
		public void drop (DropTargetDropEvent event) {
			if (event.getTransferable().isDataFlavorSupported(View.DATA_FLAVOR)) {
				boolean move = false;
				AbstractComponent before = null;
				try {
					final List<AbstractComponent> childComponents = new ArrayList<AbstractComponent>();
					View[] views = (View[]) event.getTransferable().getTransferData(View.DATA_FLAVOR);
					for (View v : views) childComponents.add(v.getManifestedComponent());
					
					// Are we moving an existing component around?
					if (childComponents.size() == 1) {
						for (AbstractComponent child : getManifestedComponent().getComponents()) {
							if (child.getComponentId().equals(childComponents.get(0).getComponentId())) {
								move = true;
							}
						}
					}
					
					// Figure out which component the drop comes before
					Point location = event.getLocation();
					SwingUtilities.convertPointToScreen(location, ((TimelineDropTarget) event.getSource()).getComponent());
					for (View ref : references) {
						try {
							Point p = ref.getLocationOnScreen();
							if (p.y < location.y) {
								if (getManifestedComponent().getComponents().contains(ref.getManifestedComponent()))
									before = ref.getManifestedComponent();
							}
						} catch (Exception e) {
							// not on screen - don't worry about it
						}
					}

					if (move) {
						getManifestedComponent().removeDelegateComponents(childComponents);
					} else {
						// Adding new components, so check Chronologies
						final List<AbstractComponent> needAdapters = new ArrayList<AbstractComponent> ();
						for (AbstractComponent child : childComponents) {
							Chronology<?> chrono = child.getCapability(Chronology.class);
							if (chrono != null && chrono.getDomain() != chronologyInfo.domain) {
								needAdapters.add(child);
							}
						}
						
						if (needAdapters.size() > 0) {
							childComponents.removeAll(needAdapters);
							showAdapterDialog(before, needAdapters);
						}
					}
					addComponents(before, childComponents);
					setupView();
					doLayout();
				} catch (Exception ioe) {
					event.rejectDrop();
					return; //TODO: Log?
				}
			} else {
				event.rejectDrop();
				return;
			}
		}
	}
	
	private class ChronologyInfo<T extends ChronologicalInstant> implements TimelineInterval<T> {
		Chronology<T>            primary = null;
		ChronologyDomain<T>      domain;
		ChronologicalInterval<T> interval;
		ChronologicalInterval<T> boundary;
		
		List<TimelineMarker<T>> markers = new ArrayList<TimelineMarker<T>>();
		
		public ChronologyInfo () {
			
		}

		@SuppressWarnings("unchecked")
		public ChronologyInfo (Chronology<T> chrono) {
			primary = chrono;
			domain = chrono.getDomain();
			List<ChronologicalEvent<T>> events = chrono.getEvents();
			//TODO: What if there are no events? (hopefully caller has checked)
			ChronologicalEvent<T> firstEvent = events.get(0);
			ChronologicalEvent<T> lastEvent  = events.get(events.size() - 1);
			ChronologicalInterval<T> extents = new ChronologicalInterval<T>(firstEvent.getStart(), lastEvent.getEnd());
			if (domain.equals(UNIXTimeInstant.DOMAIN)) {
				UNIXTimeInstant now = getTimeRelativeToNow(0);
				// Check to see if we have a coherent version of "now" - Time Service may be unavailable
				if (domain.getComparator().compare((T) now, extents.getEnd()) > 0) {
					extents = new ChronologicalInterval<T>(extents.getStart(), (T) now);
				}
			}
			interval = extents; //domain.getSubInterval(extents, 0.25, 0.75);
			boundary = extents;// domain.getSubInterval(extents, -0.5, 1.5);
		}
		
		@SuppressWarnings("unchecked")
		public Chronology<T> castChronology(Chronology<?> chrono) {
			return (Chronology<T>) chrono;
		}
		
		public JComponent makeMarker(TimelineMarker.Alignment a) {
			TimelineMarker<T> marker = new TimelineMarker<T>(this, timelineWidth, a);
			markers.add(marker);
			return marker;
		}
		
		public void updateMarkers() {
			for (TimelineMarker<T> m : markers) m.changeInterval(this, timelineWidth);
		}
		
		public boolean setInterval(String start, String end) {
			try {
				interval = new ChronologicalInterval<T>(domain.convertToInstant(start), 
						domain.convertToInstant(end));
				rebuildView();
				return true;
			} catch (ParseException parseEx) {
				return false;
			}
		}
		
		public void setInterval(double start, double end) {
			interval = domain.getSubInterval(boundary, start, end);
		}
		
		public void shiftBounds(double proportion) {
			double i1 = domain.locateBetween(interval.getStart(),   boundary.getStart(), boundary.getEnd()); 
		    double i2 = domain.locateBetween(interval.getEnd(),     boundary.getStart(), boundary.getEnd());
			double b1 = domain.locateBetween(boundary.getStart(),   boundary.getStart(), boundary.getEnd()); //0.0 
		    double b2 = domain.locateBetween(boundary.getEnd(),     boundary.getStart(), boundary.getEnd()); //1.0
			double shift = (i2-i1) * proportion;
			interval = domain.getSubInterval(boundary, i1 + shift, i2 + shift);
			boundary = domain.getSubInterval(boundary, b1 + shift, b2 + shift); // shift, 1.0 + shift?
			rebuildView();
		}
		
		public boolean setBoundaryAndInterval(String start, String end) {
			try {
				setBoundaryAndInterval(domain.convertToInstant(start), domain.convertToInstant(end));
				return true;
			} catch (ParseException parseEx) {
				return false;
			}
		}
		
		public void setBoundaryAndInterval(T start, T end) {
			interval = boundary = new ChronologicalInterval<T>(start, end);
			rebuildView();			
		}
		
		public void setSubInterval(float low, float high) {
			interval = domain.getSubInterval(boundary, low, high);
		}
		
		public double getWindowLow() {
			return domain.locateBetween(interval.getStart(), boundary.getStart(), boundary.getEnd());
		}
		public double getWindowHigh() {
			return domain.locateBetween(interval.getEnd(),   boundary.getStart(), boundary.getEnd());
		}

		@Override
		public ChronologyDomain<T> getDomain() {
			return domain;
		}

		@Override
		public ChronologicalInterval<T> getInterval() {
			return interval;
		}

		@Override
		public ChronologicalInterval<T> getBoundary() {
			return boundary;
		}
		
		public ChronologicalInterval<T> makeInterval(String start, String end) {
			try {
				T s = domain.convertToInstant(start);
				T e = domain.convertToInstant(end);
				if (domain.getComparator().compare(s, e) < 0) {
					return new ChronologicalInterval<T>(s, e);
				} else {
					return null;
				}
			} catch (ParseException parseEx) {
				return null;
			}
		}
	}
	
	private class DefaultChronologyInfo extends ChronologyInfo<UNIXTimeInstant> {
		public DefaultChronologyInfo() { // UNIX time by default
			interval = new ChronologicalInterval<UNIXTimeInstant>( 
					getTimeRelativeToNow(-MIN*40), 
					getTimeRelativeToNow( MIN*30));

			boundary = new ChronologicalInterval<UNIXTimeInstant>( 
					getTimeRelativeToNow( - DAY*1), 
					getTimeRelativeToNow( MIN*30));
			
			domain = UNIXTimeInstant.DOMAIN;
		}
	}
	
	private class PanButtonListener implements MouseListener {
		private double shift;
		
		public PanButtonListener(double shift) {
			this.shift = shift;
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {
			if (arg0.getButton() == MouseEvent.BUTTON1 && arg0.getClickCount() == 1) {
				chronologyInfo.shiftBounds(shift);
				if (controlPanel != null) controlPanel.update();
			}
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
		}
		
	}
}
