import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
/**
 *
 * @author salah
 */
public class JPing {

    private static int transmitted = 0; //pings sent successfully
    private static float max = 0; //max roundtrip time
    private static float min = 0; //min roundtrip time
    private static float total = 0; //total roundtrips time
    private static int received = 0; //pings received by destination
    private static int lost = 0; //pings that did not succeed
    private static final long startTime = System.currentTimeMillis();
    private static int interval = 800; //default interval between pings
    private static int npackets = -1; // number of pings, -1 means infinite
    private static int timeout = 1000; // ping timeout
    private static int packetsize = (64 - 8); //bytes to be transmitted, 8 bytes are for the ICMP header size
    private static String host; // destination
    private static String version = "v1.0.0";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            usage ();
            exit(0);
        }
        int argsLen = 0;
        /*
        * This is to read the flags passed in the command line and treat each case accordingly
        * */
        while( argsLen < args.length ) {
            if (args[argsLen].contains("-")) { // this condition is to read flags with dash (-)
                switch (args[argsLen].split("-")[1]) {
                    case "c":
                        npackets = Integer.valueOf(args[argsLen + 1]);
                        if (npackets < 1) {
                            System.out.println("Ping: bad number of packets.");
                        }
                        break;
		    case "4":
			Flags.setIpv4(true);
			if (Flags.isIpv6()) {
			    System.out.println("Only -4 or -6 allowed.");
                            exit(0);			
			}
			break;
		    case "6":
			Flags.setIpv6(true);
			if (Flags.isIpv4()) {
			    System.out.println("Only -4 or -6 allowed.");
                            exit(0);			
			}
			break;
                    case "f":
                        interval = 200;
                        Flags.setFlood(true);
                        break;
                    case "D":
                        Flags.setTimestamp(true);
                        break;
                    case "q":
                        Flags.setQuiet(true);
                        break;
                    case "s":
                        packetsize = Integer.valueOf(args[argsLen + 1]);
                        if(packetsize > Flags.getMaxPacketSize()) {
                            System.out.println(String.format("Packet size cannot exceed %d.", Flags.getMaxPacketSize()));
                            exit(0);
                        }
                        break;
                    case "i":
                        interval = Integer.valueOf(args[argsLen + 1]);
                        break;
                    case "W":
                        timeout = Integer.valueOf(args[argsLen + 1]);
                        break;
		    case "h":
			usage();
			exit(0);
                    case "V":
                        System.out.println(String.format("JPing %s", version));
                        exit(0);
                    default:
                        usage();
                        exit(0);
                        break;

                }
            }
            argsLen++;
        }
        host = args[args.length - 1]; // the destionation of the ping is always the last argument entered
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
	    // check if ip is compatible with version passed in arguments
	    if (addr instanceof Inet6Address) {
	        if (Flags.isIpv4()) {
		    System.out.println("Hostname error: Only IPv4 allowed.");
		    exit(0);		
		}
      	    } else if (addr instanceof Inet4Address) {
	        if (Flags.isIpv6()) {
		    System.out.println("Hostname error: Only IPv6 allowed.");
		    exit(0);		
		}
	    }
	    //check flood mode limitations
	    if (Flags.isFlood()) {
		if (interval < 200) {
			if(!System.getProperty("user.name").equals("root")) {
				System.out.println("You need to be root to set a timeout below 200ms in flood mode.");
				exit(0);			
			}
		}
                floodMode();
            }
            System.out.println(String.format("JPing %s (%s) %d bytes of data", host, addr.getHostAddress(), packetsize));
        } catch (UnknownHostException ex) {
            System.out.println(String.format("JPing: %s: Name or service not found", host));
            exit(0);
        }
        final Thread jPingThread = new Thread(() -> {
            if (Flags.isFlood()) {
                floodMode();
            }
            if (npackets < 0) {
                normalMode();
            }
            else {
                countMode();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().interrupt();
            finish(host);
        }));
        jPingThread.start();
    }

    public static boolean pingHost(String host, int port, int timeout) {
        String timestamp = Flags.isTimestamp() ? String.valueOf(System.currentTimeMillis()) : "";
        try (Socket socket = new Socket()) {
            socket.setSendBufferSize(packetsize);
            socket.setReceiveBufferSize(packetsize + 8); //ICMP header simulation
            long pingStart = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(host, port), timeout);
            long pingEnd = System.currentTimeMillis();
		long time = pingEnd - pingStart;
		if (min == 0) min = time;
		if (max == 0) max = time;
                if (max < time) max = time; //get max ping travel time
                if (min > time) min = time; //get min ping travel time
            if (!Flags.isQuiet() && !Flags.isFlood()) {
                total += time; // sum
                System.out.println(String.format("%s%d bytes received from %s, seq=%d, time=%d ms",
                        !timestamp.isEmpty() ? String.format("[%s]", timestamp) : "",
                        socket.getReceiveBufferSize(),
                        host,
                        transmitted,
                        time)
                );
            }
            socket.close();
            return true;
        } catch (IOException e) {
            if (!Flags.isFlood()) System.out.println(e.getMessage());
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
    public static void normalMode() {
        while (true) {
            try {
                transmitted += 1;
                if (pingHost(host, 80, timeout)) {
                    received += 1;
                } else {
                    lost += 1;
                }
                TimeUnit.MILLISECONDS.sleep(interval);
            } catch (InterruptedException ex) {
            }
        }
    }
    public static void countMode() {
        int i = 0;
        while (i < npackets) {
            try {
                transmitted += 1;
                if (pingHost(host, 80, timeout)) {
                    received += 1;
                } else {
                    lost += 1;
                }
                i++;
                TimeUnit.MILLISECONDS.sleep(interval);
            } catch (InterruptedException ex) {
            }
        }
    }
    public static void floodMode() {
        int i = 0;
        String output = "..";
        while (true) {

            try {
                transmitted += 1;
                if (pingHost(host, 80, timeout)) {
                    received += 1;
                    output = output.replaceFirst(".$", "");
                } else {
                    lost += 1;
                }
                System.out.print(output);
                i++;
                TimeUnit.MILLISECONDS.sleep(interval);
            } catch (InterruptedException ex) {

            }
        }
    }
    public static void usage () {
        System.out.println("Usage: [-h] [-q] [-D] [-V] [-s packetsize] [-c count] [-i interval] [-t timeout] destination.\n" +
                "Options:\n" +
                "-c count:\tStop after sending count packets.\n" +
                "-i interval:\tWait interval seconds between sending each packet. The default is to wait for one second between each packet.\n" +
                "-W timeout:\tTime to wait for a response, in milliseconds.\n" +
                "-q:\tQuiet output. Nothing is displayed except the summary lines at startup time and when finished.\n" +
                "-f:\tFlood mode: continously ping host, only shows failed pings as dots\n" +
		"-h:\tShow help.\n" +
		"-V:\tShow version.\n" +
                "-D:\tPrint timestamp (milliseconds) before each line.\n" +
                "-s packetsize:\tSpecifies the number of data bytes to be sent. The default is 56, which translates into 64 ICMP data bytes when combined with the 8 bytes of ICMP header data.\n");
    }
    public static void finish (String host) {
        long stopTime = System.currentTimeMillis();
        System.out.println(String.format("\n--- %s statistics ---", host));
        System.out.println(String.format("%d packets transmitted, %d received, %d percent lost, time %d ms",
                transmitted, received, ((transmitted - received) * 100) / transmitted, stopTime - startTime));
        System.out.println(String.format("min/max/avg time in ms: %.0f/%.0f/%.0f", min, max, total / transmitted));
    }
    public static void exit (int STATUS_CODE) {
        System.exit(STATUS_CODE);
    }
}
