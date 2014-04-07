package actors;

import akka.actor.*;
import actors.PositionSubscriberProtocol.PositionSubscriberUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import models.backend.*;
import actors.ClientConnectionProtocol.*;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import play.libs.Json;

import java.util.stream.Collectors;

/**
 * Represents a client connection
 */
public class ClientConnection extends UntypedActor {

    /**
     * @param email               The email address of the client
     * @param upstream            The upstream actor to send to
     * @param regionManagerClient The region manager client to send updates to
     */
    public static Props props(String email, ActorRef upstream, ActorRef regionManagerClient) {
        return Props.create(ClientConnection.class, () -> new ClientConnection(email, upstream, regionManagerClient));
    }

    private final String email;
    private final ActorRef upstream;
    private final ActorRef regionManagerClient;
    private final ActorRef subscriber;

    private ClientConnection(String email, ActorRef upstream, ActorRef regionManagerClient) {
        this.email = email;
        this.upstream = upstream;
        this.regionManagerClient = regionManagerClient;

        this.subscriber = getContext().actorOf(PositionSubscriber.props(self()), "positionSubscriber");
    }

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof JsonNode) {
            ClientEvent event = Json.fromJson((JsonNode) msg, ClientEvent.class);

            if (event instanceof UserMoved) {
                UserMoved userMoved = (UserMoved) event;
                regionManagerClient.tell(new PointOfInterest.UserPosition(email, System.currentTimeMillis(),
                        LatLng.fromLngLatAlt(userMoved.getPosition().getCoordinates())), self());
            } else if (event instanceof ViewingArea) {
                ViewingArea viewingArea = (ViewingArea) event;
                subscriber.tell(BoundingBox.fromBbox(viewingArea.getArea().getBbox()), self());
            }

        } else if (msg instanceof PositionSubscriberUpdate) {

            PositionSubscriberUpdate update = (PositionSubscriberUpdate) msg;
            FeatureCollection collection = new FeatureCollection();

            collection.setFeatures(update.getUpdates().stream().map(pos -> {
                Feature feature = new Feature();
                Point point = new Point();

                point.setCoordinates(pos.getPosition().toLngLatAlt());
                feature.setGeometry(point);

                feature.setId(pos.getId());
                feature.setProperty("timestamp", pos.getTimestamp());
                if (pos instanceof PointOfInterest.Cluster) {
                    feature.setProperty("count", ((PointOfInterest.Cluster) pos).getCount());
                }

                return feature;
            }).collect(Collectors.toList()));

            update.getArea().ifPresent(bbox -> collection.setBbox(bbox.toBbox()));

            upstream.tell(Json.toJson(new UserPositions(collection)), self());
        }
    }
}
