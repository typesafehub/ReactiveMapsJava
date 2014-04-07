package backend;

import akka.actor.*;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator.Publish;
import scala.concurrent.duration.Deadline;
import models.backend.*;
import models.backend.PointOfInterest.*;

import play.libs.F.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * These sit at the lowest level, and hold all the users in that region, and publish their summaries up.
 * User position updates are published to subscribers of the topic with the region id.
 */
public class Region extends UntypedActor {

    public static Props props(RegionId regionId) {
        return Props.create(Region.class, () -> new Region(regionId));
    }

    private static final Object TICK = new Object();

    private final ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private final SettingsImpl settings = Settings.SettingsProvider.get(getContext().system());

    private final RegionId regionId;
    private final BoundingBox regionBounds;

    public Region(RegionId regionId) {
        this.regionId = regionId;

        this.regionBounds = settings.GeoFunctions.boundingBoxForRegion(regionId);
    }

    /**
     * The active points for this region, keyed by region id.
     *
     * The values are the points for the region, tupled with the deadline they are valid until.
     */
    private final Map<String, Tuple<UserPosition, Deadline>> activeUsers = new HashMap<>();

    private final Cancellable tickTask = getContext().system().scheduler().schedule(settings.SummaryInterval.$div(2),
            settings.SummaryInterval, self(), TICK, getContext().dispatcher(), self());

    public void postStop() throws Exception {
        tickTask.cancel();
    }

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof UserPosition) {
            UserPosition pos = (UserPosition) msg;

            activeUsers.put(pos.getId(), new Tuple<>(pos, Deadline.now().$plus(settings.ExpiryInterval)));
            // publish new user position to subscribers
            mediator.tell(new Publish(regionId.getName(), pos), self());

        } else if (msg == TICK) {
            // expire inactive users
            Set<String> expired = activeUsers.entrySet().stream()
                    .filter(user -> user.getValue()._2.isOverdue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            expired.forEach(activeUsers::remove);

            // Cluster
            RegionPoints points = new RegionPoints(regionId, settings.GeoFunctions.cluster(regionId.getName(),
                    regionBounds, activeUsers.values().stream().map(u -> u._1).collect(Collectors.toList())));

            // propagate the points to the summary region via the parent manager
            getContext().parent().tell(points, self());

            // stop the actor when no active users
            if (activeUsers.isEmpty()) {
                getContext().stop(self());
            }
        }
    }

}
