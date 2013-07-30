package gov.nasa.arc.mct.scenario.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.SelectionProvider;
import gov.nasa.arc.mct.gui.Twistie;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.gui.ViewRoleSelection;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;
import gov.nasa.arc.mct.util.LafColor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

@SuppressWarnings("serial")
public class TimelineInspector extends View {
    
    private static final Color BACKGROUND_COLOR = LafColor.WINDOW_BORDER.darker();
    private static final Color FOREGROUND_COLOR = LafColor.WINDOW.brighter();
    
    private static final String PANEL_SPECIFIC = " (Timeline-Specific)";
    private static final String DASH = " - ";
    
    private final PropertyChangeListener selectionChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            @SuppressWarnings("unchecked")
            Collection<View> selectedViews =  (Collection<View>) evt.getNewValue();
            selectedManifestationChanged(selectedViews.isEmpty() || selectedViews.size() > 1
                            ? null
                            : selectedViews.iterator().next());
        }
    };

    private JLabel viewTitle = new JLabel();
    private JLabel space = new JLabel(" ");
    private JPanel emptyPanel = new JPanel();
    private JComponent content;
    private View view;
    private JComponent viewControls;
    private JPanel titlebar = new JPanel();
    private JPanel viewButtonBar = new JPanel();
    private GridBagConstraints c = new GridBagConstraints();
    private ControllerTwistie controllerTwistie;

    public TimelineInspector(AbstractComponent ac, ViewInfo vi) {    
        super(ac,vi);
        registerSelectionChange();        
        setLayout(new BorderLayout());
                
        titlebar.setLayout(new GridBagLayout());
        JLabel titleLabel = new JLabel("Timeline Inspector:  ");
        
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.gridwidth = 1;
        titlebar.add(titleLabel, c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        titlebar.add(viewTitle, c);
        
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        titlebar.add(space);
        
        titleLabel.setForeground(FOREGROUND_COLOR);
        viewTitle.setForeground(FOREGROUND_COLOR);
        viewTitle.addMouseMotionListener(new WidgetDragger());
        titlebar.setBackground(BACKGROUND_COLOR);
        viewButtonBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        viewButtonBar.setBackground(BACKGROUND_COLOR);
        
        add(titlebar, BorderLayout.NORTH);
        add(emptyPanel, BorderLayout.CENTER);
        
        content = emptyPanel;
        setMinimumSize(new Dimension(0, 0));
    }
    
    private ButtonGroup createViewSelectionButtons(AbstractComponent ac, ViewInfo selectedViewInfo) {
        ButtonGroup buttonGroup = new ButtonGroup();
        final Set<ViewInfo> viewInfos = ac.getViewInfos(ViewType.OBJECT);
        for (ViewInfo vi : viewInfos) {
            ViewChoiceButton button = new ViewChoiceButton(vi);
            buttonGroup.add(button);
            viewButtonBar.add(button);
            
            if (vi.equals(selectedViewInfo))
                buttonGroup.setSelected(button.getModel(), true);
        }
        return buttonGroup;
    }
    
    private void selectedManifestationChanged(View view) {
        remove(content);
        if (view == null) {
            viewTitle.setIcon(null);
            viewTitle.setText("");   
            viewTitle.setTransferHandler(null);
            content = emptyPanel;
        } else {
            viewTitle.setIcon(view.getManifestedComponent().getIcon());
            viewTitle.setText(view.getManifestedComponent().getDisplayName() + DASH + view.getInfo().getViewName() + PANEL_SPECIFIC);
            viewTitle.setTransferHandler(new WidgetTransferHandler());
            Collection<ViewInfo> viewInfos = view.getManifestedComponent().getViewInfos(ViewType.OBJECT);
            for (ViewInfo vi : viewInfos) { // find Info view
            	if (vi.getViewName().contains("Info")) {
            		content = this.view = vi.createView(view.getManifestedComponent());		
            	}
            } // TODO: Maybe do a custom view for fiddling with duration?            
            JComponent viewControls = getViewControls();
            if (viewControls != null) {
                c.weightx = 0;
                controllerTwistie = new ControllerTwistie();
                titlebar.add(controllerTwistie, c);
            }
            createViewSelectionButtons(view.getManifestedComponent(), view.getInfo());

            c.anchor = GridBagConstraints.LINE_END;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 0;       
            titlebar.add(viewButtonBar, c);
        }
        Dimension preferredSize = content.getPreferredSize();
        JScrollPane jp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        preferredSize.height += jp.getHorizontalScrollBar().getPreferredSize().height;
        JScrollPane inspectorScrollPane = new JScrollPane(content,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        content = inspectorScrollPane;
        add(inspectorScrollPane, BorderLayout.CENTER);
        revalidate();
    }

    private void registerSelectionChange() {
        // Register when the panel inspector is added to the window.
        addAncestorListener(new AncestorListener() {
            SelectionProvider selectionProvider;
            @Override
            public void ancestorAdded(AncestorEvent event) {
                selectionProvider = (SelectionProvider) event.getAncestorParent();
                selectionProvider.addSelectionChangeListener(selectionChangeListener);
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                if (selectionProvider != null) {
                    selectionProvider.removeSelectionChangeListener(selectionChangeListener);
                }
            }
            
        });            
    }
    
    private void showOrHideController(boolean toShow) {
        remove(content);
        
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(view);
        if (toShow) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getViewControls(), scrollPane);
            splitPane.setResizeWeight(.66);
            splitPane.setOneTouchExpandable(true);
            splitPane.setContinuousLayout(true);
            splitPane.setBorder(null);
            content = splitPane;
            // Overwrite lock state for the panel-specific view
            if (isLocked)
                view.exitLockedState();
            else
            	view.enterLockedState();
        } else {
            content = scrollPane;
        }
        add(content, BorderLayout.CENTER);
        revalidate();
    }
    
    protected JComponent getViewControls() {
    	assert view != null;
    	if (viewControls == null)
    		viewControls = view.getControlManifestation();
    	return viewControls;
    }
    
    @Override
    public SelectionProvider getSelectionProvider() {
        return super.getSelectionProvider();
    }

    private boolean isLocked = false;
    
    @Override
    public void enterLockedState() {
        isLocked = false;
        super.enterLockedState();
        view.enterLockedState();
    }
    
    @Override
    public void exitLockedState() {
        isLocked = true;
        super.exitLockedState();
        if (view != null) 
            view.exitLockedState();
    }
    
    private static final class WidgetDragger extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            JComponent c = (JComponent) e.getSource();
            TransferHandler th = c.getTransferHandler();
            th.exportAsDrag(c, e, TransferHandler.COPY);
        }
    }
    
    private final class WidgetTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (view != null) {
                return new ViewRoleSelection(new View[] { view});
            } else {
                return null;
            }
        }
    }
    
    private final class ViewChoiceButton extends JToggleButton {
        private static final String SWITCH_TO = "Switch to ";

        public ViewChoiceButton(final ViewInfo viewInfo) {
            setBorder(BorderFactory.createEmptyBorder());
            setAction(new AbstractAction() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    TimelineInspector.this.remove(content);
                    content = view = viewInfo.createView(view.getManifestedComponent());
                    Dimension preferredSize = content.getPreferredSize();
                    JScrollPane jp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                    preferredSize.height += jp.getHorizontalScrollBar().getPreferredSize().height;
                    JScrollPane inspectorScrollPane = new JScrollPane(content,
                                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    TimelineInspector.this.add(inspectorScrollPane, BorderLayout.CENTER);
                    TimelineInspector.this.revalidate();
                    content = inspectorScrollPane;
                    viewTitle.setText(view.getManifestedComponent().getDisplayName() + DASH + view.getInfo().getViewName() + PANEL_SPECIFIC);
                    viewControls = null;
                    if (controllerTwistie != null)
                        controllerTwistie.changeState(false);
                }
            });
//            setIcon(viewInfo.getIcon() == null ? BUTTON_ICON : viewInfo.getIcon());
//            setPressedIcon(viewInfo.getIcon() == null ? BUTTON_ICON : viewInfo.getIcon());
//            setSelectedIcon(viewInfo.getSelectedIcon() == null ?  BUTTON_PRESSED_ICON : viewInfo.getSelectedIcon());
            setToolTipText(SWITCH_TO + viewInfo.getViewName() + PANEL_SPECIFIC);
        }        
    }
    
    private final class ControllerTwistie extends Twistie {
        
        public ControllerTwistie() {
            super();
        }
        
        @Override
        protected void changeStateAction(boolean state) {
            showOrHideController(state);
        }        
    }
}
