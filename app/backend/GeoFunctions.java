package backend;

import com.google.common.collect.ImmutableSet;
import models.backend.*;
import models.backend.PointOfInterest.*;
import org.geojson.LngLatAlt;
import play.libs.F;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Geo functions.
 */
public class GeoFunctions {

    private final SettingsImpl settings;

    GeoFunctions(SettingsImpl settings) {
        this.settings = settings;
    }

    /**
     * Get the region for the given point.
     *
     * @param point The point.
     * @return The id of the region at the given zoom depth.
     */
    public RegionId regionForPoint(LatLng point) {
        return regionForPoint(point, settings.MaxZoomDepth);
    }

    /**
     * Get the region for the given point.
     *
     * @param point     The point.
     * @param zoomDepth The zoom depth.
     * @return The id of the region at the given zoom depth.
     */
    public RegionId regionForPoint(LatLng point, int zoomDepth) {
        assert (zoomDepth <= settings.MaxZoomDepth);
        long axisSteps = 1l << zoomDepth;
        double xStep = 360d / axisSteps;
        int x = (int) Math.floor((point.getLng() + 180) / xStep);
        double yStep = 180d / axisSteps;
        int y = (int) Math.floor((point.getLat() + 90) / yStep);
        return new RegionId(zoomDepth, x, y);
    }

    /**
     * Get the regions for the given bounding box.
     *
     * @param bbox The bounding box.
     * @return The regions
     */
    public Set<RegionId> regionsForBoundingBox(BoundingBox bbox) {
        return regionsAtZoomLevel(bbox, settings.MaxZoomDepth);
    }

    private Set<RegionId> regionsAtZoomLevel(BoundingBox bbox, int zoomLevel) {
        if (zoomLevel == 0) {
            return ImmutableSet.of(new RegionId(0, 0, 0));
        } else {
            int axisSteps = 1 << zoomLevel;
            // First, we get the regions that the bounds are in
            RegionId southWestRegion = regionForPoint(bbox.getSouthWest(), zoomLevel);
            RegionId northEastRegion = regionForPoint(bbox.getNorthEast(), zoomLevel);
            // Now calculate the width of regions we need, we need to add 1 for it to be inclusive of both end regions
            int xLength = northEastRegion.getX() - southWestRegion.getX() + 1;
            int yLength = northEastRegion.getY() - southWestRegion.getY() + 1;
            // Check if the number of regions is in our bounds
            int numRegions = xLength * yLength;
            if (numRegions <= 0) {
                return ImmutableSet.of(new RegionId(0, 0, 0));
            } else if (settings.MaxSubscriptionRegions >= numRegions) {
                // Generate the sequence of regions
                List<RegionId> regions = new ArrayList<>(numRegions);
                for (int i = 0; i < numRegions; i++) {
                    int y = i / xLength;
                    int x = i % xLength;
                    // We need to mod positive the x value, because it's possible that the bounding box started or ended
                    // from less than -180 or greater than 180 W/E.
                    regions.add(new RegionId(zoomLevel, modPositive(southWestRegion.getX() + x, axisSteps),
                            southWestRegion.getY() + y));
                }
                return ImmutableSet.copyOf(regions);
            } else {
                return regionsAtZoomLevel(bbox, zoomLevel - 1);
            }
        }
    }


    /**
     * Get the bounding box for the given region.
     */
    public BoundingBox boundingBoxForRegion(RegionId regionId) {
        long axisSteps = 1l << regionId.getZoomLevel();
        double yStep = 180d / axisSteps;
        double xStep = 360d / axisSteps;
        double latRegion = regionId.getY() * yStep - 90;
        double lngRegion = regionId.getX() * xStep - 180;

        return new BoundingBox(
                new LatLng(latRegion, lngRegion),
                new LatLng(latRegion + yStep, lngRegion + xStep));
    }

    public Optional<RegionId> summaryRegionForRegion(RegionId regionId) {
        if (regionId.getZoomLevel() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(new RegionId(regionId.getZoomLevel() - 1, regionId.getX() >>> 1, regionId.getY() >>> 1));
        }
    }

