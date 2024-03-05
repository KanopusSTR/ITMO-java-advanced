package info.kgeorgiy.ja.rynk.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket ds;
    private ExecutorService receiverService, senderService;

    @Override
    public void start(int port, int threads) {
        try {
            ds = new DatagramSocket(port);
            receiverService = Executors.newFixedThreadPool(threads);
            senderService = Executors.newFixedThreadPool(threads);
            receiverService.submit(() -> {
                while (!Thread.interrupted()) {
                    try {
                        int size = ds.getReceiveBufferSize();
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[size], size);
                        ds.receive(datagramPacket);

                        senderService.submit(() -> {
                            try {
                                String str = new String(
                                        datagramPacket.getData(), datagramPacket.getOffset(),
                                        datagramPacket.getLength(), StandardCharsets.UTF_8
                                );
                                str = "Hello, " + str;
                                DatagramPacket answerPocket = new DatagramPacket(
                                        str.getBytes(), str.length(), datagramPacket.getSocketAddress());
                                ds.send(answerPocket);
                            } catch (IOException e) {
                                System.out.println("Error while sending with DatagramSocket: " + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Error while receive from DatagramSocket: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.out.println("Error while creating DatagramSocket: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        ds.close();
        receiverService.shutdownNow();
        senderService.shutdownNow();
        receiverService.close();
        senderService.close();
    }

    public static void main(String[] args) {
        MainProc.serverMain(args, HelloUDPServer::new);
    }
}
