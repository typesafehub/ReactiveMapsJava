package backend;

import akka.actor.*;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator.Publish;
import scala.concurrent.duration.Deadline;
import models.backend.*;

import play.libs.F.Tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Summary regions receive region points from their 4 sub regions, cluster them, and publishes the resulting points
 * to subscribers of the topic with the region id.
 */
public class SummaryRegion extends UntypedActor {

    public static Props props(RegionId regionId) {
        return Props.create(SummaryRegion.class, () -> new SummaryRegion(regionId));
    }

    private static final Object TICK = new Object();

    private final ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private final SettingsImpl settings = Settings.SettingsProvider.get(getContext().system());

    private final RegionId regionId;
    private final BoundingBox regionBounds;

    public SummaryRegion(RegionId regionId) {
        this.regionId = regionId;

        this.regionBounds = settings.GeoFunctions.boundingBoxForRegion(regionId);
    }

    /**
     * The active points for this summary region, keyed by summary region id.
     * <p>
     * The values are the points for the summary region, tupled with the deadline they are valid until.
     */
    private final Map<RegionId, Tuple<Collection<PointOfInterest>, Deadline>> activePoints = new HashMap<>();


    private final Cancellable tickTask = getContext().system().scheduler().schedule(settings.SummaryInterval.$div(2),
            settings.SummaryInterval, self(), TICK, getContext().dispatcher(), self());

    public void postStop() throws Exception {
        tickTask.cancel();
    }

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof RegionPoints) {
            RegionPoints points = (RegionPoints) msg;

            activePoints.put(points.getRegionId(), new Tuple<>(points.getPoints(), Deadline.now().$plus(settings.ExpiryInterval)));

        } else if (msg == TICK) {
            // expire inactive points
            Set<RegionId> expired = activePoints.entrySet().stream()
                    .filter(user -> user.getValue()._2.isOverdue())
                    .map(e -> e.getKey())
                    .collect(Collectors.toSet());
            expired.forEach(activePoints::remove);

            // Cluster
            RegionPoints points = new RegionPoints(regionId, settings.GeoFunctions.cluster(regionId.getName(),
                    regionBounds, activePoints.values().stream().flatMap(u -> u._1.stream()).collect(Collectors.toList())));

            // propagate the points to higher level summary region via the manager
            getContext().parent().tell(points, self());

            // publish total count to subscribers
            mediator.tell(new Publish(regionId.getName(), points), self());

            // stop the actor when no active sub-regions
            if (activePoints.isEmpty()) {
                getContext().stop(self());
            }
        }
    }
}
