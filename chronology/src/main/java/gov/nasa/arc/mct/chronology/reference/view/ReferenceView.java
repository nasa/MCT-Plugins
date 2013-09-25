package gov.nasa.arc.mct.chronology.reference.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.gui.FeedView;
import gov.nasa.arc.mct.gui.FeedView.RenderingCallback;
import gov.nasa.arc.mct.gui.NamingContext;
import gov.nasa.arc.mct.gui.SelectionProvider;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.gui.ViewRoleSelection;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;
import gov.nasa.arc.mct.util.MCTIcons;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

public class ReferenceView extends FeedView implements NamingContext, RenderingCallback, SelectionProvider {
	private static final long serialVersionUID = -1918540228199715462L;
	public static final ViewInfo VIEW_INFO = new ViewInfo(ReferenceView.class, "Reference", 
			ViewType.TITLE);
	
	private JLabel nameLabel;
	private JLabel dataLabel;
	private boolean mousePressed = false;
	private Collection<FeedProvider> feedProviders = Collections.emptyList();
	
	private FeedUpdateListener updateListener = null;
	
	public ReferenceView (AbstractComponent ac, ViewInfo vi) {
	    super(ac,vi);
	    setLayout(new BorderLayout());
	    String name = getManifestedComponent().getDisplayName();
	    nameLabel = new JLabel(name.length() < 25 ? name : name.substring(0, 22) + "...");
	    nameLabel.setToolTipText(name);
	    final JLabel icon = new JLabel(MCTIcons.processIcon(ac.getAsset(ImageIcon.class)));
	    dataLabel = new JLabel("") {
			private static final long serialVersionUID = 2324105443468219908L;

			public void paint(Graphics g) {
	    		if (!this.getText().isEmpty()) super.paint(g);
	    	}
	    };
	    
	    dataLabel.setForeground(new Color(64, 180, 64));
	    dataLabel.setFont(dataLabel.getFont().deriveFont(11.0f).deriveFont(Font.BOLD));
	    dataLabel.setBackground(Color.DARK_GRAY);//getBackground().darker().darker());
	    dataLabel.setBorder(BorderFactory.createLineBorder(getBackground().brighter(), 1));
	    dataLabel.setOpaque(true);
	    
	    add(icon, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
        add(dataLabel, BorderLayout.EAST);
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
				if (evt.isPopupTrigger()) {
//					PlatformAccess.getPlatform().getMenuManager().getManifestationPopupMenu(ReferenceView.this)
//						.show(ReferenceView.this, evt.getX(), evt.getY());
				}
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
            	View v[] = { ReferenceView.this };
                return new ViewRoleSelection(v);
            }
            
        	
        });
        
        FeedProvider fp = ac.getCapability(FeedProvider.class);
        if (fp != null) feedProviders = Collections.singleton(fp);
	}
	
    @Override
    public void updateMonitoredGUI() {
        nameLabel.setText(getManifestedComponent().getDisplayName());
        revalidate();
    }

	@Override
	public NamingContext getParentContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextualName() {
		// TODO Auto-generated method stub
		return nameLabel.getText();
	}
	
	public void setForeground (Color fg) {
		if (nameLabel != null) nameLabel.setForeground(fg);
	}
	
	public void setBackground (Color bg) {
		super.setBackground(bg);
		if (dataLabel != null) {
		    //dataLabel.setBackground(getBackground().darker().darker());
		    dataLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getBackground(), 2), BorderFactory.createLineBorder(getBackground().brighter(), 1)));
		}
	}
	
	public void setDataVisibility(boolean visibility) {
		dataLabel.setVisible(visibility);
	}

	@Override
	public void updateFromFeed(Map<String, List<Map<String, String>>> data) {
		for (FeedProvider fp : feedProviders) {
			String subscription = fp.getSubscriptionId();
			if (!data.containsKey(subscription)) continue;
			List<Map<String,String>> feedData = data.get(subscription);
			if (feedData.isEmpty()) continue;
			Map<String, String> feedDataItem = feedData.get(feedData.size() - 1);
			RenderingInfo ri = fp.getRenderingInfo(feedDataItem);
			// TODO: Check status, format value succinctly
			dataLabel.setText(ri.getValueText());
			dataLabel.setForeground(ri.getValueColor());
			if (updateListener != null) updateListener.feedUpdated(this, ri);
		}
		repaint();
	}

	@Override
	public void synchronizeTime(Map<String, List<Map<String, String>>> data,
			long syncTime) {
		updateFromFeed(data);
	}

	@Override
	public Collection<FeedProvider> getVisibleFeedProviders() {
		return feedProviders;
	}

	@Override
	public void render(Map<String, List<Map<String, String>>> data) {
		updateFromFeed(data);
	}
	
	public SelectionProvider getSelectionProvider() {
		return this;
	}
	
	@Override
	public void addSelectionChangeListener(PropertyChangeListener listener) {
	}

	@Override
	public void removeSelectionChangeListener(PropertyChangeListener listener) {
	}

	@Override
	public Collection<View> getSelectedManifestations() {
		return Collections.<View>singleton(this);
	}

	@Override
	public void clearCurrentSelections() {
	}

	public void setFeedUpdateListener(FeedUpdateListener listener) {
		this.updateListener = listener;
	}

	public interface FeedUpdateListener {
		public void feedUpdated(View v, RenderingInfo ri);
	}
	
}
