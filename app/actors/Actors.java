package actors;

import akka.actor.*;
import akka.cluster.Cluster;
import backend.*
import play.Application;
import play.Play;
import play.Plugin;
import play.libs.Akka;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Lookup for actors used by the web front end.
 */
public class Actors extends Plugin {

    private static Actors actors() {
        return Play.application().plugin(Actors.class);
    }

    /**
     * Get the region manager client.
     */
    public static ActorRef regionManagerClient() {
        return actors().regionManagerClient;
    }

    private final Application app;

    private ActorRef regionManagerClient;

    public Actors(Application app) {
        this.app = app;
    }

    public void onStart() {
        ActorSystem system = Akka.system();

        regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient");

        if (Cluster.get(system).getSelfRoles().stream().anyMatch(r -> r.startsWith("backend"))) {
            system.actorOf(RegionManager.props(), "regionManager");
        }

        if (Settings.SettingsProvider.get(system).BotsEnabled) {
            int id = 1;
            URL url = app.resource("bots/" + id + ".json");
            List<URL> urls = new ArrayList<>();
            while (url != null) {
                urls.add(url);
                id++;
                url = app.resource("bots/" + id + ".json");
            }
            system.actorOf(BotManager.props(regionManagerClient, urls));
        }
    }
}
