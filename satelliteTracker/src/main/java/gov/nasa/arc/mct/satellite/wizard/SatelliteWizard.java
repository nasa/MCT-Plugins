/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.satellite.wizard;


import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.satellite.component.CoordinateComponent;
import gov.nasa.arc.mct.satellite.component.CoordinateModel;
import gov.nasa.arc.mct.satellite.component.SatelliteComponent;
import gov.nasa.arc.mct.satellite.component.VectorComponent;
import gov.nasa.arc.mct.satellite.utilities.ComboItem;
import gov.nasa.arc.mct.satellite.utilities.ComboListener;
import gov.nasa.arc.mct.satellite.utilities.ComboRenderer;
import gov.nasa.arc.mct.satellite.utilities.TLEListRenderer;
import gov.nasa.arc.mct.satellite.utilities.TLEUtility;
import gov.nasa.arc.mct.services.component.ComponentRegistry;
import gov.nasa.arc.mct.services.component.CreateWizardUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jsattrak.utilities.TLE;

/*
 * This wizard populates MCT with the satellite-objects chosen by the user
 *   Note: to see the how the classes of this plug-in are associated with one-another,
 *         see the createComp method (which uses the classes made in this plug-in to create
 *         satellite objects)
 * 
 * Note: SatelliteComponentProvider tells MCT to call this wizard when the user right-clicks
 * and selects 'Create->Satellite'
 * 
 * Note: 'createComp' creates the satellite objects and populates the satellite objects into MCT
 * 
 */
public class SatelliteWizard extends CreateWizardUI {
		
	/*
	 * Based from Celestrak.com, this 2D-array represents satellite categories, and all choices within said categories
	 */
	private static final String[][] SatCat = new String[][] {
		{""},//initially have the ComboBox blank
		{"-----Special-Interest-----", 
			 "Last 30 Days' Launches", "Space Stations", "100 (or so) Brightest", "FENGYUN 1C Debris",
			 "IRIDIUM 33 Debris", "COSMOS 2251 Debris", "BREEZE-M R/B Breakup (2012-044C)" }, 
		{"-----Weather & Earth Resources-----",
			 "Weather", "NOAA", "GOES", "Earth Resources", "Search & Rescue (SARSAT)",
			 "Disaster Monitoring", "Tracking and Data Relay Satellite System (TDRSS)" },
		{"-----Communications-----",
			 "Geostationary", "Intelsat", "Gorizont",	"Raduga",	"Molniya", "Iridium",
			 "Orbcomm",	"Globalstar", "Amateur Radio", "Experimental",	"Other" },
		{"-----Navigation-----",
			 "GPS Operational",	"Glonass Operational", "Galileo",	"Beidou", 
			 "Satellite-Based Augmentation System (WAAS/EGNOS/MSAS)",
			 "Navy Navigation Satellite System (NNSS)", "Russian LEO Navigation" },
		{"-----Scientific-----",
			 "Space & Earth Science", "Geodetic",	"Engineering", "Education" },
		{"-----Miscellaneous-----",
		     "Miscellaneous Military",	"Radar Calibration", 	 "CubeSats",	"Other"},		
	};//--end of 2D array of satellite categories
	
	/*
	 * this provides the functionality: don't keep requesting the same TLE file.  We the program
	 * are going to download it once, and then store it in memory
	 *   String: this is the Celestrak satellite-category name (as defined in our 2D array above).
	 *           Note that the Celestrak satellite category name IS UNIQUE
	 */
	private static final Map<String, List<TLE>> storedSatCats= new HashMap<String, List<TLE>>();
	
	private final String DEFAULT_COLLECTION_NAME = "My Satellite Collection";
	private final int TEXT_FIELD_COL_SIZE = 12;	//size of the text box which names a satellite collection
	
	private TLEUtility tleUtil;		//provides access to TLE data on the web
	private Set<String> chosenSats;	//this provides the functionality: don't allow duplicates in the satellite-chosen list
	
