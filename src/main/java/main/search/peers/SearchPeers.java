package main.search.peers;

import main.TorrentInfo;
import main.allocator.AllocatorStore;
import main.peer.Link;
import main.peer.algorithms.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import redux.store.Store;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import main.tracker.TrackerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.AbstractMap;

public class SearchPeers {
    private static Logger logger = LoggerFactory.getLogger(SearchPeers.class);

    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<Link> peers$;
    private String identifier;

    public SearchPeers(AllocatorStore allocatorStore, TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store, String identifier,
                       FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this(torrentInfo, store, identifier, new TrackerProvider(torrentInfo),
                new PeersProvider(allocatorStore, torrentInfo, identifier, emitIncomingPeerMessages));
    }

    public SearchPeers(TorrentInfo torrentInfo, Store<TorrentStatusState, TorrentStatusAction> store, String identifier,
                       TrackerProvider trackerProvider, PeersProvider peersProvider) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.identifier = identifier;

        Flux<TorrentStatusState> startSearch$ = store.statesByAction(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_SEARCHING_PEERS_SELF_RESOLVED), 1)
                .publish()
                .autoConnect(0);

        this.peers$ = store.statesByAction(TorrentStatusAction.RESUME_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_SEARCHING_PEERS_SELF_RESOLVED), 1)
                .flatMap(__ -> this.trackerProvider.connectToTrackersFlux()
                        .as(this.peersProvider::connectToPeers$))
                .doOnNext(link -> logger.debug(this.identifier + " - search-peers-module connected to new peer: " + link))
                .concatMap(link -> store.notifyWhen(TorrentStatusAction.RESUME_SEARCHING_PEERS_WIND_UP, link), 1)
                .doOnNext(link -> logger.debug(this.identifier + " - search-peers-module published new peer: " + link))
                .doOnNext(link -> System.out.println("Connected to peer: " + link.getPeer().getPeerIp() + ":" + link.getPeer().getPeerPort()))
                .publish()
                .autoConnect(0);

        Flux<TorrentStatusState> pauseSearch$ = store.statesByAction(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_SELF_RESOLVED), 1)
                .publish()
                .autoConnect(0);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }

    public PeersProvider getPeersProvider() {
        return peersProvider;
    }

    public Flux<Link> getPeers$() {
        return peers$;
    }
}
