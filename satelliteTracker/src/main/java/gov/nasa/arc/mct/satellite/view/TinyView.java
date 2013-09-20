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
package gov.nasa.arc.mct.satellite.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.gui.ViewRoleSelection;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;


/*
 * TinyView, used with the MercatorProjectionView, handles the set-up and drawing of satellite
 * icons, displayed satellite names, as well as tool-tips.  Also, TinyView handles how
 * MCT interacts with the satellite icons (e.g: double-clicking a satellite icon opens
 * that satellite into its own MCT window.
 */
@SuppressWarnings("serial")
public class TinyView extends View {
	public static final ViewInfo VIEW_INFO = new ViewInfo(TinyView.class, "Reference", 
			ViewType.TITLE);
	
	private boolean mousePressed = false;
	private JLabel nameLabel;			//name of satellite	
	private BufferedImage image = null; //satellite image

	/*
	 * The International Space Station gets a special icon
	 */
	public TinyView (AbstractComponent ac, ViewInfo vi) {
	    super(ac,vi);
	    
	    	try {
	    		if( ac.getDisplayName().contains("ISS"))
	    			image = ImageIO.read(getClass().getResourceAsStream("images/ISS20x20.png"));
	    		else
	    			image = ImageIO.read(getClass().getResourceAsStream("images/sat20x20.png"));
	    	} catch (IOException ioe) {
	    		//TODO: Log
	    	}

	    //Layout for icon and satellite name	
	   	setLayout(new BorderLayout());
	    String name = getManifestedComponent().getDisplayName();
	    nameLabel = new JLabel(name.length() < 25 ? name : name.substring(0, 22) + "...");
	    nameLabel.setToolTipText(name);
	    final JLabel icon = new JLabel(ICON);
	    setOpaque(false);
	    add(icon, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        
        
        addMouseListener( new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
					getManifestedComponent().open();
				} // TODO: Popup?
				
			}

			@Override public void mouseEntered(MouseEvent arg0) {}
			@Override public void mouseExited(MouseEvent arg0) {}

			@Override
			public void mousePressed(MouseEvent evt) {
				mousePressed = true;
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				if (evt.isPopupTrigger()) {
					// Not quite - the menu manager assumes you will have a selection (and thus a selection manager)
					// for this method to work (otherwise, just ends up referring to the view you're sitting on.)
//					PlatformAccess.getPlatform().getMenuManager().getManifestationPopupMenu(ReferenceView.this)
//						.show(ReferenceView.this, evt.getX(), evt.getY());
				}
				mousePressed = false;
			}
        	
        });
        

        addMouseMotionListener( new MouseMotionListener() {
			public void mouseDragged(MouseEvent evt) {
				if (mousePressed) { 
					icon.getTransferHandler().exportAsDrag(icon, evt, TransferHandler.COPY);
				}
			}

			@Override
			public void mouseMoved(MouseEvent arg0) {}
        	
        });
        
        icon.setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = 4636428854269090018L;

			@Override
            public int getSourceActions(JComponent c) {
            	return COPY;
            }
            
            @Override
            protected Transferable createTransferable(JComponent c) {
            	View v[] = { TinyView.this };
                return new ViewRoleSelection(v);
            }
        });
	}
	
    @Override
    public void updateMonitoredGUI() {
        nameLabel.setText(getManifestedComponent().getDisplayName());
        revalidate();
    }
	
	public void setForeground (Color fg) {
		for (Component c : getComponents()) {
			c.setForeground(fg);
		}
	}
	
	/*
	 * This is a satellite icon.
	 */
	private  final Icon ICON = new Icon() {

		@Override
		public int getIconHeight() {
			return image.getHeight();
		}

		@Override
		public int getIconWidth() {
			return image.getWidth();
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			if (g instanceof Graphics2D) {
				float[] offset = { 0 , 0 , 0 , 0};
				float[] scale  = { (float) c.getForeground().getRed()   / 255.0f, 
						(float) c.getForeground().getGreen() / 255.0f,
						(float) c.getForeground().getBlue()  / 255.0f, 1.0f };
				((Graphics2D) g).drawImage(image, new RescaleOp(scale, offset, null), x, y);	
			} else {
				g.drawImage(image, x, y, c.getForeground(), c);
			}
		}
	};//--end satellite icon
}
