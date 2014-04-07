package backend;

import akka.routing.ConsistentHashingRouter.ConsistentHashable;
import models.backend.PointOfInterest;
import models.backend.RegionId;
import models.backend.RegionPoints;

public abstract class RegionManagerProtocol {

    /**
     * Update the users position.
     *
     * Sent by clients of the backend when they want to update a users position.
     */
    public static class UpdateUserPosition implements ConsistentHashable {
        private final RegionId regionId;
        private final PointOfInterest.UserPosition userPosition;

        /**
         * @param regionId The region id that position is in.  This is used as the hash key for deciding which node
         *                 to route the update to.
         * @param userPosition The user position object.
         */
        public UpdateUserPosition(RegionId regionId, PointOfInterest.UserPosition userPosition) {
            this.regionId = regionId;
            this.userPosition = userPosition;
        }

        public RegionId getRegionId() {
            return regionId;
        }

        public PointOfInterest.UserPosition getUserPosition() {
            return userPosition;
        }

        public Object consistentHashKey() {
            return regionId.getName();
        }
    }

    /**
     * Update the region points at a given region.
     *
     * Sent by child regions to update their data in their parent summary region.
     */
    public static class UpdateRegionPoints implements ConsistentHashable {
        private final RegionId regionId;
        private final RegionPoints regionPoints;

        /**
         * @param regionId The region id that position is in.  This is used as the hash key for deciding which node
         *                 to route the update to.
         * @param regionPoints The points to update.
         */
        public UpdateRegionPoints(RegionId regionId, RegionPoints regionPoints) {
            this.regionId = regionId;
            this.regionPoints = regionPoints;
        }

        public RegionId getRegionId() {
            return regionId;
        }

        public RegionPoints getRegionPoints() {
            return regionPoints;
        }

        public Object consistentHashKey() {
            return regionId.getName();
        }
    }


}
