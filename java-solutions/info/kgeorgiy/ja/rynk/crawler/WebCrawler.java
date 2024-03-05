package info.kgeorgiy.ja.rynk.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    final private Downloader downloader;
    final private ExecutorService downloaderService;
    final private ExecutorService extractorService;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaderService = Executors.newFixedThreadPool(downloaders);
        this.extractorService = Executors.newFixedThreadPool(extractors);
    }


    @Override
    public Result download(String url, int depth) {
        Set<String> links = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> nextLinks = new HashSet<>(); // :NOTE: code style
        nextLinks.add(url);
        Phaser phaser = new Phaser(1);
        for (int i = 0; i < depth; ++i) {
            Set<String> local_links = ConcurrentHashMap.newKeySet();
            for (String link : nextLinks) {
                phaser.register();
                downloaderService.submit(() -> {
                    try {
                        if (links.add(link)) {
                            Document downloaded = downloader.download(link);
                            phaser.register();
                            extractorService.submit(() -> {
                                try {
                                    local_links.addAll(downloaded.extractLinks());
                                } catch (IOException e) {
                                    errors.put(link, e);
                                } finally {
                                    phaser.arriveAndDeregister();
                                }
                            });
                        }
                    } catch (IOException e) {
                        errors.put(link, e);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
            }
            phaser.arriveAndAwaitAdvance();
            nextLinks = local_links;
        }
        links.removeAll(errors.keySet());
        return new Result(new ArrayList<>(links), errors);
    }

    @Override
    public void close() {
        downloaderService.shutdownNow();
        extractorService.shutdownNow();
        downloaderService.close();
        extractorService.close();
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("You must write correct arguments: url [depth [downloads [extractors [perHost]]]]");
        } else {
            int depth = processingArgs(args, 2, 1); // :NOTE: print parseInt message exception
            int downloaders = processingArgs(args, 3, 8);
            int extractors = processingArgs(args, 4, 8);
            int perHost = processingArgs(args, 5, 8);
            try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(1), downloaders, extractors, perHost)) {
                webCrawler.download(args[0], depth); // :NOTE: print result
            } catch(IOException e) {
                e.printStackTrace(); // :NOTE: printStackTrace
            }
        }
    }

    private static int processingArgs(String[] args, int argNumber, int defaultValue) {
        return args.length >=argNumber ? Integer.parseInt(args[argNumber - 1]) : defaultValue;
    }
}
