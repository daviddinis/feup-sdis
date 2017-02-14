package server;

import java.io.IOException;

/**
 * Created by Nuno on 14/02/2017.
 */
public class ServerRunner {
    private static Server server;

    public static void main(String[] args) throws IOException {
        if(args.length != 1){
            throw new IllegalArgumentException("\nUsage: java server.ServerRunner portNumber");
        }

        int portNumber = Integer.parseInt(args[0]);

        server = new Server(portNumber);

        server.running();
    }
}
