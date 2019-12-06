package gameserver.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import gameserver.GameEngine;
import gameserver.effects.effects.DeadEffect;
import gameserver.engine.TeamAffiliation;

public class Entity extends Box {
    @JsonProperty
    public double health, maxHealth;
    @JsonProperty
    public TeamAffiliation team;
    @JsonProperty
    public double speed = 5;
    @JsonProperty
    public double armorRatio = 1.0;
    @JsonProperty
    public double healReduce = 1.0;

    public Entity() {
    }

    public Entity(TeamAffiliation team) {
        this();
        this.team = team;
        if (team != null && team != TeamAffiliation.HOME && team != TeamAffiliation.AWAY && team != TeamAffiliation.UNAFFILIATED) {
            throw new IllegalArgumentException("Entity team must be a literal, not a comparator");
        }
    }

    public double getHealth() {
        return health;
    }

    public void heal(double health) {
        health/= healReduce;
        this.health += health;
        if (this.health > this.maxHealth)
            this.health = this.maxHealth;
    }

    public void damage(GameEngine context, double health) {
        health /= this.armorRatio;
        this.health -= health;
        if (this.health < 0.0)
            this.die(context);
    }

    private void die(GameEngine context) {
        context.effectPool.addUniqueEffect(new DeadEffect(4000, this));
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public double getHealReduce() {
        return healReduce;
    }

    public void setHealReduce(double healReduce) {
        this.healReduce = healReduce;
    }


    public boolean teamPoss(GameEngine context) {
        for(Titan t : context.players){
            if(t.possession == 1 && t.team.equals(this.team)){
                return true;
            }
        }
        return false;
    }

    public void translateBounded(double dx, double dy) {
        this.X+=dx;
        this.Y+=dy;
        if(this.X > GameEngine.E_MAX_X){
            this.X = GameEngine.E_MAX_X;
        }
        if(this.X < GameEngine.E_MIN_X){
            this.X = GameEngine.E_MIN_X;
        }
        if(this.Y > GameEngine.E_MAX_Y){
            this.Y = GameEngine.E_MAX_Y;
        }
        if(this.Y < GameEngine.E_MIN_Y){
            this.Y = GameEngine.E_MIN_Y;
        }
    }
}
