package main.torrent.status.reducers;

import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;
import redux.reducer.IReducer;

import java.util.function.Supplier;

public class TorrentStatusReducer implements IReducer<TorrentStatusState, TorrentStatusAction> {

    public static Supplier<TorrentStatusState> defaultTorrentStateSupplier = () ->
            new TorrentStatusState(TorrentStatusAction.INITIALIZE,
                    DownloadStateReducer.defaultDownloadStateSupplier.get(),
                    PeersStateReducer.defaultPeersStateSupplier.get(),
                    TorrentFileSystemStateReducer.defaultTorrentFileSystemStateSupplier.get());

    private DownloadStateReducer downloadStateReducer = new DownloadStateReducer();
    private PeersStateReducer peersStateReducer = new PeersStateReducer();
    private TorrentFileSystemStateReducer torrentFileSystemStateReducer = new TorrentFileSystemStateReducer();

    public TorrentStatusState reducer(TorrentStatusState lastState, TorrentStatusAction torrentStatusAction) {

        DownloadState downloadState = this.downloadStateReducer.reducer(lastState, torrentStatusAction);
        PeersState peersState = this.peersStateReducer.reducer(lastState, torrentStatusAction);
        TorrentFileSystemState torrentFileSystemState = this.torrentFileSystemStateReducer.reducer(lastState, torrentStatusAction);
        if (lastState.getDownloadState().equals(downloadState) &&
                lastState.getPeersState().equals(peersState) &&
                lastState.getTorrentFileSystemState().equals(torrentFileSystemState))
            return lastState;
        return new TorrentStatusState(torrentStatusAction,
                downloadState,
                peersState,
                torrentFileSystemState);
    }
}