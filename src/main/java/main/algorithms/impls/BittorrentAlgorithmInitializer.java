package main.algorithms.impls;

import main.TorrentInfo;
import main.algorithms.*;
import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.algorithms.impls.v1.download.PeersToPiecesMapperImpl;
import main.algorithms.impls.v1.download.PiecesDownloaderImpl;
import main.algorithms.impls.v1.notification.NotifyAboutCompletedPieceAlgorithmImpl;
import main.algorithms.impls.v1.upload.UploadAlgorithmImpl;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.peer.Link;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;
import redux.store.Store;

import java.util.AbstractMap;

public class BittorrentAlgorithmInitializer {
    public static BittorrentAlgorithm v1(AllocatorStore allocatorStore,
                                         TorrentInfo torrentInfo,
                                         Store<TorrentStatusState, TorrentStatusAction> store,
                                         FileSystemLink fileSystemLink,
                                         Flux<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$,
                                         Flux<Link> peersCommunicatorFlux,
                                         String identifier) {
        Flux<Link> recordedPeerFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect();

        NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm =
                new NotifyAboutCompletedPieceAlgorithmImpl(torrentInfo,
                        store,
                        fileSystemLink,
                        recordedPeerFlux);

        UploadAlgorithm uploadAlgorithm = new UploadAlgorithmImpl(torrentInfo,
                store,
                fileSystemLink,
                incomingPeerMessages$.filter(tuple2 -> tuple2.getValue() instanceof RequestMessage)
                        .map(tuple2 -> new AbstractMap.SimpleEntry<>(tuple2.getKey(), (RequestMessage) (tuple2.getValue()))));

        PeersToPiecesMapper peersToPiecesMapper =
                new PeersToPiecesMapperImpl(recordedPeerFlux,
                        fileSystemLink.getUpdatedPiecesStatus());

        BlockDownloader blockDownloader = new BlockDownloaderImpl(torrentInfo, fileSystemLink, identifier);

        PiecesDownloader piecesDownloader = new PiecesDownloaderImpl(allocatorStore,
                torrentInfo, store,
                fileSystemLink, peersToPiecesMapper, blockDownloader);

        DownloadAlgorithm downloadAlgorithm = new DownloadAlgorithm(piecesDownloader, blockDownloader, peersToPiecesMapper);

        return new BittorrentAlgorithm(uploadAlgorithm, downloadAlgorithm, notifyAboutCompletedPieceAlgorithm);
    }
}
