package gameserver;

import authserver.SpringContextBridge;
import authserver.users.UserService;
import networking.CandidateGame;
import networking.ClientPacket;
import networking.PlayerDivider;
import util.Util;

import java.util.*;

public class GameTenant {
    public static final ServerMode serverMode = ServerMode.TRUETWO;
    public static int PLAYERS;//instantiated in static block

    public GameEngine state;
    public String gameId;
    List<PlayerDivider> clients = new ArrayList<>();
    public List<List<Integer>> availableSlots;
    int claimIndex = 0;
    private UserService userService;

    public GameTenant() {
    }

    List<Integer> nextUnclaimedSlot(){
        claimIndex++;
        return availableSlots.get(claimIndex -1);
    }

    public PlayerDivider playerFromToken(String token){
        String email = Util.jwtExtractEmail(token);
        PlayerDivider pd = null;
        for(PlayerDivider p : state.clients){
            if(p.getEmail().equals(email)){
                pd = p;
                pd.setEmail(email);
            }
        }
        return pd;
    }

    void addOrReplaceNewClient(List<PlayerDivider> queue, String token){
        String email = Util.jwtExtractEmail(token);
        if (!accountQueued(queue, email)) {
            for(PlayerDivider p : queue){
                System.out.println(p.toString());
            }
            System.out.println("adding NEW client");
            queue.add(new PlayerDivider(nextUnclaimedSlot(), email));
            if(lobbyFull(queue)){
                startGame(queue);
            }
        }
    }

