package backend;

import akka.actor.*;
import akka.routing.FromConfig;
import backend.RegionManagerProtocol.*;
import models.backend.RegionId;
import models.backend.RegionPoints;
import scala.Option;

import java.util.function.Function;

/**
 * Handles instantiating region and summary region actors when data arrives for them, if they don't already exist.
 * It also routes the `RegionPoints` from child `Region` or `SummaryRegion` to the node
 * responsible for the target region.
 */
public class RegionManager extends UntypedActor {

    public static Props props() {
        return Props.create(RegionManager.class, RegionManager::new);
    }

    private final ActorRef regionManagerRouter =
            getContext().actorOf(Props.empty().withRouter(FromConfig.getInstance()), "router");
    private final SettingsImpl settings = Settings.SettingsProvider.get(getContext().system());


    public void onReceive(Object msg) throws Exception {
        if (msg instanceof UpdateUserPosition) {
            UpdateUserPosition update = (UpdateUserPosition) msg;

            ActorRef region = getRegionActor(update.getRegionId(), Region::props);

            region.tell(update.getUserPosition(), self());

        } else if (msg instanceof UpdateRegionPoints) {
            UpdateRegionPoints update = (UpdateRegionPoints) msg;

            ActorRef region = getRegionActor(update.getRegionId(), SummaryRegion::props);

            region.tell(update.getRegionPoints(), self());

        } else if (msg instanceof RegionPoints) {
            RegionPoints points = (RegionPoints) msg;

            // count reported by child region, propagate it to summary region on responsible node
            settings.GeoFunctions.summaryRegionForRegion(points.getRegionId()).ifPresent(summaryRegionId ->
                    regionManagerRouter.tell(new UpdateRegionPoints(summaryRegionId, points), self())
            );
        }
    }

    /**
     * Get the actor for the given region, creating it if it doesn't already exist.
     */
    private ActorRef getRegionActor(RegionId regionId, Function<RegionId, Props> props) {
        Option<ActorRef> maybeChild = context().child(regionId.getName());

        if (maybeChild.isDefined()) {
            return maybeChild.get();
        } else {
            return getContext().actorOf(props.apply(regionId), regionId.getName());
        }
    }
}