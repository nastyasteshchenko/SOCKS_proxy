**To run the app:**  
mvn compile  
mvn exec:java -Dexec.mainClass="nsu.networks.Main"  
**_______________________________**  

**SOCKS proxy**  
1.  You must implement a proxy server that is SOCKS version 5 compliant.  
2.  In the parameters, the program is passed only the port on which the proxy will listen for incoming connections from clients.  
3.  Of the three commands available in the protocol, only the implementation of command 1 (establish a TCP/IP stream connection) is mandatory.
4.  Support for authentication and IPv6 addresses is not required.  
5.  To implement proxies, use non-blocking sockets, working with them within one thread. Additional threads are not allowed. Accordingly, no blocking calls (other than a selector call) are allowed.  
6.  The proxy should not make assumptions about which application layer protocol will be used within the forwarded TCP connection. In particular, simultaneous data transfer in both directions must be supported, and connections must be closed carefully (only when they are no longer needed).  
7.  The application should not have idle loops in any situation. In other words, it should not be possible for a program state to repeatedly execute a loop body that does not make any actual data transfers during the iteration.  
8.  Unlimited memory consumption is not allowed to serve one client.  
9.  Performance through a proxy should not be noticeably worse than without a proxy. To monitor the correctness and speed of operation, you can look in the browser’s Developer tools on the Network tab.  
10.  The proxy must support domain name resolution (value 0x03 in the address field). Resolving should also be non-blocking. To do this, it is proposed to use the following approach:  
*  At the start of the program, create a new UDP socket and add it to the read selector  
*  When it is necessary to resolve a domain name, send a DNS request for A records through this socket to the address of the recursive DNS resolver  
*  In the socket read handler, handle the case when a response to a DNS request is received and continue working with the received address  
