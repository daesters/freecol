/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

package net.sf.freecol.client.networking;

import org.w3c.dom.Element;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.networking.ServerAPI;


/**
 * Implementation of the ServerAPI.
 */
public class UserServerAPI extends ServerAPI {

    private final GUI gui;

    public UserServerAPI(GUI gui) {
        super();
        this.gui = gui;
    }

    @Override
    protected void doRaiseErrorMessage(String complaint) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            gui.showErrorMessage(null, complaint);
        }
    }

    @Override
    protected void doClientProcessingFor(Element reply) {
        String sound = reply.getAttribute("sound");
        if (sound != null && !sound.isEmpty()) {
            gui.playSound(sound);
        }
    }
}
