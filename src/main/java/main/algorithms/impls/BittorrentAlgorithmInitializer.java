package main.algorithms.impls;

import main.TorrentInfo;
import main.algorithms.*;
import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.algorithms.impls.v1.download.PeersToPiecesMapperImpl;
import main.algorithms.impls.v1.download.PieceDownloaderImpl;
import main.algorithms.impls.v1.download.PiecesDownloaderImpl;
import main.algorithms.impls.v1.notification.NotifyAboutCompletedPieceAlgorithmImpl;
import main.algorithms.impls.v1.upload.UploadAlgorithmImpl;
import main.allocator.AllocatorStore;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.algorithms.IncomingPeerMessagesNotifier;
import main.peer.peerMessages.RequestMessage;
import redux.store.Store;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

import java.util.AbstractMap;

public class BittorrentAlgorithmInitializer {
    public static BittorrentAlgorithm v1(AllocatorStore allocatorStore,
                                         TorrentInfo torrentInfo,
                                         Store<TorrentStatusState, TorrentStatusAction> store,
                                         FileSystemLink fileSystemLink,
                                         IncomingPeerMessagesNotifier incomingPeerMessagesNotifier,
                                         Flux<Link> peersCommunicatorFlux,
                                         String identifier) {
        Flux<Link> recordedPeerFlux = peersCommunicatorFlux.replay().autoConnect();

        NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm =
                new NotifyAboutCompletedPieceAlgorithmImpl(torrentInfo,
                        store,
                        fileSystemLink,
                        recordedPeerFlux);

        UploadAlgorithm uploadAlgorithm = new UploadAlgorithmImpl(torrentInfo,
                store,
                fileSystemLink,
                incomingPeerMessagesNotifier.getIncomingPeerMessages$()
                        .filter(tuple2 -> tuple2.getValue() instanceof RequestMessage)
                        .map(tuple2 -> new AbstractMap.SimpleEntry<>(tuple2.getKey(), (RequestMessage) (tuple2.getValue()))));

        PeersToPiecesMapper peersToPiecesMapper =
                new PeersToPiecesMapperImpl(torrentInfo,
                        fileSystemLink,
                        incomingPeerMessagesNotifier,
                        recordedPeerFlux,
                        fileSystemLink.getUpdatedPiecesStatus());

        BlockDownloader blockDownloader = new BlockDownloaderImpl(torrentInfo, fileSystemLink, identifier);

        PieceDownloader pieceDownloader = new PieceDownloaderImpl(allocatorStore, torrentInfo, fileSystemLink, blockDownloader);

        PiecesDownloader piecesDownloader = new PiecesDownloaderImpl(allocatorStore,
                torrentInfo, store, fileSystemLink, peersToPiecesMapper, pieceDownloader);

        DownloadAlgorithm downloadAlgorithm = new DownloadAlgorithm(piecesDownloader, blockDownloader, peersToPiecesMapper);

        return new BittorrentAlgorithm(uploadAlgorithm, downloadAlgorithm, notifyAboutCompletedPieceAlgorithm);
    }
}
