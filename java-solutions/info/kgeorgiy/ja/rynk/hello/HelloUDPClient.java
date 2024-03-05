package info.kgeorgiy.ja.rynk.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient implements HelloClient {

    public static final int SO_TIMEOUT = 100;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (ExecutorService senderService = Executors.newFixedThreadPool(threads)) {
            SocketAddress address = new InetSocketAddress(host, port);

            for (int thread = 1; thread < threads + 1; thread++) {
                final int threadNumber = thread;
                senderService.submit(() -> {
                    try (DatagramSocket ds = new DatagramSocket()) {
                        ds.setSoTimeout(SO_TIMEOUT);

                        DatagramPacket datagramPacket = new DatagramPacket(
                                new byte[ds.getReceiveBufferSize()], ds.getReceiveBufferSize());

                        for (int request = 1; request < requests + 1; ++request) {
                            String answerString = prefix + threadNumber + "_" + request;
                            DatagramPacket dp = new DatagramPacket(
                                    answerString.getBytes(StandardCharsets.UTF_8), answerString.length(), address);

                            while (!ds.isClosed()) {
                                try {
                                    ds.send(dp);
                                    ds.receive(datagramPacket);
                                    String answer = new String(
                                            datagramPacket.getData(), datagramPacket.getOffset(),
                                            datagramPacket.getLength(), StandardCharsets.UTF_8
                                    );
                                    if (answer.equals("Hello, " + answerString)) {
                                        System.out.println("Good: " + answerString + " ---> " + answer);
                                        break;
                                    } else {
                                        System.out.println("Bad: " + answerString + " ---> " + answer);
                                    }
                                } catch (IOException e) {
                                    System.err.println("Error while sending with DatagramSocket: " + e.getMessage());
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error while creating DatagramSocket: " + e.getMessage());
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Bad threads count or host name: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new MainProc().clientMain(args);
    }
}
