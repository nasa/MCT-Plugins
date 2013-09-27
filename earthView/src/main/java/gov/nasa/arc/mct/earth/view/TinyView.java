package gov.nasa.arc.mct.earth.view;

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

public class TinyView extends View {
	private static final long serialVersionUID = -1918540228199715462L;
	public static final ViewInfo VIEW_INFO = new ViewInfo(TinyView.class, "Reference", 
			ViewType.TITLE);
	
	private JLabel nameLabel;
	private boolean mousePressed = false;
	
	private BufferedImage image = null;

	
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
        
        //ac.addViewManifestation(this);
        addMouseListener( new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent evt) {
				if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
					getManifestedComponent().open();
				} // TODO: Popup?
				
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {			
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

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
			public void mouseMoved(MouseEvent arg0) {
			}
        	
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
	
	private final Icon ICON = new Icon() {


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
		
	};
}
