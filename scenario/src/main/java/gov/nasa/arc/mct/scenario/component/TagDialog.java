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
package gov.nasa.arc.mct.scenario.component;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.platform.spi.PlatformAccess;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;



/**
 * Provides a dialog box for creating tags. 
 * Tags are created by default in some appropriate 
 * user repository, defined in the constructor.
 * 
 * Adapted from { @link NewObjectDialog }
 */
@SuppressWarnings("serial")
public class TagDialog extends JDialog {    
    private static final int PADDING = 12;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("Bundle");
    
    private JButton create = new JButton();
    private TagPanel tagPanel = new TagPanel();
    private AbstractComponent component = null;
    
    /**
     * The constructor that creates the dialog.
     */
    public TagDialog(JComponent parent, final AbstractComponent repository) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.DOCUMENT_MODAL);

        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        String suffix = (parentWindow instanceof Frame) ?
        		BUNDLE.getString("wizard_title_infix") + ((Frame) parentWindow).getTitle() : 
        		"";        
         
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle(BUNDLE.getString("wizard_title_tag") + suffix);
        
        JPanel controlPanel = new JPanel();
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	component = PlatformAccess.getPlatform().getComponentRegistry()
            		.newInstance(TagComponent.class, repository);
            	component.setDisplayName(tagPanel.getText());
            	PlatformAccess.getPlatform().getPersistenceProvider()
            		.persist(Arrays.asList(component, repository));
            	dispose();
            }
        });
        create.setText("Create");
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(create);
        controlPanel.add(cancel);
        
        // Allocate most available space to wizard's UI, with create/cancel on the bottom
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createEmptyBorder(PADDING,PADDING,PADDING-1,PADDING-1));
        add(tagPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH); 

        // Set Create button as default to respond to enter key
        this.getRootPane().setDefaultButton(create);

        // Instrument the buttons
        create.setName("createButton");
        cancel.setName("cancelButton");

        // Pack window to fit contents and position
        pack();
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(parent));
        
        // Make sure there is room for the title
        int estimatedTitleWidth = getTitle().length() * 10;
        if (getSize().getWidth() < estimatedTitleWidth) {
            setSize(new Dimension(estimatedTitleWidth, (int) getSize().getHeight()));
        } 
    }
    
    /**
     * Create a new tag component with this dialog.
     * This will block until the dialog is disposed 
     * (typically when the user has chosen create 
     * or cancel.) If cancelled, this will return null.
     * @return the tag component created (null if cancelled)
     */
    public AbstractComponent createComponent() {
    	setVisible(true);
    	return component;
    }
    
    private static class TagPanel extends JPanel {
    	private JLabel tagLabel = new JLabel("Tag: ");
    	private JTextField tagField = new JTextField("untitled tag");
    	
    	public TagPanel() {
    		tagField.setColumns(30);
    		add(tagLabel);
    		add(tagField);
    		setBorder(BorderFactory.createEmptyBorder(0, 0, 17, 0));
    	}
    	
    	public String getText() {
    		return tagField.getText();
    	}
    } 
}