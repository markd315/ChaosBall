package authserver.matchmaking;

import authserver.Controller;
import gameserver.GameTenant;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Matchmaker {
    private Set<String> waitingPool = new HashSet<>();//user emails
    private Map<String, String> gameMap = new HashMap<>();//user emails -> game id
    private int desperation = 0; //TODO increase to eventually sacrifice match quality

    public String findGame(Authentication login){
        return findGame(login.getName());
    }

    public String findGame(String email){
        if(gameMap.containsKey(email)){
            return gameMap.get(email);
        }
        if(waitingPool.contains(email)) {
            return "WAITING";
        }
        return "NOT QUEUED";
    }

    public Map<String, String> getGameMap(){
        return gameMap;
    }

    private void makeMatches() {
        if(waitingPool.size() >= GameTenant.PLAYERS){
            Set<String> gameFor = new HashSet<>();
            int gameMembers = 0;
            for(String email : waitingPool){
                gameFor.add(email);
                gameMembers++;
                if(gameMembers == GameTenant.PLAYERS){
                    break;
                }
            }
            waitingPool.removeAll(gameFor);
            spawnGame(gameFor);
        }
    }

    private void spawnGame(Set<String> gameFor){
        UUID gameId = UUID.randomUUID();
        for(String email : gameFor) {
            gameMap.put(email, gameId.toString());
        }
        Controller.addNewGame(gameId.toString(), this);
    }

    public void registerIntent(Authentication login) {
        String email = login.getName();
        boolean contains = false;
        for(String e : waitingPool){
            if(e.equals(email)){ //check by email in case some other attr changed
                contains = true;
            }
        }
        if(!contains){
            waitingPool.add(email);
            makeMatches();
        }
    }

    public void removeIntent(Authentication login){
        String email = login.getName();
        String rm = null;
        for(String e : waitingPool){
            if(e.equals(email)){
                rm = e;
            }
        }
        if(rm != null){
            waitingPool.remove(rm);
        }
    }

    public void endGame(String id){
        gameMap.values().removeIf(id::equals);
    }
}