    /**
   * Cluster the given points into n2 boxes
   *
   * @param id The id of the region
   * @param bbox The bounding box within which to cluster
   * @param points The points to cluster
   * @return The clustered points
   */
  public List<PointOfInterest> cluster(String id, BoundingBox bbox, List<PointOfInterest> points) {
    if (points.size() > settings.ClusterThreshold) {
      return groupNBoxes(bbox, settings.ClusterDimension, points).filter(cluster -> !cluster._2.isEmpty()).map(cluster -> {
          if (cluster._2.size() == 1) {
              return cluster._2.get(0);
          } else {

              // Map to a tuple of the lat/long/count representing the point, then reduce to the totals of these
              F.Tuple3<Double, Double, Integer> result = cluster._2.stream().map(point -> {
                  int count = 1;
                  if (point instanceof Cluster) {
                      // A cluster should have its lat/lng weighted by its count
                      count = ((Cluster) point).getCount();
                  }
                  // Normalise to a 0-360 based version of longitude
                  double normalisedWest = modPositive(point.getPosition().getLng() + 180, 360);
                  return new F.Tuple3<Double, Double, Integer>(point.getPosition().getLat() * count,
                          normalisedWest * count, count);
              }).reduce(new F.Tuple3<>(0d, 0d, 0), (a, b) -> new F.Tuple3<>(a._1 + b._1, a._2 + b._2, a._3 + b._3));

              // Compute averages
              int count = result._3;
              return new Cluster(id + "-" + cluster._1, System.currentTimeMillis(),
                      new LatLng(result._1 / count, (result._2 / count) - 180), count);
          }
      }).collect(Collectors.toList());
    } else {
      return points;
    }
  }

    /**
     * Group the positions into n2 boxes
     *
     * @param bbox      The bounding box
     * @param positions The positions to group
     * @return The grouped positions
     */
    private Stream<F.Tuple<Integer, List<PointOfInterest>>> groupNBoxes(BoundingBox bbox, int n, List<PointOfInterest> positions) {
        F.Tuple<Integer, List<PointOfInterest>>[] boxes = new F.Tuple[n * n];
        for (int i = 0; i < n * n; i++) {
            boxes[i] = new F.Tuple<>(i, new ArrayList<>());
        }
        positions.stream().forEach(pos -> {
            int segment = latitudeSegment(n, bbox.getSouthWest().getLat(), bbox.getNorthEast().getLat(),
                    pos.getPosition().getLat()) * n +
                    longitudeSegment(n, bbox.getSouthWest().getLng(), bbox.getNorthEast().getLng(),
                            pos.getPosition().getLng());
            boxes[segment]._2.add(pos);
        });
        return Arrays.stream(boxes);
    }

    /**
     * Find the segment that the point lies in in the given south/north range
     *
     * @return A number from 0 to n - 1
     */
    public int latitudeSegment(int n, double south, double north, double point) {
        // Normalise so that the southern most point is 0
        double range = north - south;
        double normalisedPoint = point - south;
        int segment = (int) Math.floor(normalisedPoint * (n / range));
        if (segment >= n || segment < 0) {
            // The point was never in the given range.  Default to 0.
            return 0;
        } else {
            return segment;
        }
    }

    /**
     * Find the segment that the point lies in in the given west/east range
     *
     * @return A number from 0 to n - 1
     */
    public int longitudeSegment(int n, double west, double east, double point) {
        // Normalise so that the western most point is 0, taking into account the 180 cut over
        double range = modPositive(east - west, 360);
        double normalisedPoint = modPositive(point - west, 360);
        int segment = (int) Math.floor(normalisedPoint * (n / range));
        if (segment >= n || segment < 0) {
            // The point was never in the given range.  Default to 0.
            return 0;
        } else {
            return segment;
        }
    }

    /**
     * Modulo function that always returns a positive number
     */
    public double modPositive(double x, int y) {
        double mod = x % y;
        if (mod > 0) {
            return mod;
        } else {
            return mod + y;
        }
    }

    /**
     * Modulo function that always returns a positive number
     */
    public int modPositive(int x, int y) {
        int mod = x % y;
        if (mod > 0) {
            return mod;
        } else {
            return mod + y;
        }
    }

}
