package models.backend;

import com.google.common.collect.ImmutableList;

import java.util.Collection;

/**
 * The points of interest for a given regionId.
 */
public class RegionPoints {

    private final RegionId regionId;
    private final Collection<PointOfInterest> points;

    public RegionPoints(RegionId regionId, Collection<PointOfInterest> points) {
        this.regionId = regionId;
        this.points = ImmutableList.copyOf(points);
    }

    public RegionId getRegionId() {
        return regionId;
    }

    public Collection<PointOfInterest> getPoints() {
        return points;
    }
}