	private JComboBox jcbSatCategories;	//drop down box to contain all of the Satellite Categories

	
	private DefaultListModel lmSatChoices;	//model stores what the user can choose
	private DefaultListModel lmSatChosen;	//model stores what the user has chosen
	private JList jlSatChoices;
	private JList jlSatChosen;
	private JScrollPane jscrlpSatChoices;
	private JScrollPane jscrlpSatChosen;
	
	private JButton jbAddSat;
	private JButton jbAddAllSat;
	private JButton jbRemoveSat;
	private JButton jbRemoveAllSat;
	
	private JCheckBox jchkbMakeCollection;
	private JTextField jtfCollectionName;
	
	private JLabel lblChooseSat;
	private JLabel lblChoiceSat;
	private JLabel lblChosenSat;
	private JLabel lblCollectionName;
	
	
	

	/*
	 * SatelliteComponentProvider tells MCT that this method should be called when creating
	 * a satellite (when the user right clicks and selects Create->satellite)
	 * 
	 * This method creates the GUI's look and functionality
	 */
	@Override
	public JComponent getUI(final JButton jbCreate) {
		
		tleUtil = new TLEUtility();
		
		chosenSats = new HashSet<String>();
		lmSatChoices = new DefaultListModel();
		lmSatChosen  = new DefaultListModel();
		
		jbAddSat = new JButton("Add -->");
		jbAddAllSat = new JButton("Add All -->");
		jbRemoveSat = new JButton("Remove <--");
		jbRemoveAllSat = new JButton("Remove All <--");
		
		lblChooseSat = new JLabel("Satellite Group:");
		lblChoiceSat = new JLabel("Choices in Satellite Group");
		lblChosenSat = new JLabel("My Chosen Satellites");
		lblCollectionName = new JLabel("Collection name:");
		
		jchkbMakeCollection = new JCheckBox("Create as a Collection");
		jtfCollectionName = new JTextField("", TEXT_FIELD_COL_SIZE);
		
		
		//--initially buttons and checkbox and text field are disabled
		jbAddSat.setEnabled(false);
		jbAddAllSat.setEnabled(false);
		jbRemoveSat.setEnabled(false);
		jbRemoveAllSat.setEnabled(false);
		jbCreate.setEnabled(false);
		jchkbMakeCollection.setSelected(true);
		jchkbMakeCollection.setEnabled(true);
		jtfCollectionName.setEnabled(true);
		lblCollectionName.setEnabled(true);
		jtfCollectionName.setText(DEFAULT_COLLECTION_NAME);
		jtfCollectionName.selectAll();
			
		
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new FlowLayout());
		
		JPanel motherPanel = new JPanel();
		motherPanel.setLayout(new BoxLayout(motherPanel, BoxLayout.Y_AXIS));
		
		JPanel headPanel = new JPanel();
		headPanel.setLayout(new BoxLayout(headPanel, BoxLayout.LINE_AXIS));
		
