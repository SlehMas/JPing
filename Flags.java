/**
 * Created by salah on 10/1/2018.
 */
public class Flags {
    private static final int MAX_PACKET_SIZE = (65536 - 60 - 8);
    private static boolean timestamp = false;
    private static boolean quiet = false;
    private static boolean flood = false;
    private static boolean ipv4 = false;
    private static boolean ipv6 = false;

    public static boolean isIpv4 () {
    	return ipv4;
    }
    public static void setIpv4 (boolean ipv4) {
    	Flags.ipv4 = ipv4;
    }
    public static boolean isIpv6 () {
    	return ipv6;
    }
    public static void setIpv6 (boolean ipv6) {
    	Flags.ipv6 = ipv6;
    }
    public static boolean isTimestamp() {
        return timestamp;
    }

    public static void setTimestamp(boolean timestamp) {
        Flags.timestamp = timestamp;
    }

    public static boolean isQuiet() {
        return quiet;
    }

    public static void setQuiet(boolean quiet) {
        Flags.quiet = quiet;
    }

    public static int getMaxPacketSize() {
        return MAX_PACKET_SIZE;
    }

    public static boolean isFlood() {
        return flood;
    }

    public static void setFlood(boolean flood) {
        Flags.flood = flood;
    }
}