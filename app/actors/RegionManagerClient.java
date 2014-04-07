package actors;

import akka.actor.*;
import backend.*;
import backend.RegionManagerProtocol.UpdateUserPosition;
import akka.routing.FromConfig;
import models.backend.PointOfInterest.UserPosition;
import models.backend.RegionId;

/**
 * A client for the region manager, handles routing of position updates to the
 * regionManager on the right backend node.
 */
public class RegionManagerClient extends UntypedActor {
  public static Props props() {
      return Props.create(RegionManagerClient.class, RegionManagerClient::new);
  }

    private final ActorRef regionManagerRouter =
            getContext().actorOf(Props.empty().withRouter(FromConfig.getInstance()), "router");
    private final SettingsImpl settings = Settings.SettingsProvider.get(getContext().system());

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof UserPosition) {
            UserPosition pos = (UserPosition) msg;
            RegionId regionId = settings.GeoFunctions.regionForPoint(pos.getPosition());
            regionManagerRouter.tell(new UpdateUserPosition(regionId, pos), self());
        }
    }
}