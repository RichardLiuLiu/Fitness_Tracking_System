package demo.support;

import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.LatLng;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import demo.model.Point;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class NavUtils {
    private static double EARTH_RADIUS_IN_METERS = DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM * 1000;

    private NavUtils() {
        throw new AssertionError();
    }

    public static double getDistance (Point point1, Point point2) {
        Assert.notNull(point1, "point1 must not be null");
        Assert.notNull(point2, "point2 must not be null");

        final SpatialContext ctx = SpatialContext.GEO;
        com.spatial4j.core.shape.Point p1 = ctx.makePoint(point1.getLongitude(), point1.getLatitude());
        com.spatial4j.core.shape.Point p2 = ctx.makePoint(point2.getLongitude(), point2.getLatitude());

        return DistanceUtils.degrees2Dist(ctx.getDistCalc().distance(p1, p2), DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM) * 1000;
    }

    public static double getTotalDistance(List<Point> points) {
        double totalDistance = 0;
        int count = 0;
        Point prevCount = null;
        for (Point point : points) {
            count++;
            if (count > 1 && count < points.size()) {
                totalDistance += getDistance(prevCount, point);
            }
            prevCount = point;
        }
        return totalDistance;
    }

    public static double getBearing(Point point1, Point point2) {
        double longitude1 = point1.getLongitude();
        double longitude2 = point2.getLongitude();
        double latitude1 = Math.toRadians(point1. getLatitude());
        double latitude2 = Math.toRadians(point2. getLatitude());
        double longDiff = Math.toRadians(longitude1 - longitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        double y = Math.sin(longDiff) * Math.cos(latitude2);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public static Point getPosition(Point point, double d, double brg) {
        if (Double.doubleToRawLongBits(d) == 0) {
            return point;
        }

        double lat1 = Math.toRadians(point.getLatitude());
        double lon1 = Math.toRadians(point.getLongitude());
        double brgAsRadians = Math.toRadians(brg);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / EARTH_RADIUS_IN_METERS)
                    + Math.cos(lat1) * Math.sin(d / EARTH_RADIUS_IN_METERS) * Math.cos(brgAsRadians));
        double x = Math.sin(brgAsRadians) * Math.sin(d / EARTH_RADIUS_IN_METERS) * Math.cos(lat1);
        double y = Math.cos(d / EARTH_RADIUS_IN_METERS) - Math.sin(lat1) * Math.sin(lat2);
        double lon2 = lon1 + Math.atan2(x, y);

        return new Point(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public static List<Point> decodePolyline(String polyline) {
        final List<LatLng> latLngs = PolylineEncoding.decode(polyline);
        return latLngs.stream().map(latLng -> new Point(latLng.lat, latLng.lng)).collect(Collectors.toList());
    }
}
