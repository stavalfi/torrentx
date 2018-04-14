package main;

import christophedetroyer.torrent.TorrentParser;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.peer.Link;
import main.peer.ReceivePeerMessages;
import main.peer.SendPeerMessages;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

public class App {
    public static Scheduler MyScheduler = Schedulers.elastic();
    private static String downloadPath = System.getProperty("user.dir") + "/" + "torrents-test/";

    private static void f4() throws IOException {
        TorrentDownloader torrentDownloader = TorrentDownloaders
                .createDefaultTorrentDownloader(getTorrentInfo(), downloadPath);

        torrentDownloader.getPeersCommunicatorFlux()
                .map(Link::sendMessages)
                .flatMap(SendPeerMessages::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .subscribe(peerMessage -> System.out.println("sent: " + peerMessage), Throwable::printStackTrace);

        torrentDownloader.getPeersCommunicatorFlux()
                .map(Link::receivePeerMessages)
                .flatMap(ReceivePeerMessages::getPieceMessageResponseFlux)
                .subscribe(System.out::println, Throwable::printStackTrace);

        torrentDownloader.getTorrentStatusController().startDownload();
        torrentDownloader.getTorrentStatusController().startUpload();
    }

    public static void main(String[] args) throws Exception {
        Hooks.onOperatorDebug();
        f4();
        Thread.sleep(1000 * 1000);
    }

    private static TorrentInfo getTorrentInfo() throws IOException {
        String torrentFilePath = "src/main/resources/torrents/tor.torrent";
        return new TorrentInfo(torrentFilePath, TorrentParser.parseTorrent(torrentFilePath));
    }
}