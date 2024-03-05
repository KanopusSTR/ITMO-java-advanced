package info.kgeorgiy.ja.rynk.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Scanner;
import java.util.function.Supplier;

public class MainProc {

    public static void clientMain(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("You must write correct arguments: hostName portNumber prefix threadsCount requestsCount");
        } else {
            String hostName = args[0];
            int portNumber = processingArgs(args, 2, 8080);
            String prefix = args.length >= 3 ? args[2] : "";
            int threadsCount = processingArgs(args, 4, 1);
            int requestCount = processingArgs(args, 5, 1);
            HelloUDPClient client = new HelloUDPClient();
            client.run(hostName, portNumber, prefix, threadsCount, requestCount);
        }
    }

    public static void serverMain(String[] args, Supplier<HelloServer> sup) {
        if (args == null || args.length == 0) {
            System.out.println("You must write correct arguments: portNumber threadsCount");
        } else {
            int portNumber = processingArgs(args, 1, 8080);
            int threadsCount = processingArgs(args, 2, 1);
            try (HelloServer server = sup.get()) {
                server.start(portNumber, threadsCount);
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNext()) {
                    scanner.next();
                }
            }
        }
    }

    private static int processingArgs(String[] args, int argNumber, int defaultValue) {
        try {
            return args.length >= argNumber ? Integer.parseInt(args[argNumber - 1]) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue; // :NOTE: не затирать исключение
        }
    }

}
