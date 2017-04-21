/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.common.networking;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * A wrapper message.  That is a message with just a reply identifier
 * attribute to be matched, and the underlying real message.
 */
public abstract class WrapperMessage extends AttributeMessage {

    public static final String REPLY_ID_TAG = "networkReplyId";

    /** The encapsulated message. */
    protected Message message = null;


    /**
     * Create a new {@code WrapperMessage} of a given type.
     *
     * @param tag The actual message tag.
     * @param replyId The reply id.
     * @param message The {@code Message} to encapsulate.
     */
    protected WrapperMessage(String tag, int replyId, Message message) {
        super(tag, REPLY_ID_TAG, String.valueOf(replyId));

        this.message = message;
    }

    /**
     * Create a new {@code WrapperMessage} from a stream.
     *
     * @param tag The actual message tag.
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    protected WrapperMessage(String tag, Game game, FreeColXMLReader xr)
        throws XMLStreamException, FreeColException {
        super(tag, REPLY_ID_TAG, xr.getAttribute(REPLY_ID_TAG, (String)null));

        if (xr.moreTags()) {
            String mt = xr.getLocalName();
            this.message = Message.read(game, xr);
            xr.closeTag(mt);
        }
        xr.expectTag(tag);
    }
        

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSourcePlayer(ServerPlayer serverPlayer) {
        super.setSourcePlayer(serverPlayer);
        if (this.message != null) { // Propagate to wrapped message
            this.message.setSourcePlayer(serverPlayer);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void clientHandler(FreeColClient freeColClient)
        throws FreeColException {
        if (this.message != null) {
            this.message.clientHandler(freeColClient);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return (this.message == null) ? null
            : this.message.serverHandler(freeColServer, serverPlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.message != null) {
            this.message.toXML(xw);
        }
    }
    

    // Public interface

    /**
     * Get the reply identifier.
     *
     * @return The reply identifier.
     */
    public int getReplyId() {
        return getIntegerAttribute(REPLY_ID_TAG, -1);
    }

    /**
     * Get the wrapped message.
     *
     * @return The {@code Message}.
     */
    public Message getMessage() {
        return this.message;
    }

    /**
     * Get the type of the wrapped message.
     *
     * @return The subtype, or null if no message.
     */
    public String getSubType() {
        return (this.message == null) ? null : this.message.getType();
    }
}