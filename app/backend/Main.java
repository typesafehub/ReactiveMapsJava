package backend;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import actors.RegionManagerClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import akka.cluster.Cluster;

/**
 * Main class for starting a backend node.
 * A backend node can have two roles: "backend-region" and/or "backend-summary".
 * The lowest level regions run on nodes with role "backend-region".
 * Summary level regions run on nodes with role "backend-summary".
 * <p>
 * The roles can be specfied on the sbt command line as:
 * {{{
 * sbt -Dakka.remote.netty.tcp.port=0 -Dakka.cluster.roles.1=backend-region -Dakka.cluster.roles.2=backend-summary "run-main backend.Main"
 * }}}
 * <p>
 * If the node has role "frontend" it starts the simulation bots.
 */
public class Main {
    public static void main(String... args) {
        ActorSystem system = ActorSystem.create("application");

        if (Cluster.get(system).getSelfRoles().stream().anyMatch(r -> r.startsWith("backend"))) {
            system.actorOf(RegionManager.props(), "regionManager");
        }

        if (Settings.SettingsProvider.get(system).BotsEnabled && Cluster.get(system).getSelfRoles().contains("frontend")) {
            ActorRef regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient");

            int id = 1;
            URL url = Main.class.getClassLoader().getResource("bots/" + id + ".json");
            List<URL> urls = new ArrayList<>();
            while (url != null) {
                urls.add(url);
                id++;
                url = Main.class.getClassLoader().getResource("bots/" + id + ".json");
            }
            system.actorOf(BotManager.props(regionManagerClient, urls));
        }
    }
}