		JPanel bodyPanel = new JPanel();
		bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.LINE_AXIS));
		
		JPanel footPanel = new JPanel();
		footPanel.setLayout(new BorderLayout());
		
		
		
		
		JPanel satChoicePanel = new JPanel();
		satChoicePanel.setLayout(new BoxLayout(satChoicePanel, BoxLayout.Y_AXIS));
		
		JPanel SatChosenPanel = new JPanel();
		SatChosenPanel.setLayout(new BoxLayout(SatChosenPanel, BoxLayout.Y_AXIS));
		
		JPanel addBtnsPanel = new JPanel();
		addBtnsPanel.setLayout(new GridBagLayout());
		
		JPanel removeBtnsPanel = new JPanel();
		removeBtnsPanel.setLayout(new GridBagLayout());
		
		JPanel makeCollectionPanel = new JPanel();
		makeCollectionPanel.setLayout(new BoxLayout(makeCollectionPanel, BoxLayout.Y_AXIS));
		
		JPanel nameCollectionPanel = new JPanel();
		nameCollectionPanel.setLayout(new BoxLayout(nameCollectionPanel, BoxLayout.X_AXIS));
		
		
		GridBagConstraints cAdd = new GridBagConstraints();
		cAdd.fill = GridBagConstraints.BOTH;
        cAdd.weightx = 1.0;
        
        GridBagConstraints cRemove = new GridBagConstraints();
		cRemove.fill = GridBagConstraints.BOTH;
        cRemove.weightx = 1.0;
        
        
        
		
        /* ComboBox:
		 * create all of the elements in the combo-box, but Disable the Prime Categories,
		 * like 'Special Interest', 'Weather & Earth Resources', etc
		 */
		List<Object> jcbElements = new ArrayList<Object>();
	
		for( int i=0; i< SatCat.length; i++) {
			jcbElements.add(new ComboItem(SatCat[i][0], false));
			for(int j=1; j< SatCat[i].length; j++) {
				jcbElements.add(new ComboItem(SatCat[i][j], true));
			}//--inner
		}//--outer
		
		/*
		 * ComboBox: Here we populate and appropriately display the Enabled/Disabled
		 * elements within the ComboBox
		 */
		jcbSatCategories = new JComboBox(jcbElements.toArray());
		jcbSatCategories.setBackground(Color.WHITE);
    	jcbSatCategories.setRenderer(new ComboRenderer());
		jcbSatCategories.addActionListener(new ComboListener(jcbSatCategories));
		
		
		/*ActionListener for JComboBox that holds the satellite categories
		 * 
		 *    When the user selects a satellite grouping
		 *      -save all of the TLEs (so we don't have to access the TLE file again
		 *      -populate the SatChoices List
		 *      -Enable the Add/AddAll buttons
		 *      -Set the focus to the SatChoices List
		 *      -Select the first element in the SatChoices List
		 *      
		 *    If the user selects a disabled element, do nothing.
		 */
		jcbSatCategories.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent le) {
				 //if user selected an enabled element of the ComboBox...
				if( ((ComboItem)jcbSatCategories.getSelectedItem()).isEnabled()==true){
					lmSatChoices.clear();
					
					String choice = jcbSatCategories.getSelectedItem().toString();
					List<TLE> userSatChoices;
					if(storedSatCats.containsKey(choice)) {
						userSatChoices = storedSatCats.get(choice);
					}
					else {
						userSatChoices = tleUtil.getTLEs(choice);	//this operation is expensive!
						storedSatCats.put(choice, userSatChoices);
					}
					
					//TODO: mark the TLEs already in the Chosen list as '**already added**'?
					for(int i=0; i < userSatChoices.size(); i++)
						lmSatChoices.addElement(userSatChoices.get(i));
					
					jbAddSat.setEnabled(true);
					jbAddAllSat.setEnabled(true);
					
					jlSatChoices.requestFocusInWindow();
					jlSatChoices.setSelectedIndex(0);
				}				
			}//--end actionPerformed
		});
		
		
		/*ActionListener for Add button:
		 *     add TLEs from list SatChoice to list SatChosen,
		 *     and then remove them from the SatChoice list.  If the SatChoice list is empty,
		 *     we must disable the Add/AddAll buttons
		 */
		jbAddSat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				
				int selected[] = jlSatChoices.getSelectedIndices();
				int len = selected.length;
				
				//happens if user clicks add when focus is away from the list
				if(len==0) {
					return;
				}
				
				jbRemoveSat.setEnabled(true);
				jbRemoveAllSat.setEnabled(true);
				jchkbMakeCollection.setEnabled(true);
				jbCreate.setEnabled(true);
				
				//add the Satellites in the order as they appear in the SatChoice list, 
				for(int i=0; i< len; i++) {
					Object chosen = lmSatChoices.get(selected[i]);
					if( !satAlreadyAdded((TLE)chosen)) {
						addToChosen((TLE)chosen);				
						lmSatChosen.addElement(chosen);
					}
				}
				
				//remove added Satellites from SatChoice (in reverse order to not to cause index shifting)
				for(int i=0; i< len; i++)
					lmSatChoices.remove(selected[len-1-i]);
				
				if(lmSatChoices.isEmpty()) {
					jbAddSat.setEnabled(false);
					jbAddAllSat.setEnabled(false);
				}
				else
					jlSatChoices.setSelectedIndex(0);
			}//--end actionPerformed
			
		});
		
		
		/*Action listener: Add All button
		 *  Take everything from the SatChoices list and 
		 */
		jbAddAllSat.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int size = lmSatChoices.getSize();
				for(int i=0; i< size; i++) {
					if( !satAlreadyAdded((TLE)lmSatChoices.get(0))) {
						addToChosen((TLE)lmSatChoices.get(0));
						lmSatChosen.addElement(lmSatChoices.get(0));
					}
					lmSatChoices.remove(0);
				}
				
				jbAddSat.setEnabled(false);
				jbAddAllSat.setEnabled(false);
				
				jbRemoveSat.setEnabled(true);
				jbRemoveAllSat.setEnabled(true);
				jchkbMakeCollection.setEnabled(true);
				jbCreate.setEnabled(true);
			}
		});
		
		/*Action listener: Remove button
		 * 
		 */
		jbRemoveSat.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int selected[] = jlSatChosen.getSelectedIndices();
				
				if(selected.length==0)
					return;
				
				//remove all of the selected elements (from highest index to lowest; in order to avoid an index-shift)
				//And put them back in the Choice list
				for(int i=0; i<selected.length; i++) {
					removeFromChosen((TLE)lmSatChosen.get(selected[selected.length-1-i]));
					lmSatChosen.remove(selected[selected.length-1-i]);
				}
				
				if(lmSatChosen.isEmpty()) {
					jbRemoveSat.setEnabled(false);
					jbRemoveAllSat.setEnabled(false);
					jchkbMakeCollection.setEnabled(false);
					jbCreate.setEnabled(false);
					jcbSatCategories.requestFocus();  //since no elements in the Chosen list, set focus to comboBox
				}
				else
					jlSatChosen.setSelectedIndex(0);
			}
		});
		
		/*Action listener: Remove All button
		 * 
		 */
		jbRemoveAllSat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				lmSatChosen.clear();
				chosenSats.clear();
				jbRemoveSat.setEnabled(false);
				jbRemoveAllSat.setEnabled(false);
				jchkbMakeCollection.setEnabled(false);
				jbCreate.setEnabled(false);
			}
		});
		
		/*Check Box
		 * 
		 */
		jchkbMakeCollection.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange()==ItemEvent.SELECTED ) {
					lblCollectionName.setEnabled(true);
					jtfCollectionName.setEnabled(true);
					jtfCollectionName.requestFocusInWindow();
					jtfCollectionName.setText(DEFAULT_COLLECTION_NAME);
					jtfCollectionName.selectAll();
				}
				else {
					lblCollectionName.setEnabled(false);
					jtfCollectionName.setText("");
					jtfCollectionName.setEnabled(false);
				}
			}
		});
		
		/*TextField associated with the satellite-collection name:
		 *  If the user wants to create their satellites in a collection, we do not allow
		 *  the user to have an empty name for the collection. 
		 */
		jtfCollectionName.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				if(jtfCollectionName.getText().isEmpty() && jchkbMakeCollection.isSelected()) 
					jbCreate.setEnabled(false);
				else
					jbCreate.setEnabled(true);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				jbCreate.setEnabled(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				if(jtfCollectionName.getText().isEmpty() && jchkbMakeCollection.isSelected()) {
					jbCreate.setEnabled(false);
				}
				else
					jbCreate.setEnabled(true);
			}
		});
		
		
		//--populate JLists from the listModels
		jlSatChoices = new JList(lmSatChoices);
		jlSatChoices.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jlSatChoices.setCellRenderer(new TLEListRenderer());
		jscrlpSatChoices = new JScrollPane(jlSatChoices);
	    
		jlSatChosen = new JList(lmSatChosen);
		jlSatChosen.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jlSatChosen.setCellRenderer(new TLEListRenderer());
		jscrlpSatChosen  = new JScrollPane(jlSatChosen);
		
		
		//-------Lastly set the layout of the GUI
		headPanel.add(lblChooseSat);
		headPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		headPanel.add(jcbSatCategories);
		headPanel.add(Box.createRigidArea(new Dimension(200, 0)));

		
		satChoicePanel.add(lblChoiceSat);
		lblChoiceSat.setAlignmentX(Component.CENTER_ALIGNMENT);
		satChoicePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		satChoicePanel.add(jscrlpSatChoices);
		
		satChoicePanel.setMinimumSize(new Dimension(250, 300));
		satChoicePanel.setPreferredSize(new Dimension(250, 300));
		satChoicePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		
     	addBtnsPanel.add(jbAddSat, cAdd);
		cAdd.gridx++;
		cAdd.insets = new Insets(5, 0, 0, 0);
		addBtnsPanel.add(jbAddAllSat, cAdd);
		
		SatChosenPanel.add(lblChosenSat);
		lblChosenSat.setAlignmentX(Component.CENTER_ALIGNMENT);
		SatChosenPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		SatChosenPanel.add(jscrlpSatChosen);
		
		SatChosenPanel.setMinimumSize(new Dimension(250, 300));
		SatChosenPanel.setPreferredSize(new Dimension(250, 300));
		SatChosenPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		
		removeBtnsPanel.add(jbRemoveSat, cRemove);
		cRemove.gridx++;
		cRemove.insets = new Insets(5, 0, 0, 0);
		removeBtnsPanel.add(jbRemoveAllSat, cRemove);
		
		bodyPanel.add(satChoicePanel);
		bodyPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		bodyPanel.add(addBtnsPanel);
		bodyPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		bodyPanel.add(SatChosenPanel);
		bodyPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		bodyPanel.add(removeBtnsPanel);
	
		/*
		 * Panel containing check box and text field 
		 */
		makeCollectionPanel.add(jchkbMakeCollection);
		jchkbMakeCollection.setAlignmentX(Component.LEFT_ALIGNMENT);
		nameCollectionPanel.add(lblCollectionName);
		nameCollectionPanel.add(jtfCollectionName);
		nameCollectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		makeCollectionPanel.add(nameCollectionPanel);
		
		footPanel.add(makeCollectionPanel, BorderLayout.EAST);
		
		//Here you can turn on the boarders to all of the panels if you want:
		//		headPanel.setBorder(BorderFactory.createTitledBorder("Head Panel"));
		//		bodyPanel.setBorder(BorderFactory.createTitledBorder("Body Panel"));
		//		motherPanel.setBorder(BorderFactory.createTitledBorder("Mother Panel"));
		//		rootPanel.setBorder(BorderFactory.createTitledBorder("Root Panel"));
		
		motherPanel.add(headPanel);
		motherPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		motherPanel.add(bodyPanel);
		motherPanel.add(footPanel);
	
		
		rootPanel.add(motherPanel, Component.CENTER_ALIGNMENT);
		
		
		return rootPanel;
		
	}
	
	
	/*
	 * Determine whether the satellite TLE being added is already contained in the Chosen list.
	 * 
	 * Note: the satellite number (which is actually a NORAD catalog number) in a TLE is a unique key 
	 *    ref: Celestrak:: "The NORAD Catalog Number is a unique identifier assigned by
	 *                      NORAD for each earth-orbiting artificial satellite in their
	 *                      SATCAT (Satellite Catalog)."
     * Reason for satAlreadyAdded: Do not allow multiple copies of satellites
	 * into the 'SatChosen' list.
	 */
	private boolean
	satAlreadyAdded
	( TLE tle ) {
		
		String SatNum = tle.getLine2().split("\\s+")[1]; //grab the Sat Num (2nd token on 2nd line of TLE)
		return chosenSats.contains(SatNum);
	}
	
	/*
	 * Associated with satAlreadyAdded, this is to include the functionality: Do not allow multiple copies of satellites
	 * into the 'SatChosen' list
	 */
	private void
	addToChosen
	( TLE tle ) {
		String SatNum = tle.getLine2().split("\\s+")[1];
		chosenSats.add(SatNum);
	}
	
	/*
	 * Associated with satAlreadyAdded, this is to include the functionality: Do not allow multiple copies of satellites
	 * into the 'SatChosen' list
	 *    The user has removed a sat from the chosen list, so we must clear it from the chosenSat lookup
	 */
	private void
	removeFromChosen
	(TLE tle) {
		String SatNum = tle.getLine2().split("\\s+")[1];
		chosenSats.remove(SatNum);
	}

	
	/*
	 * This method is called when the 'create' button is clicked within the GUI (for creating satellites)
	 * 
	 * Precondition: the list model containing the chosen satellites to create (lmSatChosen) is not empty
	 * 
	 * An example: if the user decided to create only one satellite (the International Space Station), at the
	 * completion of this method, the following is stored in MCT (with their associated classes in parenthesis)
	 *        
	 *          ISS                 (This is a SatelliteComponet)
	 *            Position             (This is a VectorComponent)
	 *                x                     (This is a CoordinateComponent)
	 *                y                     (This is a CoordinateComponent)
	 *                z                     (This is a CoordinateComponent)
	 *            Velocity          (This is a VectorComponent)
	 *                x                  (This is a CoordinateComponent)
	 *                y                  (This is a CoordinateComponent)
	 *                z                  (This is a CoordinateComponent)
	 */
	@Override
	public AbstractComponent createComp(ComponentRegistry registry,
			AbstractComponent parentComp) {

		//all satellites will have this as their parent; root may either be a collection (if selected) or will be
		//whatever component the user is creating from; e.g., they right-clicked 'MySandbox' and chose to create
		//a satellite.
		AbstractComponent rootComponent;
		
		//determine whether or not we need to make a collection.
		if(jchkbMakeCollection.isSelected()) {
			rootComponent = registry.newInstance(registry.newInstance("gov.nasa.arc.mct.components.collection.CollectionComponent").getClass(), parentComp);
			rootComponent.setDisplayName(jtfCollectionName.getText());
			rootComponent.save();
		}
		else
			rootComponent = parentComp;

		
		int createSize = lmSatChosen.getSize();

		//here we are creating new instances of mct objects and adding them the the parent component (which may be a collection or the
		//given parentComp--an example of this is if the user right-clicked 'MySandbox')
		for(int i=0; i<createSize; i++) {
			SatelliteComponent satComponent = registry.newInstance(SatelliteComponent.class, rootComponent);
			TLE currentTLE = (TLE)lmSatChosen.get(i);
			satComponent.setDisplayName(currentTLE.getSatName());
			satComponent.setOrbitalParameters(currentTLE);
			satComponent.save();

			//the following makes the position and velocity components where the SatelliteComponent is their parent
			boolean[] truths   = { false,   true };
			String[]  axisName = { "X", "Y", "Z" };
			for (boolean velocity : truths) {
				String name = velocity ? "Velocity (in m/s)" : "Position (in ECEF)";
				
				VectorComponent vectorComponent = registry.newInstance(VectorComponent.class, satComponent);
				vectorComponent.setDisplayName( name );
				vectorComponent.save();
				
				//make x, y, z components, where position/velocity is their parent
				for (int axis = 0; axis < 3; axis++) {
					CoordinateComponent coordinateComponent = registry.newInstance(CoordinateComponent.class, vectorComponent);
					coordinateComponent.setDisplayName(name + " " + axisName[axis]);
					coordinateComponent.setModel(new CoordinateModel(axis, velocity, satComponent.getComponentId(), currentTLE));
					coordinateComponent.save();
				}
			}
		}

		return rootComponent;
        
	}
	
}