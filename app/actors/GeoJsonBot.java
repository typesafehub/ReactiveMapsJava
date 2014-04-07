package actors;

import akka.actor.*;
import models.backend.*;
import models.backend.PointOfInterest.UserPosition;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import scala.concurrent.duration.Duration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * A bot that walks back and forth along a GeoJSON LineString.
 */
public class GeoJsonBot extends UntypedActor {

    public static Props props(LineString trail, double offsetLat, double offsetLng, String userId, ActorRef regionManagerClient) {
        return Props.create(GeoJsonBot.class, () -> new GeoJsonBot(trail, offsetLat, offsetLng, userId, regionManagerClient));
    }

    private static final Object STEP = new Object();

    private final LineString trail;
    private final double offsetLat;
    private final double offsetLng;
    private final String userId;
    private final ActorRef regionManagerClient;
    private final ActorRef positionSubscriber;

    public GeoJsonBot(LineString trail, double offsetLat, double offsetLng, String userId, ActorRef regionManagerClient) {
        this.trail = trail;
        this.offsetLat = offsetLat;
        this.offsetLng = offsetLng;
        this.userId = userId;
        this.regionManagerClient = regionManagerClient;

        this.positionSubscriber = getContext().actorOf(PositionSubscriber.props(self()), "positionSubscriber");
    }

    private int pos = 0;
    private int direction = -1;
    private int stepCount = 0;

    private final Cancellable stepTask = getContext().system().scheduler().schedule(
            Duration.apply(1, TimeUnit.SECONDS), Duration.apply(1, TimeUnit.SECONDS), self(), STEP,
            getContext().dispatcher(), self());

    public void postStop() throws Exception {
        stepTask.cancel();
    }

    public void onReceive(Object msg) throws Exception {
        if (msg == STEP) {
            if (pos == trail.getCoordinates().size() - 1 || pos == 0) {
                direction = -direction;
            }
            pos += direction;

            LngLatAlt c = trail.getCoordinates().get(pos);
            UserPosition userPos = new UserPosition(userId, System.currentTimeMillis(),
                    new LatLng(c.getLatitude() + offsetLat, c.getLongitude() + offsetLng));
            regionManagerClient.tell(userPos, self());

            stepCount++;
            if (stepCount % 30 == 0) {
                double w = ThreadLocalRandom.current().nextDouble() * 10.0;
                double h = ThreadLocalRandom.current().nextDouble() * 20.0;
                LatLng southWest = new LatLng(c.getLatitude() + offsetLat - w / 2, c.getLongitude() + offsetLng - h / 2);
                LatLng northEast = new LatLng(c.getLatitude() + offsetLat + w / 2, c.getLongitude() + offsetLng + h / 2);
                positionSubscriber.tell(new BoundingBox(southWest, northEast), self());
            }
        }

    }
}
