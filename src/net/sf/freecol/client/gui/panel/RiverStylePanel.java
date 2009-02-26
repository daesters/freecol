/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A panel for adjusting the river style.
 *
 * <br><br>
 * 
 * This panel is only used when running in
 * {@link FreeColClient#isMapEditor() map editor mode}.
 * 
 */
public final class RiverStylePanel extends FreeColDialog<Integer> implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RiverStylePanel.class.getName());
    
    private static final int CANCEL = -1;
    private static final int DELETE = 0;
    /**
     * The constructor that will add the items to this panel. 
     * @param parent The parent of this panel.
     */
    public RiverStylePanel(Canvas parent) {
        super(parent);
        setLayout(new BorderLayout());
        
        ImageLibrary library = parent.getGUI().getImageLibrary();

        JPanel stylesPanel = new JPanel(new GridLayout(9, 9));
        JButton deleteButton = new JButton(library.getScaledImageIcon(library.getMiscImageIcon(ImageLibrary.DELETE), 0.5f));
        deleteButton.setActionCommand(String.valueOf(DELETE));
        deleteButton.addActionListener(this);
        stylesPanel.add(deleteButton);
        for (int index = 1; index < ImageLibrary.RIVER_STYLES; index++) {
            JButton riverButton = new JButton(library.getScaledImageIcon(library.getRiverImage(index), 0.5f));
            riverButton.setActionCommand(String.valueOf(index));
            riverButton.addActionListener(this);
            stylesPanel.add(riverButton);
        }
        this.add(stylesPanel, BorderLayout.CENTER);
        JButton cancelButton = new JButton(Messages.message("cancel"));
        cancelButton.setActionCommand(String.valueOf(CANCEL));
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic('C');
        this.add(cancelButton, BorderLayout.SOUTH);
        setSize(getPreferredSize());
    }


    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        int style = Integer.parseInt(event.getActionCommand());
        setResponse(new Integer(style));
    }

}
