package nsu.networks;

import nsu.networks.exceptions.DangerousProxyException;
import nsu.networks.exceptions.InputException;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.print("To start SOCKS5 proxy enter server port: ");
        int port = Integer.parseInt(in.next());

        in.close();

        try {
            Socks5Proxy socks5Proxy = Socks5Proxy.create(port);
            socks5Proxy.start();
        } catch (InputException | DangerousProxyException e) {
            System.err.println(e.getMessage());
        }
    }
}