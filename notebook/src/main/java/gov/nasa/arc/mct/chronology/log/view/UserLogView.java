package gov.nasa.arc.mct.chronology.log.view;

import gov.nasa.arc.mct.chronology.Chronology;
import gov.nasa.arc.mct.chronology.event.ChronologicalEvent;
import gov.nasa.arc.mct.chronology.log.component.ComponentRegistryAccess;
import gov.nasa.arc.mct.chronology.log.component.LogEntry;
import gov.nasa.arc.mct.chronology.log.component.UserLogComponent;
import gov.nasa.arc.mct.chronology.reference.view.ReferenceView;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.services.component.ComponentRegistry;
import gov.nasa.arc.mct.services.component.ViewInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

public class UserLogView extends View {
	private static final long serialVersionUID = 1129569325053980669L;
	public static final Border ENTRY_BORDER = new CurvedBorder(12,12,12,12); 
	public static String VIEW_ROLE_NAME = "Notebook";

	public UserLogView(AbstractComponent ac, ViewInfo vi) {
		super(ac, vi);
		setupView();
	}
	
	private void setupView() {
		JScrollPane pane       = new JScrollPane();
		JPanel      logPanel   = new JPanel();
		JPanel      inputPanel = buildInputPanel();
		
		logPanel.setBackground(Color.LIGHT_GRAY);
		logPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridy = 0;
		gbc.insets = new Insets(4,4,4,4);
		
		getManifestedComponent().getComponents(); // Force loading of referenced components
		
		Chronology<?> chron = getManifestedComponent().getCapability(Chronology.class);
		if (chron != null) {
			for (ChronologicalEvent<?> event : chron.getEvents()) {
				Object eventInfo = event.getEventInfo();
				if (eventInfo instanceof LogEntry && 
					((LogEntry<?>) eventInfo).canCast(String.class)) {
					JComponent representation = ((LogEntry<String>) eventInfo).toEvent().getRepresentation(null);
					representation.setBorder(ENTRY_BORDER);
					logPanel.add(representation, gbc);
					gbc.gridy++;
				}
			}
		}
		
		pane.getViewport().add(logPanel);
	
		setLayout(new BorderLayout());
		
		removeAll();
		add(pane,       BorderLayout.CENTER);
		add(inputPanel, BorderLayout.SOUTH);
		revalidate();
	}
	
