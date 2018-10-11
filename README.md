JPing
=======
ping command re-implemented in java

A simple implementation of the linux ping command in java


## Usage: 
java JPing [-h] [-q] [-D] [-V] [-s packetsize] [-c count] [-i interval] [-t timeout] destination. 

## Options: <br>
-c count: Stop after sending count packets.<br>
-i interval: Wait interval seconds between sending each packet. The default is to wait for one second between each packet. <br>
-W timeout: Time to wait for a response, in milliseconds. <br>
-q: Quiet output. Nothing is displayed except the summary lines at startup time and when finished. <br>
-f: Flood mode: continously ping host, only shows failed pings as dots <br>
-h: Show help. <br>
-V: Show version. <br>
-D: Print timestamp (milliseconds) before each line. <br>
-s packetsize: Specifies the number of data bytes to be sent. The default is 56, which translates into 64 ICMP data bytes when combined with the 8 bytes of ICMP header data.


