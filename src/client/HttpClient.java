package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import gameserver.models.Game;
import networking.ClientPacket;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

public class HttpClient {

    public String token, refreshToken = null;
    public String gameId;

    public String springEndpoint() {
        return "http://" + System.getenv("host") + ":8080/";
    }

    public String authenticate(String un, String pass) {
        try {
            HttpResponse<JsonNode> response = Unirest.post(springEndpoint() + "login")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body("{\"usernameOrEmail\":\"" + un + "\"," +
                            " \"password\" :\"" + pass + "\"}")
                    .asJson();
            System.out.println("statusCode = " + response.getStatus());
            token = response.getBody().getObject().get("accessToken").toString();
            refreshToken = response.getBody().getObject().get("refreshToken").toString();
        } catch (Exception ex) {
        }
        return token;
    }

    public void refresh() {
        int att = 0;
        while (refreshRequest(this.refreshToken) == 401) {
            token = null;
            att++;
            System.out.println("Session refresh expired, refreshing token attempt: " + att);
        }
    }

    public int refreshRequest(String inputRt) {
        try {
            HttpResponse<JsonNode> response = Unirest.post(springEndpoint() + "refresh")
                    .header("Authorization", "Bearer " + inputRt)
                    .asJson();
            System.out.println("statusCode = " + response.getStatus());
            JSONObject body = response.getBody().getObject();
            this.token = body.getString("accessToken");
            this.refreshToken = body.getString("refreshToken");
            return response.getStatus();
        }catch (Exception e){
            e.printStackTrace();
            return 401;
        }
    }

    public Game update(ClientPacket controlsHeld){
        Optional<HttpResponse<JsonNode>> response;
        response = updateRequest(controlsHeld);
        while (!response.isPresent()) {
            System.out.println("SENDING CONTROLS");
            System.out.println(controlsHeld.toString());
            response = updateRequest(controlsHeld);
            System.out.println("Session update expired, refreshing token");
            refresh();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response.get().getBody().toString(), Game.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Optional<HttpResponse<JsonNode>> updateRequest(ClientPacket controlsHeld) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            HttpResponse<JsonNode> response = Unirest.post(springEndpoint() + "ingame")
                    .header("Authorization", "Bearer " + token)
                    .header("Connection", "Keep-Alive")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(mapper.writeValueAsString(controlsHeld))
                    .asJson();
            System.out.println("statusCode = " + response.getStatus());
            System.out.println("gameId = " + gameId);
            return Optional.of(response);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void join() throws UnirestException {
        while (joinRequest() == 401) {
            System.out.println("Session join expired, refreshing token");
            refresh();
        }
    }

    public int joinRequest() {
        try {
            HttpResponse<String> response = Unirest.get(springEndpoint() + "join")
                    .header("Authorization", "Bearer " + token)
                    .asString();
            System.out.println("statusCode = " + response.getStatus());
            gameId = response.getBody();
            System.out.println("gameId = " + gameId);
            return response.getStatus();
        }catch (Exception e){
            return 401;
        }
    }

    public void leave() throws UnirestException {
        while (leaveRequest() == 401) {

            System.out.println("Session leave expired, refreshing token");
            refresh();
        }
    }

    public int leaveRequest() throws UnirestException {
        try {
            HttpResponse<String> response = Unirest.get(springEndpoint() + "leave")
                    .header("Authorization", "Bearer " + token)
                    .asString();
            System.out.println("statusCode = " + response.getStatus());
            gameId = response.getBody();
            System.out.println("gameId = " + gameId);
            return response.getStatus();
        }catch (Exception e){
            return 401;
        }
    }

    public void stat() throws UnirestException {
        while (statRequest() == 401) {
            System.out.println("Session stat expired, refreshing token");
            refresh();
        }
    }

    public int statRequest(){
        try {
            HttpResponse<String> response = Unirest.get(springEndpoint() + "stat")
                    .header("Authorization", "Bearer " + token)
                    .asString();

        System.out.println("statusCode = " + response.getStatus());
        gameId = response.getBody();
        System.out.println("gameId = " + gameId);
        return response.getStatus();
        }
    catch (Exception e){
        return 401;
    }
    }

    public void check() throws UnirestException {
        while (checkRequest() == 401) {
            System.out.println("Session check expired, refreshing token");
            refresh();
        }
    }

    public int checkRequest(){
        try {
            HttpResponse<String> response = Unirest.get(springEndpoint() + "gamecheck")
                    .header("Authorization", "Bearer " + token)
                    .asString();
            System.out.println("statusCode = " + response.getStatus());
            System.out.println("checking");
            gameId = response.getBody();
            System.out.println("gameId = " + gameId);
            return response.getStatus();
        }
        catch (Exception e){
            return 401;
        }
    }
}
