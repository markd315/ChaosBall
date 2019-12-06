package networking;

import client.ChaosballClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import gameserver.models.Game;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JacksonServer {

    private ObjectMapper mapper = new ObjectMapper();
    private Socket socket = new Socket(InetAddress.getByName(System.getenv("host")), 54555);
    private DataInputStream is = new DataInputStream(socket.getInputStream());
    private DataOutputStream os = new DataOutputStream(socket.getOutputStream());
    private PrintWriter pw = new PrintWriter(os);
    private BufferedReader in = new BufferedReader(new InputStreamReader(is));

    public JacksonServer(ChaosballClient client) throws IOException {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(new JacksonServer.ServerIO(client), 1, 20, TimeUnit.MILLISECONDS);
    }

    public class ServerIO implements Runnable {
        private ChaosballClient client;
        public ServerIO(ChaosballClient _data) {
            this.client = _data;
        }

        @Override
        public void run() {
            try {
                ClientPacket controlsHeld = client.controlsHeld;
                client.controlsHeld.gameID = client.loginClient.gameId;
                client.controlsHeld.token = client.loginClient.token;
                client.controlsHeld.masteries = client.masteries;
                String json = mapper.writeValueAsString(controlsHeld) + "\n";
                pw.println(json);
                pw.flush();
                String inputLine;
                boolean once = false;
                while ((inputLine = in.readLine()) != null){
                    once = true;
                    System.out.println("reading a valiue from server");
                    client.game = mapper.readValue(inputLine, Game.class);
                    client.game.began = true;
                    client.phase = client.game.phase;
                    client.controlsHeld.gameID = client.gameID;
                    client.controlsHeld.token = client.loginClient.token;
                    client.controlsHeld.masteries = client.masteries;
                    System.out.println("read a valiue from server");
                }
                if(!once){
                    System.out.println("no connection this time!");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
