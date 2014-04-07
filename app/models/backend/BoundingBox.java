package models.backend;

/**
 * A bounding box
 */
public class BoundingBox {

    private final LatLng southWest;
    private final LatLng northEast;

    /**
     * @param southWest The south western most point
     * @param northEast The north eastern most point
     */
    public BoundingBox(LatLng southWest, LatLng northEast) {
        this.southWest = southWest;
        this.northEast = northEast;
    }

    public LatLng getSouthWest() {
        return southWest;
    }

    public LatLng getNorthEast() {
        return northEast;
    }

    public double[] toBbox() {
        return new double[] {southWest.getLng(), southWest.getLat(), northEast.getLng(),
                northEast.getLat()};
    }

    public static BoundingBox fromBbox(double[] bbox) {
        assert(bbox.length == 4);
        return new BoundingBox(new LatLng(bbox[1], bbox[0]), new LatLng(bbox[3], bbox[2]));
    }
}
