package models.backend;

/**
 * A region id.
 *
 * The zoomLevel indicates how deep this region is zoomed, a zoom level of 8 means that there are 2 ^^ 8 steps on the
 * axis of this zoomLevel, meaning the zoomLevel contains a total of 2 ^^ 16 regions.
 *
 * The x value starts at 0 at -180 West, and goes to 2 ^^ zoomLevel at 180 East.  The y value starts at 0 at -90 South,
 * and goes to 2 ^^ zoomLevel at 90 North.
 */
public class RegionId {

    private final int zoomLevel;
    private final int x;
    private final int y;
    private final String name;

    public RegionId(int zoomLevel, int x, int y) {
        this.zoomLevel = zoomLevel;
        this.x = x;
        this.y = y;

        this.name = "region-" + zoomLevel + "-" + x + "-" + y;
    }

    public String getName() {
        return name;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegionId regionId = (RegionId) o;

        if (x != regionId.x) return false;
        if (y != regionId.y) return false;
        if (zoomLevel != regionId.zoomLevel) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = zoomLevel;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }
}
