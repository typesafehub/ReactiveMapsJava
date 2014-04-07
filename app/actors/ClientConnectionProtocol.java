package actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.geojson.Polygon;

public abstract class ClientConnectionProtocol {

    /**
     * Events to/from the client side
     */
    @JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME, property = "event")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserPositions.class, name = "user-positions"),
            @JsonSubTypes.Type(value = ViewingArea.class, name = "viewing-area"),
            @JsonSubTypes.Type(value = UserMoved.class, name = "user-moved"),
    })
    public static abstract class ClientEvent {

        private ClientEvent() {
        }

    }

    /**
     * Event sent to the client when one or more users have updated their position in the current area
     */
    public static class UserPositions extends ClientEvent {
        private final FeatureCollection positions;

        @JsonCreator
        public UserPositions(@JsonProperty("positions") FeatureCollection positions) {
            this.positions = positions;
        }

        public FeatureCollection getPositions() {
            return positions;
        }
    }

    /**
     * Event sent from the client when the viewing area has changed
     */
    public static class ViewingArea extends ClientEvent {
        private final Polygon area;

        @JsonCreator
        public ViewingArea(@JsonProperty("area") Polygon area) {
            this.area = area;
        }

        public Polygon getArea() {
            return area;
        }
    }

    /**
     * Event sent from the client when they have moved
     */
    public static class UserMoved extends ClientEvent {
        private final Point position;

        @JsonCreator
        public UserMoved(@JsonProperty("position") Point position) {
            this.position = position;
        }

        public Point getPosition() {
            return position;
        }
    }


}
