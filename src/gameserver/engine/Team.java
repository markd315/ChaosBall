package gameserver.engine;


import com.fasterxml.jackson.annotation.JsonProperty;
import gameserver.entity.Titan;

import java.util.ArrayList;
import java.util.List;

public class Team {
    @JsonProperty
    public double score;
    @JsonProperty
    public boolean hasBall;
    @JsonProperty
    public TeamAffiliation which;
    @JsonProperty
    List<GoalHoop> toScore = new ArrayList<>();
    @JsonProperty
    List<Titan> players = new ArrayList<>();

    public Team(TeamAffiliation which, double score, Object... playersAndHoops){
        allTeams.add(this);
        this.score = score;
        this.which = which;
        for(Object g : playersAndHoops){
            if(g instanceof GoalHoop){
                toScore.add((GoalHoop) g);
            }
            if(g instanceof Titan){
                players.add((Titan) g);
            }
        }
    }

    public static Team getTeamFromAffilitation(TeamAffiliation enumerated){


        for(int i=0; i < allTeams.size(); i++){
            if(allTeams.get(i).which == enumerated){
                return allTeams.get(i);
            }
        }
        return null;
    }

    public static List<Team> allTeams = new ArrayList<>();

    public Team(){
    }
}
