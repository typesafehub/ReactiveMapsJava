package models.backend;

/**
 * A point of interest, either a user position or a cluster of positions
 */
public abstract class PointOfInterest {

    private final String id;
    private final long timestamp;
    private final LatLng position;

    private PointOfInterest(String id, long timestamp, LatLng position) {
        this.id = id;
        this.timestamp = timestamp;
        this.position = position;
    }

    /**
     * The id of the point of interest
     */
    public String getId() {
        return id;
    }

    /**
     * When the point of interest was created
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The position of the point of interest
     */
    public LatLng getPosition() {
        return position;
    }

    public static class UserPosition extends PointOfInterest {
        public UserPosition(String id, long timestamp, LatLng position) {
            super(id, timestamp, position);
        }
    }

    public static class Cluster extends PointOfInterest {
        private final int count;

        public Cluster(String id, long timestamp, LatLng position, int count) {
            super(id, timestamp, position);
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

}
