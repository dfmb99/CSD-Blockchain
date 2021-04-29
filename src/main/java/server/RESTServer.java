package server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import api.WalletResource;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class RESTServer {

    private static Logger Log = Logger.getLogger(RESTServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final String SERVICE = "WalletResourceService";

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Use: RestServer <id> <processId> <server_port>");
            System.exit(-1);
        }

        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format("http://%s:%s/rest", ip, Integer.parseInt(args[2]));

        WalletResource wallet = new WalletResource(Integer.parseInt(args[0]), Integer.parseInt(args[1]));;

        ResourceConfig config = new ResourceConfig();
        config.register(wallet);

        JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

        Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
    }

}