    private void startGame(List<PlayerDivider> gameIncludedClients){
        System.out.println("starting full");
        state = new GameEngine(gameId, clients); //Start the game
        try {
            state.initializeServer();
            instantiateSpringContext();
            gameIncludedClients = this.monteCarloBalance(gameIncludedClients);
            int seconds = 5;
            for(int i=0; i<5; i++){
                Thread.sleep(1000);
                seconds -=1;
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("reassigning client list on startgame");
        for(PlayerDivider p : clients){
            System.out.println(p.toString());
        }
        clients = gameIncludedClients;
        for(PlayerDivider p : clients){
            System.out.println(p.toString());
        }
    }

    boolean lobbyFull(List<PlayerDivider> pd){
        List<String> uniqueEmails = new ArrayList<>();
        for(PlayerDivider p : pd){
            if(!uniqueEmails.contains(p.getEmail())){
                uniqueEmails.add(p.getEmail());
            }
        }
        return (uniqueEmails.size() == availableSlots.size());
    }

    public void delegatePacket(ClientPacket request) {
        if (state != null) {
            state.processClientPacket(playerFromToken(request.token), request);
        }
    }

    private boolean accountQueued(List<PlayerDivider> queue, String email) {
        boolean emailFound = false;
        for(PlayerDivider p : queue){
            if (p.getEmail().equals(email)){
                emailFound = true;
            }
        }
        return emailFound;
    }

    private void instantiateSpringContext() {
        userService = SpringContextBridge.services().getUserService();
    }

    private List<PlayerDivider> monteCarloBalance(List<PlayerDivider> players) {
        Map<String, Double> tempRating= new HashMap<>();
        for(PlayerDivider pl : players){
            System.out.println(pl.email +  " " + userService.findUserByEmail(pl.email).getRating());
            tempRating.put(pl.email, userService.findUserByEmail(pl.email).getRating());
        }
        final int MAX_MM = 5;
        CandidateGame candidateGame= new CandidateGame();
        for(int i=0; i<MAX_MM; i++){
            //The final possibleSelection is still wrong, maybe trash this last list constructor
            List<PlayerDivider> testOrder = new ArrayList<>(players);
            Collections.shuffle(testOrder);
            List<PlayerDivider> home = testOrder.subList(0, testOrder.size() / 2);
            List<PlayerDivider> away = testOrder.subList(testOrder.size() / 2, testOrder.size());
            candidateGame.suggestTeams(home, away, tempRating);
        }
        return candidateGame.bestMonteCarloBalance(availableSlots);
    }

    static{
        if(serverMode == ServerMode.ALL){
            PLAYERS = 1;
        }
        if(serverMode == ServerMode.TEAMS){
            PLAYERS = 2;
        }
        if(serverMode == ServerMode.SOLONOGOL){
            PLAYERS = 8;
        }
        if(serverMode == ServerMode.SOLOS){
            PLAYERS = 10;
        }
        if(serverMode == ServerMode.TWOS){
            PLAYERS = 4;
        }
        if(serverMode == ServerMode.THREES){
            PLAYERS = 6;
        }
        if(serverMode == ServerMode.TRUETWO){
            PLAYERS = 4;
        }
        if(serverMode == ServerMode.TRUETHREE){
            PLAYERS = 6;
        }
        if(serverMode == ServerMode.ONEVTWO){
            PLAYERS = 3;
        }
    }

    public GameTenant(String id){
        this.gameId = id;
        if(GameTenant.serverMode == ServerMode.TEAMS) {
            this.availableSlots = new ArrayList<>();
            ArrayList<Integer> c1 = new ArrayList<>();
            c1.add(4);
            c1.add(1);
            c1.add(3);
            c1.add(5);
            c1.add(6);
            ArrayList<Integer> c2 = new ArrayList<>();
            c2.add(8);
            c2.add(2);
            c2.add(7);
            c2.add(9);
            c2.add(10);
            this.availableSlots.add(c1);
            this.availableSlots.add(c2);
        }
        else if(GameTenant.serverMode == ServerMode.ALL) {
            this.availableSlots = new ArrayList<>();
            List<Integer> c1 = new ArrayList<>();
            c1.add(4);
            c1.add(3);
            c1.add(5);
            c1.add(6);
            c1.add(1);

            c1.add(8);
            c1.add(7);
            c1.add(9);
            c1.add(10);
            c1.add(2);
            this.availableSlots.add(c1);
        }
        else if(GameTenant.serverMode == ServerMode.SOLOS) {
            this.availableSlots = new ArrayList<>();
            List<Integer> c1 = new ArrayList<>();
            List<Integer> c2 = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            List<Integer> c7 = new ArrayList<>();
            List<Integer> c8 = new ArrayList<>();
            List<Integer> c9 = new ArrayList<>();
            List<Integer> c10 = new ArrayList<>();
            c1.add(1);
            c2.add(2);
            c3.add(3);
            c4.add(4);
            c5.add(5);
            c6.add(6);
            c7.add(7);
            c8.add(8);
            c9.add(9);
            c10.add(10);
            this.availableSlots.add(c1);
            this.availableSlots.add(c2);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
            this.availableSlots.add(c7);
            this.availableSlots.add(c8);
            this.availableSlots.add(c9);
            this.availableSlots.add(c10);
        }
        else if(GameTenant.serverMode == ServerMode.SOLONOGOL){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            List<Integer> c7 = new ArrayList<>();
            List<Integer> c8 = new ArrayList<>();
            List<Integer> c9 = new ArrayList<>();
            List<Integer> c10 = new ArrayList<>();
            c3.add(3);
            c3.add(1);
            c4.add(4);
            c5.add(5);
            c6.add(6);

            c7.add(7);
            c7.add(2);//def covers goalie
            c8.add(8);
            c9.add(9);
            c10.add(10);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
            this.availableSlots.add(c7);
            this.availableSlots.add(c8);
            this.availableSlots.add(c9);
            this.availableSlots.add(c10);
        }
        else if(GameTenant.serverMode == ServerMode.THREES){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            List<Integer> c7 = new ArrayList<>();
            List<Integer> c8 = new ArrayList<>();
            c3.add(3);
            c3.add(1);
            c4.add(4);
            c5.add(5);
            c5.add(6);

            c6.add(7);
            c6.add(2);//def covers goalie
            c7.add(8);
            c8.add(9);
            c8.add(10);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
            this.availableSlots.add(c7);
            this.availableSlots.add(c8);
        }
        else if(GameTenant.serverMode == ServerMode.TWOS){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            c3.add(3);
            c3.add(1);
            c4.add(4);
            c4.add(5);
            c4.add(6);

            c5.add(7);
            c5.add(2);//def covers goalie
            c6.add(8);
            c6.add(9);
            c6.add(10);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
        }
        else if(GameTenant.serverMode == ServerMode.TRUETWO){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            c3.add(1); //goalies are removed anyway if disabled for "true" 2v2
            c3.add(3);
            c4.add(4);

            c5.add(2);
            c5.add(5);
            c6.add(6);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
        }
        else if(GameTenant.serverMode == ServerMode.TRUETHREE){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            List<Integer> c6 = new ArrayList<>();
            List<Integer> c7 = new ArrayList<>();
            List<Integer> c8 = new ArrayList<>();
            c3.add(1);//goalies are removed anyway if disabled for "true" 3v3
            c3.add(3);
            c4.add(4);
            c5.add(5);

            c6.add(2);
            c6.add(6);
            c7.add(7);
            c8.add(8);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
            this.availableSlots.add(c6);
            this.availableSlots.add(c7);
            this.availableSlots.add(c8);
        }
        else if(GameTenant.serverMode == ServerMode.ONEVTWO){
            this.availableSlots = new ArrayList<>();
            List<Integer> c3 = new ArrayList<>();
            List<Integer> c4 = new ArrayList<>();
            List<Integer> c5 = new ArrayList<>();
            c3.add(1); //goalies are removed anyway if disabled for "true" 2v1
            c3.add(3);
            c4.add(4);

            c5.add(2);
            c5.add(5);
            c5.add(6);
            this.availableSlots.add(c3);
            this.availableSlots.add(c4);
            this.availableSlots.add(c5);
        }
    }
}
