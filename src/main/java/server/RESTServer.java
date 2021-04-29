package server;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import api.WalletResource;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import utils.InsecureHostnameVerifier;


public class RESTServer {

    private static Logger Log = Logger.getLogger(RESTServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public final static long RETRY_PERIOD = 3000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 5000;
    public static final String SERVICE = "WalletResourceService";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Use: java WalletResource <id> <processId>");
            System.exit(-1);
        }

        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format("http://%s:%s/rest", ip, PORT);

        //This will allow client code executed by this process to ignore hostname verification
        //HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

        WalletResource wallet = new WalletResource(Integer.parseInt(args[0]), Integer.parseInt(args[1]));;

        ResourceConfig config = new ResourceConfig();
        config.register(wallet);

        JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

        Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
        //More code can be executed here...
    }

}