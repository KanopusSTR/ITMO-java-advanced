package info.kgeorgiy.ja.rynk.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class HelloUDPNonblockingClient implements HelloClient {

    public static final int TIMEOUT = 50;

    @SuppressWarnings("resource")
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (Selector selector = Selector.open()) {
            SocketAddress address = new InetSocketAddress(host, port);
            for (int thread = 1; thread < threads + 1; thread++) {
                try {
                    DatagramChannel datagramChannel = DatagramChannel.open();
                    Entry entry = new Entry(thread, 1);
                    datagramChannel
                            .configureBlocking(false)
                            .register(selector, SelectionKey.OP_WRITE, entry);
                } catch (IOException e) {
                    System.err.println("Error while creating datagramChannel:" + e.getMessage());
                }
            }
            while (!Thread.currentThread().isInterrupted()) {
                if (selector.select(TIMEOUT) == 0) {
                    for (SelectionKey key : selector.keys()) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
                if (selector.keys().isEmpty()) {
                    break;
                }

                for (final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                    SelectionKey key = iterator.next();
                    DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                    Entry entry = (Entry) key.attachment();
                    int thread = entry.thread();
                    int request = entry.request();
                    if (key.isReadable()) {
                        ByteBuffer buffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
                        datagramChannel.receive(buffer);
                        buffer.flip();
                        String receivedString = new String(buffer.array(), buffer.arrayOffset(), buffer.limit(), StandardCharsets.UTF_8);
                        String answerString = prefix + thread + "_" + request;
                        if (receivedString.equals("Hello, " + answerString)) {
                            key.attach(new Entry(thread, request + 1));
                            if (request == requests) {
                                key.cancel();
                                datagramChannel.close();
                            } else {
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                    } else if (key.isWritable()) {
                        String answerString = prefix + thread + "_" + request;
                        datagramChannel.send(ByteBuffer.wrap(answerString.getBytes(StandardCharsets.UTF_8)), address);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("Error while working with datagramChannel:" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        MainProc.clientMain(args);
    }

    private record Entry(int thread, int request) {
    }
}
