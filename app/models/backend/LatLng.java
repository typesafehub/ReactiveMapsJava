package models.backend;

import org.geojson.LngLatAlt;

/**
 * A latitude and longitude point
 */
public final class LatLng {

    private final double lat;
    private final double lng;

    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public static LatLng fromLngLatAlt(LngLatAlt point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    public LngLatAlt toLngLatAlt() {
        return new LngLatAlt(lng, lat);
    }

    @Override
    public String toString() {
        return "LatLng(" + lat + ", " + lng + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LatLng latLng = (LatLng) o;

        if (Double.compare(latLng.lat, lat) != 0) return false;
        if (Double.compare(latLng.lng, lng) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
