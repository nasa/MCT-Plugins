package gov.nasa.arc.mct.chronology.log.view;

import gov.nasa.arc.mct.chronology.reference.view.ReferenceView;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.FeedView;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class NotebookPanel extends JPanel {
	private static final long serialVersionUID = -1673701127795728442L;

	private JPanel viewPane = new JPanel();
	private AbstractComponent component;
	private long time;
	
	public NotebookPanel(AbstractComponent comp, long time) { //TODO: TEMPORALITY!
		component = comp;
		this.time = time;
		
		setLayout(new BorderLayout());
		 
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		
		ButtonGroup group = createViewSelectionButtons(comp);
		Enumeration<AbstractButton> enumeration = group.getElements();
		while (enumeration.hasMoreElements()) buttons.add(enumeration.nextElement());
		
		JPanel titleBar = new JPanel();
		titleBar.setOpaque(false);
		titleBar.setLayout(new BorderLayout());
		ReferenceView reference = (ReferenceView) ReferenceView.VIEW_INFO.createView(comp);
		reference.setDataVisibility(false);
		reference.setOpaque(false);
		titleBar.add(reference, BorderLayout.WEST);
		titleBar.add(buttons,   BorderLayout.EAST);
		
		setOpaque(false);
		viewPane.setBackground(Color.GRAY);
		viewPane.setMinimumSize(new Dimension(20,20));
		viewPane.setVisible(false);
		
		add(titleBar, BorderLayout.NORTH);
		add(viewPane, BorderLayout.CENTER);
	}
		
	/* Much of this code comes from Canvas's PanelInspector */
    private static final ImageIcon BUTTON_ICON = new ImageIcon(NotebookPanel.class.getResource("/images/infoViewButton-OFF.png"));
    
    private static final ImageIcon X_BUTTON_ICON = new ImageIcon(NotebookPanel.class.getResource("/images/xButton-OFF.png"));
    private static final ImageIcon X_BUTTON_PRESSED_ICON = new ImageIcon(NotebookPanel.class.getResource("/images/xButton-ON.png"));
	
    private ButtonGroup createViewSelectionButtons(AbstractComponent ac) {
        ButtonGroup buttonGroup = new ButtonGroup();
        final Set<ViewInfo> viewInfos = ac.getViewInfos(ViewType.EMBEDDED);
        if (viewInfos.size() == 0) return buttonGroup; // Just an empty button group with no view infos 
        for (ViewInfo vi : viewInfos) {
            ViewChoiceButton button = new ViewChoiceButton(vi);
            buttonGroup.add(button);
            //viewButtonBar.add(button);
        }
        JToggleButton closeButton = new JToggleButton();
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setAction(new AbstractAction() {
			private static final long serialVersionUID = 5333177158472115758L;

			@Override
			public void actionPerformed(ActionEvent e) {
				viewPane.setVisible(false);
				viewPane.removeAll();
            	viewPane.revalidate();
            	viewPane.repaint();
			}
        });
        closeButton.setSelected(true);
        closeButton.setIcon        (X_BUTTON_ICON);
        closeButton.setPressedIcon (X_BUTTON_ICON);
        closeButton.setSelectedIcon(X_BUTTON_PRESSED_ICON);

        buttonGroup.add(closeButton);
        
        return buttonGroup;
    }
    
    private final class ViewChoiceButton extends JToggleButton {
		private static final long serialVersionUID = -1730781999149422873L;
		private static final String SWITCH_TO = "Switch to ";

        public ViewChoiceButton(final ViewInfo viewInfo) {
            setBorder(BorderFactory.createEmptyBorder());
            setAction(new AbstractAction() {
				private static final long serialVersionUID = -8335238318127319495L;

				@Override
                public void actionPerformed(ActionEvent e) {
                	viewPane.removeAll();
                	viewPane.setVisible(true);

                	View view = viewInfo.createView(new ComponentWrapper(component, time));
                	if (view instanceof FeedView) { 
                		viewPane.add(new FeedViewWrapper((FeedView) view, time));
                	} else {
                		viewPane.add(view);
                	}
                	
                	if (view.getPreferredSize() == null ||
                		view.getPreferredSize().height < 12) {
                		view.setPreferredSize(new Dimension (NotebookPanel.this.getWidth()-12, 40));
                	}
                	
                	viewPane.revalidate();
                	viewPane.repaint();
                }
            });
            setIcon(viewInfo.getAsset(Icon.class) == null ? BUTTON_ICON : viewInfo.getAsset(Icon.class));
            setToolTipText(SWITCH_TO + viewInfo.getViewName());
        }        
    }
}
