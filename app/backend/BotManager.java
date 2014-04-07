package backend;

import actors.GeoJsonBot;
import akka.actor.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import play.libs.Json;
import scala.concurrent.duration.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * Loads and starts GeoJSON bots
 */
public class BotManager extends UntypedActor {
    public static Props props(ActorRef regionManagerClient, List<URL> data) {
        return Props.create(BotManager.class, () -> new BotManager(regionManagerClient, data));
    }

    private final ActorRef regionManagerClient;
    private final List<URL> data;

    public BotManager(ActorRef regionManagerClient, List<URL> data) {
        this.regionManagerClient = regionManagerClient;
        this.data = data;
    }

    int total = 0;
    int max = Settings.SettingsProvider.get(getContext().system()).TotalNumberOfBots;

    private static Object TICK = new Object();

    private final Cancellable tickTask = getContext().system().scheduler().schedule(
            Duration.apply(1, TimeUnit.SECONDS), Duration.apply(3, TimeUnit.SECONDS),
            self(), TICK, getContext().dispatcher(), self());

    public void postStop() throws Exception {
        tickTask.cancel();
    }

    public void onReceive(Object msg) throws Exception {
        if (msg == TICK && total >= max) {
            tickTask.cancel();

        } else if (msg == TICK) {

            int totalBefore = total;
            boolean originalTrail = total == 0;
            for (URL url : data) {
                JsonNode json;
                try (InputStream is = url.openStream()) {
                    json = Json.parse(is);
                }
                FeatureCollection collection = Json.fromJson(json, FeatureCollection.class);
                for (int i = 0; i < collection.getFeatures().size(); i++) {
                    Feature feature = collection.getFeatures().get(i);
                    if (feature.getGeometry() instanceof LineString && total < max) {
                        LineString route = (LineString) feature.getGeometry();
                        total++;
                        String name = Optional.ofNullable(feature.getProperty("name")).orElse("").toString();
                        String userId = "bot-" + total + "-" + ThreadLocalRandom.current().nextInt(1000) + "-" + i + "-" +
                                Optional.ofNullable(feature.getId()).orElse(Integer.toString(i)) + "-" + name;
                        if (originalTrail) {
                            getContext().actorOf(GeoJsonBot.props(route, 0, 0, userId, regionManagerClient));
                        } else {
                            getContext().actorOf(GeoJsonBot.props(route,
                                    ThreadLocalRandom.current().nextDouble() * 15.0,
                                    ThreadLocalRandom.current().nextDouble() * -30.0, userId, regionManagerClient));
                        }
                    } else {
                        throw new RuntimeException("Got unknown geometry: " + feature.getGeometry());
                    }
                }
            }
            System.out.println("Started " + (total - totalBefore) + " bots, total " + total);
        }
    }

}
