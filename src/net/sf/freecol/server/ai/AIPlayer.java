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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;


/**
 * Objects of this class contains AI-information for a single {@link
 * Player} and is used for controlling this player.
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class AIPlayer extends AIObject {

    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());

    public static final String TAG = "aiPlayer";

    /** A comparator to sort AI units by location. */
    private static final Comparator<AIUnit> aiUnitLocationComparator
        = Comparator.comparing(AIUnit::getUnit, Unit.locComparator);

    /** The FreeColGameObject this AIObject contains AI-information for. */
    private ServerPlayer player;

    /** The PRNG to use for this AI player. */
    private Random aiRandom;

    /** The wrapper for the server. */
    private AIServerAPI serverAPI;

    
    /**
     * Creates a new AI player.
     *
     * @param aiMain The {@code AIMain} the player exists within.
     * @param player The {@code ServerPlayer} to associate this
     *            AI player with.
     */
    public AIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player.getId());

        this.player = player;
        this.aiRandom = new Random(aiMain.getRandomSeed("Seed for " + getId()));
        this.serverAPI = new AIServerAPI(this);

        uninitialized = false;
    }

    /**
     * Creates a new {@code AIPlayer} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIPlayer(AIMain aiMain,
                    FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
        
        this.serverAPI = new AIServerAPI(this);
        uninitialized = player == null;
    }


    /**
     * Gets the {@code Player} this {@code AIPlayer} is
     * controlling.
     *
     * @return The {@code Player}.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the PRNG to use for this player.
     *
     * @return A {@code Random} to use for this player.
     */
    public Random getAIRandom() {
        return aiRandom;
    }

    /**
     * Gets the advantage of this AI player from the nation type.
     *
     * @return A short string stating the national advantage.
     */
    protected String getAIAdvantage() {
        final String prefix = "model.nationType.";
        String id = (player == null || player.getNationType() == null) ? ""
            : player.getNationType().getId();
        return (id.startsWith(prefix)) ? id.substring(prefix.length())
            : "";
    }

    /**
     * Gets the connection to the server.
     *
     * @return The connection that can be used when communication with the
     *     server.
     */
    public Connection getConnection() {
        return ((DummyConnection)this.player.getConnection())
            .getOtherConnection();
    }

    /**
     * Meaningfully named access to the server API.
     *
     * @return The {@code AIServerAPI} wrapper.
     */
    public AIServerAPI askServer() {
        return this.serverAPI;
    }

    /**
     * Remove one of our owned objects.
     *
     * Subclasses to override.
     *
     * @param ao The {@code AIObject} to remove.
     */
    public void removeAIObject(AIObject ao) {}

    /**
     * Gets the AI colony corresponding to a given colony, if any.
     *
     * @param colony The {@code Colony} to look up.
     * @return The corresponding AI colony or null if not found.
     */
    public AIColony getAIColony(Colony colony) {
        return getAIMain().getAIColony(colony);
    }

    /**
     * Gets a list of the players AI colonies.
     *
     * @return A list of AI colonies.
     */
    public List<AIColony> getAIColonies() {
        final AIMain aiMain = getAIMain();
        return transform(getPlayer().getColonies(), alwaysTrue(),
                         c -> aiMain.getAIColony(c), toListNoNulls());
    }

    /**
     * Gets the AI unit corresponding to a given unit, if any.
     *
     * @param unit The {@code Unit} to look up.
     * @return The corresponding AI unit or null if not found.
     */
    protected AIUnit getAIUnit(Unit unit) {
        return getAIMain().getAIUnit(unit);
    }

    /**
     * Gets a list of AIUnits for the player.
     *
     * @return A list of AIUnits.
     */
    protected List<AIUnit> getAIUnits() {
        List<AIUnit> aiUnits = new ArrayList<>();
        for (Unit u : getPlayer().getUnitList()) {
            if (u.isDisposed()) {
                logger.warning("getAIUnits ignoring: " + u.getId());
                continue;
            }
            AIUnit a = getAIUnit(u);
            if (a != null) {
                if (a.getUnit() != u) {
                    throw new IllegalStateException("getAIUnits fail: " + u
                                                    + "/" + a);
                }
                aiUnits.add(a);
            } else {
                logger.warning("Could not find the AIUnit for: "
                               + u + " (" + u.getId() + ")");
            }
        }
        return aiUnits;
    }

    /**
     * Standard stance change determination.
     *
     * @param other The {@code Player} wrt consider stance.
     * @return The new {@code Stance}.
     */
    protected Stance determineStance(Player other) {
        return player.getStance(other)
            .getStanceFromTension(player.getTension(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix, LogBuilder lb) {
        int result = super.checkIntegrity(fix, lb);
        if (player == null || player.isDisposed()) {
            lb.add("\n  AIPlayer without underlying player: ", getId());
            result = -1;
        } else if (!player.isAI()) {
            lb.add("\n  AIPlayer that is not an AI: ", getId());
            result = -1;
        }
        return result;
    }

    /**
     * Do some work in a subthread.
     *
     * @param runnable The {@code Runnable} work to do.
     */
    public void invoke(final Runnable runnable) {
        final String name = FreeCol.SERVER_THREAD + "AIPlayer("
            + getPlayer().getName() + ")";
        Thread thread = new Thread(name) {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Thread " + name + " fail", e);
                    }
                }
            };
        logger.finest("Starting " + name + " " + thread.toString());
        thread.start();
    }

    // Message.aiHandler support

    /**
     * Choose a founding father.
     *
     * @param fathers A list of {@code FoundingFather}s to choose from.
     */
    public void chooseFoundingFatherHandler(List<FoundingFather> fathers) {
        FoundingFather ff = selectFoundingFather(fathers);
        if (ff == null) return;
        invoke(() -> {
                AIMessage.askChooseFoundingFather(this, fathers, ff);
            });
    }

    /**
     * Handle a diplomacy message.
     *
     * @param our Our {@code FreeColGameObject}.
     * @param other Their {@code FreeColGameObject}.
     * @param agreement The {@code DiplomaticTrade} being negotiated.
     */
    public void diplomacyHandler(FreeColGameObject our,
                                 FreeColGameObject other,
                                 DiplomaticTrade agreement) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("AI Diplomacy: ").append(agreement);
        switch (agreement.getStatus()) {
        case PROPOSE_TRADE:
            agreement.setStatus(this.acceptDiplomaticTrade(agreement));
            sb.append(" -> ").append(agreement);
            logger.fine(sb.toString());
            break;
        default: // Do not need to respond to others
            sb.append(" -> ignoring ").append(agreement.getStatus());
            logger.fine(sb.toString());
            return;
        }

        invoke(() -> {
                // Note: transposing {our,other} here, the message is
                // in sender sense.
                AIMessage.askDiplomacy(this, our, other, agreement);
            });
    }

    /**
     * Handle a first contact.
     *
     * @param contactor The contacting {@code Player}.
     * @param contactee The contacted {@code Player}.
     * @param tile The {@code Tile} where contact occurs.
     */
    public void firstContactHandler(Player contactor, Player contactee,
                                    Tile tile) {
        invoke(() -> {
                AIMessage.askFirstContact(this, contactor, contactee, tile, true);
            });
    }

    /**
     * Handle a fountain of youth.
     *
     * @param n The number of emigrating units.
     */
    public void fountainOfYouthHandler(int n) {
        invoke(() -> {
                for (int i = 0; i < n; i++) AIMessage.askEmigrate(this, 0);
            });
    }

    /**
     * Handle a native demand.
     *
     * @param unit The {@code Unit} making demands.
     * @param colony The {@code Colony} where demands are being made.
     * @param type The {@code GoodsType} demanded.
     * @param amount The amount of gold demanded.
     * @param initial The acceptance state of the demand.
     */
    public void indianDemandHandler(Unit unit, Colony colony, GoodsType type,
                                    int amount, Boolean initial) {
        Boolean result = indianDemand(unit, colony, type, amount, initial);
        logger.finest("AI handling native demand by " + unit
            + " at " + colony + " result: " + initial + " -> " + result);
        if (result != null) {
            invoke(() -> {
                    AIMessage.askIndianDemand(this, unit, colony, type,
                                              amount, result);
                });
        }
    }

    /**
     * Handle a looting.
     *
     * @param unit The looting {@code Unit}.
     * @param initialGoods A list of {@code Goods} to choose from.
     * @param defenderId The identifier for the defending unit (may be gone!).
     */
    public void lootCargoHandler(Unit unit, List<Goods> initialGoods,
                                 String defenderId) {
        final Market market = getPlayer().getMarket();
        List<Goods> goods = sort(initialGoods,
                                 market.getSalePriceComparator());
        List<Goods> loot = new ArrayList<>();
        int space = unit.getSpaceLeft();
        while (!goods.isEmpty() && space > 0) {
            Goods g = goods.remove(0);
            if (g.getSpaceTaken() > space) continue; // Approximate
            loot.add(g);
            space -= g.getSpaceTaken();
        }

        invoke(() -> {
                AIMessage.askLoot(getAIUnit(unit), defenderId, loot);
            });
    }

    /**
     * Handle the monarch.
     *
     * @param action The {@code MonarchAction} to respond to.
     * @param tax The tax change if any.
     */
    public void monarchActionHandler(MonarchAction action, int tax) {
        boolean accept;
        switch (action) {
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            accept = acceptTax(tax);
            break;

        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            accept = acceptMercenaries();
            break;

        default:
            logger.finest("AI player ignoring monarch action " + action);
            return;
        }
        logger.finest("AI player monarch action " + action + " = " + accept);

        invoke(() -> {
                AIMessage.askMonarchAction(this, action, accept);
            });
    }

    /**
     * Handle an incoming nation summary.
     *
     * @param other The {@code Player} the summary applies to.
     * @param ns The {@code NationSummary} itself.
     */
    public void nationSummaryHandler(Player other, NationSummary ns) {
        getPlayer().putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for AI " + player.getSuffix());
    }

    /**
     * Handle a native trade.
     *
     * @param action The {@code NativeTradeAction} to perform.
     * @param nt The {@code NativeTrade} itself.
     */
    public void nativeTradeHandler(NativeTradeAction action, NativeTrade nt) {
        invoke(() -> {
                NativeTradeAction result = handleTrade(action, nt);
                AIMessage.askNativeTrade(this, result, nt);
            });
    }

    /**
     * Handle a new land name.
     *
     * @param unit The {@code Unit} discovering the new world.
     * @param name The default name.
     */
    public void newLandNameHandler(Unit unit, String name) {
        invoke(() -> {
                AIMessage.askNewLandName(this, unit, name);
            });
    }


    // AI behaviour interface to be implemented by subclasses

    /**
     * Tells this {@code AIPlayer} to make decisions. The
     * {@code AIPlayer} is done doing work this turn when this method
     * returns.
     */
    public abstract void startWorking();

    /**
     * Decide whether to accept an Indian demand, or not.  Or for native
     * players, return the result of the demand.
     *
     * @param unit The {@code Unit} making demands.
     * @param colony The {@code Colony} where demands are being made.
     * @param type The {@code GoodsType} demanded.
     * @param amount The amount of gold demanded.
     * @param accept The acceptance state of the demand.
     * @return True if this player accepts the demand, false if the demand
     *     is rejected, null if no further consideration is required.
     */
    public Boolean indianDemand(Unit unit, Colony colony,
                                GoodsType type, int amount, Boolean accept) {
        return false;
    }

    /**
     * Resolves a diplomatic trade offer.
     *
     * @param agreement The proposed {@code DiplomaticTrade}.
     * @return The {@code TradeStatus} to apply to the agreement.
     *
     */
    public TradeStatus acceptDiplomaticTrade(DiplomaticTrade agreement) {
        return TradeStatus.REJECT_TRADE;
    }

    /**
     * Handle a native trade request.
     *
     * @param action The {@code NativeTradeAction} to perform.
     * @param nt The {@code NativeTrade} to handle.
     * @return The action in response.
     */
    public abstract NativeTradeAction handleTrade(NativeTradeAction action,
                                                  NativeTrade nt);
    
    /**
     * Decides to accept a tax raise or not.
     * Overridden by the European player.
     *
     * @param tax The tax raise.
     * @return True if the raise is accepted.
     */
    public boolean acceptTax(int tax) {
        return false;
    }

    /**
     * Decides to accept an offer of mercenaries or not.
     * Overridden by the European player.
     *
     * @return True if the mercenaries are accepted.
     */
    public boolean acceptMercenaries() {
        return false;
    }

    /**
     * Selects the most useful founding father offered.
     * Overridden by EuropeanAIPlayers.
     *
     * @param ffs The founding fathers on offer.
     * @return The founding father selected.
     */
    public FoundingFather selectFoundingFather(List<FoundingFather> ffs) {
        return null;
    }


    // European players need to implement these for AIColony

    public int getNeededWagons(Tile tile) {
        return 0;
    }

    public int scoutsNeeded() {
        return 0;
    }

    public void completeWish(Wish w) {
    }


    // Serialization

    private static final String RANDOM_STATE_TAG = "randomState";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(RANDOM_STATE_TAG, Utils.getRandomState(aiRandom));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        player = xr.findFreeColGameObject(aiMain.getGame(), ID_ATTRIBUTE_TAG,
            ServerPlayer.class, (ServerPlayer)null, true);

        Random rnd = Utils.restoreRandomState(xr.getAttribute(RANDOM_STATE_TAG,
                                                              (String)null));
        aiRandom = (rnd != null) ? rnd
            : new Random(aiMain.getRandomSeed("Seed for " + getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (getPlayer() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