	private JPanel buildEntryPanel(LogEntry<? extends String> entry) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel("<html>" +
					         //"<i>" + DATE_FORMAT.format(new Date(entry.getEntryTime())) + "</i>: " +
				             entry.getEntry().replaceAll("\n", "\n<br/>") + 
				             "</html>"), BorderLayout.WEST);
		JPanel referencePanel = new JPanel();
		referencePanel.setLayout(new BoxLayout(referencePanel, BoxLayout.PAGE_AXIS));
		ComponentRegistry registry = ComponentRegistryAccess.getComponentRegistry();
		for (String reference : entry.getReferencedIDs()) {
			AbstractComponent comp = registry.getComponent(reference);
			if (comp != null) {
				View     view = new ReferenceView(comp, ReferenceView.VIEW_INFO);
				view.setAlignmentX(LEFT_ALIGNMENT);
				referencePanel.add(view);
			}
		}
		panel.add(referencePanel, BorderLayout.EAST);
		panel.setBorder(ENTRY_BORDER);
		
		return panel;
	}
	
	private JPanel buildInputPanel() {
		JPanel inputPanel = new JPanel();
		
		inputPanel.setLayout(new BorderLayout());
		final JTextPane textArea = new JTextPane();
		final ReferenceArea referenceArea = new ReferenceArea(); 
		final JButton       button = new JButton("Update");
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AbstractComponent comp = getManifestedComponent();
				if (comp instanceof UserLogComponent) {
					List<AbstractComponent> references = referenceArea.getReferences();
					((UserLogComponent) comp).addEntry(textArea.getText(), references);
					comp.addDelegateComponents(references);
					comp.save();
					setupView();
				} else {
					//TODO: log error
				}
			}
		});
		
		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new BorderLayout());
		sidePanel.add(new JLabel("Relevant objects:"), BorderLayout.NORTH);
		sidePanel.add(referenceArea, BorderLayout.CENTER);
		sidePanel.add(button, BorderLayout.SOUTH);
		
		referenceArea.setPreferredSize(new Dimension(120, 160));
		
		inputPanel.add(Box.createVerticalStrut(120), BorderLayout.WEST);
		inputPanel.add(textArea, BorderLayout.CENTER);
		inputPanel.add(sidePanel, BorderLayout.EAST);
		
		textArea.setDropTarget(new TextAreaDropTarget(textArea, referenceArea.getDropTarget()));
		
		textArea.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		sidePanel.setBorder(BorderFactory.createEmptyBorder(0,8,0,0));
		
		JPanel editorPanel = new EditorPanel(textArea);		
		inputPanel.add(editorPanel, BorderLayout.NORTH);
		
		return inputPanel;
	}
	
	private class EditorPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private static final int GAP = 4;
		private final Dimension BUTTON_SIZE = new Dimension(30, 30);
		
		private JTextPane target;
		public EditorPanel(JTextPane targetPane) {
			this.target = targetPane;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(setupButton(new JToggleButton("B"), "BOLD"));
			add(Box.createHorizontalStrut(GAP));
			add(setupButton(new JToggleButton("I"), "ITALIC"));
			setBorder(BorderFactory.createEmptyBorder(GAP,GAP,GAP,GAP));
		}
		
		public JComponent setupButton(JToggleButton b, String name) {
			int style = Font.PLAIN;
			if (b.getText().equalsIgnoreCase("b")) style = Font.BOLD;
			if (b.getText().equalsIgnoreCase("i")) style = Font.ITALIC;
			b.addActionListener(this);
			b.setMargin(new Insets(0,0,0,0));
			b.setPreferredSize(BUTTON_SIZE);
			b.setMinimumSize(BUTTON_SIZE);
			b.setMaximumSize(BUTTON_SIZE);
			b.setName(name);
			b.setFont(b.getFont().deriveFont(16.0f).deriveFont(style));
			return b;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (arg0.getSource() instanceof JToggleButton) {
				JToggleButton c = (JToggleButton) arg0.getSource();
				AttributeSet s = target.getLogicalStyle().copyAttributes();
				// TODO: Move this logic into some organized extensible place
				if (s instanceof MutableAttributeSet) {
					if (c.getName().equalsIgnoreCase("BOLD")) 
						StyleConstants.setBold((MutableAttributeSet) s, c.isSelected());
					if (c.getName().equalsIgnoreCase("ITALIC")) 
						StyleConstants.setItalic((MutableAttributeSet) s, c.isSelected());
				}
				target.setCharacterAttributes(s, false);
			}
		}
		
	}
	
	private class TextAreaDropTarget extends DropTarget {
		private static final long serialVersionUID = 1975367765298995708L;
		private DropTarget delegate;
		private JTextPane  textArea;
		public TextAreaDropTarget(JTextPane textArea, DropTarget delegate) {
			this.delegate = delegate;
			this.textArea = textArea;
		}
		public void drop (DropTargetDropEvent event) {
			if (event.getTransferable().isDataFlavorSupported(View.DATA_FLAVOR)) {
				try {
					View[] views = (View[]) event.getTransferable().getTransferData(View.DATA_FLAVOR);
					if (views.length == 1) {
						textArea.replaceSelection(views[0].getManifestedComponent().getDisplayName());
					} else {
						for (View view : views) {
							textArea.replaceSelection("\n" + view.getManifestedComponent().getDisplayName());
						}
					}
				} catch (Exception ioe) {
					event.rejectDrop();
					return; //TODO: Log?
				}
			} else {
				event.rejectDrop();
				return;
			}
			delegate.drop(event);

		}
	}
	
	private static class CurvedBorder extends EmptyBorder {
		private static final long serialVersionUID = 6190785509909919993L;

		public CurvedBorder(Insets insets) {
			super(insets);
		}
		
		public CurvedBorder(int top, int left, int bottom, int right) {
			super(top, left, bottom, right);
		}

		@Override
		public boolean isBorderOpaque() {
			return false;
		}

		@Override
		public void paintBorder(Component comp, Graphics g, int x,
				int y, int w, int h) {
			Insets insets = getBorderInsets();
			
			if (comp.getParent() != null) {
				g.setColor(comp.getParent().getBackground());
				g.fillRect(x, y, w, y + insets.top);
				g.fillRect(x, y + h - insets.bottom, w, insets.bottom);
			}
			
			g.setColor(comp.getBackground());
			g.fillRect(x, y + insets.top, insets.left, h - insets.bottom - insets.top);
			g.fillRect(x + w - insets.right, y + insets.top, insets.right, h - insets.bottom - insets.top);
			g.fillRect(x + insets.left, y, w - insets.right - insets.left, insets.top);
			g.fillRect(x + insets.left, y + h - insets.bottom, w - insets.right - insets.left, insets.bottom);
			g.fillArc(x, y, insets.left * 2, insets.top * 2, 90, 90);
			g.fillArc(x+w-insets.right*2, y, insets.right * 2, insets.top * 2, 0, 90);
			g.fillArc(x, y+h-insets.bottom*2, insets.left * 2, insets.bottom * 2, 180, 90);
			g.fillArc(x+w-insets.right*2, y+h-insets.bottom*2, insets.right * 2, insets.bottom * 2, 270, 90);
		}
		
		
	}
	
	private class ReferenceArea extends JScrollPane {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4640358863656637380L;
		private List<AbstractComponent> references;
		private JPanel                  referencePanel;
		
		public ReferenceArea() {
			references = new ArrayList<AbstractComponent>();
			referencePanel = new JPanel();
			referencePanel.setLayout(new BoxLayout(referencePanel, BoxLayout.PAGE_AXIS));
			getViewport().add(referencePanel);
			setDropTarget( new DropTarget() {
				private static final long serialVersionUID = 1L;
				public void drop (DropTargetDropEvent event) {
					if (event.getTransferable().isDataFlavorSupported(View.DATA_FLAVOR)) {
						try {
							for (View view : (View[]) event.getTransferable().getTransferData(View.DATA_FLAVOR)) {
								AbstractComponent comp = view.getManifestedComponent();
//								if (comp.getMasterComponent() != null) {
//									comp = comp.getMasterComponent();
//								} //TODO: Work unit delegate?
								addReference(comp);
							}
						} catch (Exception ioe) {
							event.rejectDrop();
							return; //TODO: Log?
						}
					} else {
						event.rejectDrop();
						return;
					}
				}
			} );
		}
		
		private void addReference(AbstractComponent comp) {
			View     view = new ReferenceView(comp, ReferenceView.VIEW_INFO);
			referencePanel.add(view);
			view.setAlignmentX(Component.LEFT_ALIGNMENT);
			references.add(comp);
			
			revalidate();
		}
		
		public List<AbstractComponent> getReferences() {
			return references;
		}
		
		public void clear() {
			references.clear();
			referencePanel.removeAll();
		}
	}

	
}
