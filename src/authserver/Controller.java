package authserver;

import authserver.jwt.JwtTokenProvider;
import authserver.matchmaking.Match;
import authserver.matchmaking.Matchmaker;
import authserver.matchmaking.Rating;
import authserver.models.DTO.LoginRequest;
import authserver.models.DTO.UserDTO;
import authserver.models.User;
import authserver.models.responses.JwtAuthenticationResponse;
import authserver.models.responses.UserResponse;
import authserver.users.PersistenceManager;
import authserver.users.UserService;
import com.rits.cloning.Cloner;
import gameserver.GameEngine;
import gameserver.GameTenant;
import gameserver.engine.TeamAffiliation;
import gameserver.entity.Titan;
import gameserver.models.Game;
import networking.ClientPacket;
import networking.PlayerDivider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static util.Util.isRole;

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
public class Controller {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserService userService;

    @Autowired
    Matchmaker userPool;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    AuthenticationManager authenticationManager;

    @PostMapping(value = "/ingame", consumes = MediaType.ALL_VALUE, produces = MediaType.ALL_VALUE)
    public ResponseEntity<Game> submitControls(@Valid @RequestBody ClientPacket controls) {
        GameEngine update = null;
        if(controls.gameID !=null) {
            GameTenant state = states.get(controls.gameID);

            if(state != null){
                instantiateSpringContext();
                checkGameExpiry();

                PlayerDivider player = state.playerFromToken(controls.token);

                //input
                if (controls.token != null) {
                    state.delegatePacket(player, controls);
                }

                //response
                Cloner cloner = new Cloner();
                update = cloner.deepClone(state.state);
                try {
                    update.underControl = state.state.titanSelected(player);
                    System.out.println(update.gameId + update.began);
                    System.out.println(update.toString());
                }
                //Server is not ready for game yet
                catch(NullPointerException ex){
                    Game notStarted = new GameEngine();
                    notStarted.began = false;
                    notStarted.phase = 7;
                    return new ResponseEntity<>(notStarted, HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>(update, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        String ref = tokenProvider.generateRefreshToken(authentication);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, ref));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> reauthenticateUser(@RequestHeader String authorization) {
        String[] newTokens = tokenProvider.bothRefreshed(authorization);
        if(newTokens.length != 2){
            return ResponseEntity.status(401).body("Refresh token not/no longer valid");
        }
        return ResponseEntity.ok(new JwtAuthenticationResponse(newTokens[0], newTokens[1]));
    }

    @RequestMapping("/gamecheck")
    public ResponseEntity<String> gameCheck(Authentication auth) {
        return new ResponseEntity<>(userPool.findGame(auth), HttpStatus.OK);
    }

    @RequestMapping("/join")
    public ResponseEntity<String> joinLobby(Authentication auth) throws IOException {
        userPool.registerIntent(auth);
        return new ResponseEntity<>(userPool.findGame(auth), HttpStatus.OK);
    }

    @RequestMapping("/leave")
    public ResponseEntity<String> leaveLobby(Authentication auth) {
        userPool.removeIntent(auth);
        String game = userPool.findGame(auth);
        if(!game.equals("NOT QUEUED")){
            return new ResponseEntity<>(game, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Successfully withdrawn", HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/stat", method = RequestMethod.POST)
    public ResponseEntity<UserResponse> userStats(@RequestBody @Valid UserDTO userinput) {
        User user = userService.findUserByUsername(userinput.getUsername());
        return new ResponseEntity<>(new UserResponse(user), HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UserResponse> addUser(@RequestBody @Valid UserDTO userinput, BindingResult bindingResult, Authentication auth) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if(!isRole(auth, "ADMIN")){
            return new ResponseEntity<>(null, headers, HttpStatus.FORBIDDEN);
        }
        User user = new User();
        if (bindingResult.hasErrors() || (userinput == null) || userinput.getUsername() == null || userinput.getPassword() == null) {
            //errors.addAllErrors(bindingResult);
            //headers.add("errors", errors.toJSON());
            return new ResponseEntity<>(new UserResponse(user), headers, HttpStatus.BAD_REQUEST);
        }
        user.setUsername(userinput.getUsername());
        String hashed = passwordEncoder.encode(userinput.getPassword());
        user.setPassword(hashed);
        user.setEmail(userinput.getEmail());
        user.setRole(userinput.getRole());
        if(user.getRole() == null){
            user.setRole("USER");//default
        }

        this.userService.saveUser(user);
        return new ResponseEntity<>(new UserResponse(user), headers, HttpStatus.CREATED);
    }

    public static void addNewGame(String id, Matchmaker matchmaker) {
        GameTenant ten = new GameTenant(id);
        ten.passQueueToUsers(matchmaker);
        states.put(id, ten);
    }

    private static void checkGameExpiry() {
        Set<String> rm = new HashSet<>();
        for (String id : states.keySet()) {
            GameTenant val = states.get(id);
            if (val != null && val.state != null && val.state.ended) {
                injectRatingsToPlayers(val.state);
                for (PlayerDivider player : val.state.clients) {
                    try {
                        Titan t = val.state.titanSelected(player);
                        if (t != null) {
                            String className = t.getType().toString();
                            persistenceManager.postgameStats(player.email, val.state.stats, className, player.wasVictorious, player.newRating);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                rm.add(id);
                matchmaker.endGame(id);
            }
        }
        for (String id : rm) {
            states.remove(id);
        }
    }

    private static void injectRatingsToPlayers(GameEngine state) {
        List<Rating> home = new ArrayList<>(), away = new ArrayList<>();
        for (PlayerDivider pl : state.clients) {
            User persistence = persistenceManager.userService.findUserByEmail(pl.email);
            //System.out.println("got user " + persistence.getEmail() + persistence.getRating());
            Rating<User> oldRating = new Rating<>(persistence, persistence.getLosses() + persistence.getWins());
            if (state.players[pl.getSelection() - 1].team == TeamAffiliation.HOME) {
                oldRating.setRating(persistence.getRating());
                home.add(oldRating);
            } else {
                oldRating.setRating(persistence.getRating());
                away.add(oldRating);
            }
        }
        Rating<String> homeRating = new Rating<>(home, "home", 0);
        Rating<String> awayRating = new Rating<>(away, "away", 0);
        Match<User> match = new Match(homeRating, awayRating, state.home.score - state.away.score);
        //System.out.println(match.winMargin + "");
        match.injectAverage(home, away);
        for (PlayerDivider pl : state.clients) {
            updatePlayerRating(pl, home);
            updatePlayerRating(pl, away);
        }
    }

    private static void updatePlayerRating(PlayerDivider pl, List<Rating> team) {
        for (Rating<User> r : team) {
            if (r.getID().getEmail().equals(pl.email)) {
                //System.out.println(r.rating + " new");
                pl.newRating = r.rating;
            }
        }
    }

    private void instantiateSpringContext() {
        userService = SpringContextBridge.services().getUserService();
        persistenceManager = SpringContextBridge.services().getPersistenceManager();
        matchmaker = SpringContextBridge.services().getMatchmaker();
    }

    public static Map<String, GameTenant> states = new HashMap<>(); //UUID onto game

    static Matchmaker matchmaker;

    static PersistenceManager persistenceManager;

    static JwtTokenProvider tp = new JwtTokenProvider();

    static Properties prop;
    static String appSecret;

    static {
        try {
            prop = new Properties();
            prop.load(new FileInputStream(new File("application.properties")));
            appSecret = prop.getProperty("app.jwtSecret");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

// URL: ec2-18-223-131-64.us-east-2.compute.amazonaws.com:8080/usersAPI/login
