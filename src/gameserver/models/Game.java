package gameserver.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import gameserver.effects.EffectPool;
import gameserver.engine.GoalHoop;
import gameserver.engine.StatEngine;
import gameserver.engine.Team;
import gameserver.engine.TeamAffiliation;
import gameserver.entity.Box;
import gameserver.entity.Entity;
import gameserver.entity.Titan;
import gameserver.entity.TitanType;
import gameserver.targeting.ShapePayload;
import networking.ClientPacket;
import networking.PlayerDivider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Game {
    //jsonignored
    public final int HOME_HI_X = 133;
    public final int HOME_HI_Y = 584;
    public final int AWAY_HI_X = 1923;
    public final int AWAY_HI_Y = 584;

    @JsonProperty
    public boolean ballVisible;
    @JsonProperty
    public boolean inGame;
    @JsonProperty
    public boolean goalVisible;
    @JsonProperty
    public int phase;
    @JsonProperty
    public boolean began = false;
    @JsonProperty
    public double xKickPow;
    @JsonProperty
    public double yKickPow;
    protected int curveFactor;
    @JsonProperty
    public String gameId;
    @JsonProperty
    public UUID lastPossessed;
    @JsonProperty
    public boolean ended = false;
    @JsonProperty
    public AtomicBoolean locked = new AtomicBoolean(false);

    @JsonProperty
    public Box ball = new Box(0, 0, 30, 30);
    Titan hGol = new Titan(0, 0, TeamAffiliation.HOME, TitanType.GUARDIAN);
    Titan awGol = new Titan(1200, 400, TeamAffiliation.AWAY, TitanType.GUARDIAN);
    @JsonProperty
    public Titan underControl = null; //Only set by the gameserver right before pushing an update
    @JsonProperty
    public StatEngine stats = new StatEngine();
    @JsonProperty
    public EffectPool effectPool = new EffectPool();
    @JsonProperty
    public Titan[] players = {hGol,
            awGol,
            new Titan(0, 0, TeamAffiliation.HOME, TitanType.WARRIOR),
            new Titan(0, 0, TeamAffiliation.HOME, TitanType.SLASHER),
            new Titan(0, 0, TeamAffiliation.HOME, TitanType.MAGE),
            new Titan(0, 0, TeamAffiliation.HOME, TitanType.SUPPORT),

            new Titan(0, 0, TeamAffiliation.AWAY, TitanType.RANGER),
            new Titan(0, 0, TeamAffiliation.AWAY, TitanType.ARTISAN),
            new Titan(0, 0, TeamAffiliation.AWAY, TitanType.MARKSMAN),
            new Titan(0, 0, TeamAffiliation.AWAY, TitanType.STEALTH) //bugfix where not displayed
    };
    @JsonProperty
    public GoalHoop homeHiGoal = new GoalHoop(HOME_HI_X, HOME_HI_Y, 56, 70, TeamAffiliation.HOME);
    @JsonProperty
    public GoalHoop awayHiGoal = new GoalHoop(AWAY_HI_X, AWAY_HI_Y, 56, 70, TeamAffiliation.AWAY);
    @JsonProperty
    public GoalHoop[] hiGoals = {homeHiGoal, awayHiGoal};
    @JsonProperty
    public GoalHoop[] lowGoals = {new GoalHoop(189, 353, 23, 97, TeamAffiliation.HOME),
            new GoalHoop(189, 789, 23, 97, TeamAffiliation.HOME),
            new GoalHoop(1901, 353, 23, 97, TeamAffiliation.AWAY),
            new GoalHoop(1901, 789, 23, 97, TeamAffiliation.AWAY)};

    @JsonProperty
    public Team away = new Team(TeamAffiliation.AWAY, 0.0, awayHiGoal, lowGoals[2], lowGoals[3]);
    @JsonProperty
    public Team home = new Team(TeamAffiliation.HOME, 0.0, homeHiGoal, lowGoals[0], lowGoals[1],
            players);
    @JsonProperty
    public Entity[] allSolids;
    @JsonProperty
    public List<PlayerDivider> clients;
    @JsonProperty
    public ClientPacket[] lastControlPacket = null;
    @JsonProperty
    public List<ShapePayload> colliders;
    @JsonProperty
    public List<Entity> entityPool = new ArrayList<>();


    public boolean anyPoss() {
        for (Titan t : players) {
            if (t.possession == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean anyBallMoveState() {
        for (Titan t : players) {
            if (t.actionState == Titan.TitanState.SHOOT ||
                    t.actionState == Titan.TitanState.CURVE_RIGHT || t.actionState ==
                    Titan.TitanState.CURVE_LEFT || t.actionState == Titan.TitanState.PASS) {
                return true;
            }
        }
        return false;
    }


    public PlayerDivider clientFromTitan(Entity t) {
        for (PlayerDivider pd : clients) {
            List<Integer> sels = pd.possibleSelection;
            for (Integer i : sels) {
                if (players[i - 1].id.equals(t.id)) {
                    return pd;
                }
            }
        }
        return null;
    }

    public void cullOldColliders() {
        if (this.colliders == null) {
            this.colliders = new ArrayList<>();
        }
        ArrayList<ShapePayload> rm = new ArrayList<>();
        for (int i=0; i<this.colliders.size(); i++) {
            ShapePayload coll = this.colliders.get(i);
            if (!coll.checkDisp()) {
                rm.add(coll);
            }
        }
        this.colliders.removeAll(rm);
    }
}
