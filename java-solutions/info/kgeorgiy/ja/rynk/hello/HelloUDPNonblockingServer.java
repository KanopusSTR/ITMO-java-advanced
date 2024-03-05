package info.kgeorgiy.ja.rynk.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {
    ExecutorService receiverService;
    ExecutorService workerService;
    Selector selector;
    DatagramChannel datagramChannel;

    Queue<Entry> queue = new ConcurrentLinkedDeque<>();

    @Override
    public void start(int port, int threads) {
        receiverService = Executors.newSingleThreadExecutor();
        workerService = Executors.newFixedThreadPool(threads);

        try {
            selector = Selector.open();

            datagramChannel = DatagramChannel.open();
            datagramChannel.bind(new InetSocketAddress(port))
                    .configureBlocking(false)
                    .register(selector, SelectionKey.OP_READ);

            receiverService.submit(() -> {
                while (selector.isOpen() && !Thread.currentThread().isInterrupted()) {
                    try {
                        selector.select();
                        for (final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                            try {
                                SelectionKey key = iterator.next();
                                if (key.isReadable()) {
                                    ByteBuffer buffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
                                    SocketAddress address = datagramChannel.receive(buffer);
                                    workerService.submit(() -> {
                                        buffer.flip();
                                        String receivedString = new String(buffer.array(), buffer.arrayOffset(), buffer.limit(), StandardCharsets.UTF_8);
                                        String sendString = "Hello, " + receivedString;
                                        queue.add(new Entry(sendString, address));
                                        key.interestOpsOr(SelectionKey.OP_WRITE);
                                        selector.wakeup();
                                    });
                                }
                                if (key.isWritable()) {
                                    Entry entry = queue.poll();
                                    if (entry == null) {
                                        key.interestOps(SelectionKey.OP_READ);
                                    } else {
                                        datagramChannel.send(ByteBuffer.wrap(entry.string.getBytes(StandardCharsets.UTF_8)), entry.address);
                                    }
                                }
                            } finally {
                                iterator.remove();
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Can't work with selector or datagramChannel in thread:" + e.getMessage());
                    }

                }
            });
        } catch (IOException e) {
            System.err.println("Can't open selector or datagramChannel");
        }
    }

    @Override
    public void close() {
        // :NOTE: утечка ресурсов (NPE)
        try {
            try {
                if (datagramChannel != null) {
                    datagramChannel.close();
                }
            } catch (IOException e) {
            }
            selector.close();
        } catch (IOException e) {
            System.err.println("can't close selector or datagramChannel");
        }
        receiverService.shutdownNow();
        workerService.shutdownNow();
        receiverService.close();
        workerService.close();
    }


    public static void main(String[] args) {
        MainProc.serverMain(args, HelloUDPNonblockingServer::new);
    }

    private record Entry(String string, SocketAddress address) {
    }
}
