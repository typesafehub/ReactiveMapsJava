package backend;

import akka.actor.Extension;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class SettingsImpl implements Extension {

    SettingsImpl(Config config) {
        this.MaxZoomDepth = config.getInt("reactiveMaps.maxZoomDepth");
        this.MaxSubscriptionRegions = config.getInt("reactiveMaps.maxSubscriptionRegions");
        this.ClusterThreshold = config.getInt("reactiveMaps.clusterThreshold");
        this.ClusterDimension = config.getInt("reactiveMaps.clusterDimension");
        this.SummaryInterval = Duration.apply(config.getMilliseconds("reactiveMaps.summaryInterval"), TimeUnit.MILLISECONDS);
        this.ExpiryInterval = Duration.apply(config.getMilliseconds("reactiveMaps.expiryInterval"), TimeUnit.MILLISECONDS);
        this.SubscriberBatchInterval = Duration.apply(config.getMilliseconds("reactiveMaps.subscriberBatchInterval"), TimeUnit.MILLISECONDS);
        this.GeoFunctions = new GeoFunctions(this);
        this.BotsEnabled = config.getBoolean("reactiveMaps.bots.enabled");
        this.TotalNumberOfBots = config.getInt("reactiveMaps.bots.totalNumberOfBots");
    }

  /**
   * The maximum zoom depth for regions.  The concrete regions will sit at this depth, summary regions will sit above
   * that.
   */
  public final int MaxZoomDepth;

  /**
   * The maximum number of regions that can be subscribed to.
   *
   * This is enforced automatically by selecting the deepest zoom depth for a given bounding box that is covered by
   * this number of regions or less.
   */
  public final int MaxSubscriptionRegions;

  /**
   * The number of points that need to be in a region/summary region before it decides to cluster them.
   */
  public final int ClusterThreshold;

  /**
   * The dimension depth at which to cluster.
   *
   * A region will be clustered into the square of this number boxes.
   */
  public final int ClusterDimension;

  /**
   * The interval at which each region should generate and send its summaries.
   */
  public final FiniteDuration SummaryInterval;

  /**
   * The interval after which user positions and cluster data should expire.
   */
  public final FiniteDuration ExpiryInterval;

  /**
   * The interval at which subscribers should batch their points to send to clients.
   */
  public final FiniteDuration SubscriberBatchInterval;

  /**
   * Geospatial functions.
   */
  public final GeoFunctions GeoFunctions;

  /**
   * Whether this node should run the bots it knows about.
   */
  public final boolean BotsEnabled;

  /**
   * How many bots to create in total
   */
  public final int TotalNumberOfBots;
}
