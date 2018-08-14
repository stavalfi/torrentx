package main.peer;

import main.App;
import main.TorrentInfo;
import main.file.system.allocator.AllocatorStore;
import main.peer.peerMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.DataOutputStream;
import java.util.BitSet;

class SendMessagesNotificationsImpl implements SendMessagesNotifications {
    private static Logger logger = LoggerFactory.getLogger(SendMessagesNotificationsImpl.class);

    private TorrentInfo torrentInfo;
    private Peer me;
    private Peer peer;
    private Flux<PeerMessage> sentPeerMessagesFlux;
    private FluxSink<PeerMessage> sentMessagesFluxSink;
    private PeerCurrentStatus peerCurrentStatus;
    private SendMessages sendMessages;
    private AllocatorStore allocatorStore;
    private String identifier;

    SendMessagesNotificationsImpl(AllocatorStore allocatorStore,
                                  TorrentInfo torrentInfo,
                                  Peer me, Peer peer,
                                  PeerCurrentStatus peerCurrentStatus,
                                  Runnable closeConnectionMethod,
                                  DataOutputStream peerDataOutputStream,
                                  String identifier) {
        this.allocatorStore = allocatorStore;
        this.torrentInfo = torrentInfo;
        this.me = me;
        this.peer = peer;
        this.peerCurrentStatus = peerCurrentStatus;
        this.identifier = identifier;
        this.sentPeerMessagesFlux = Flux.create((FluxSink<PeerMessage> sink) -> this.sentMessagesFluxSink = sink)
                .subscribeOn(App.MyScheduler)
                .publish()
                .autoConnect(0);
        this.sendMessages = new SendMessages(peerDataOutputStream, closeConnectionMethod);
    }

    private Mono<SendMessagesNotifications> send(PeerMessage peerMessage) {
        return peerMessage.sendMessage(this.sendMessages)
                .doOnNext(__ -> logger.debug(this.identifier + " - sent message to peer: " + peerMessage))
                .map(sendMessages -> (SendMessagesNotifications) this)
                .doOnNext(peersCommunicator -> {
                    if (this.sentMessagesFluxSink != null)
                        this.sentMessagesFluxSink.next(peerMessage);
                });
    }

    // TODO: remove this getter. this object is for internal use only by this class.
    public SendMessages getSendMessages() {
        return sendMessages;
    }

    @Override
    public Mono<SendMessagesNotifications> sendPieceMessage(PieceMessage pieceMessage) {
        return send(pieceMessage)
                .map(__ -> this.allocatorStore)
                .doOnSuccessOrError((__, ___) -> logger.debug(this.identifier + " - dispatching cleaning for piece-message: " + pieceMessage))
                .doOnError(__ -> logger.debug(this.identifier + " - I shouldn't be here: " + __))
                .doAfterSuccessOrError((__, ___) -> this.allocatorStore.freeNonBlocking(pieceMessage.getAllocatedBlock()))
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(pieceMessage.getIndex()))
                .map(__ -> this);
    }

    @Override
    public Mono<SendMessagesNotifications> sendBitFieldMessage(BitSet completedPieces) {
        return send(new BitFieldMessage(this.getMe(), this.getPeer(), completedPieces))
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(completedPieces));
    }

    @Override
    public Mono<SendMessagesNotifications> sendCancelMessage(int index, int begin, int blockLength) {
        return send(new CancelMessage(this.getMe(), this.getPeer(), index, begin, blockLength));
    }

    @Override
    public Mono<SendMessagesNotifications> sendChokeMessage() {
        return send(new ChokeMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIChokingHim(true));
    }

    @Override
    public Mono<SendMessagesNotifications> sendHaveMessage(int pieceIndex) {
        return send(new HaveMessage(this.getMe(), this.getPeer(), pieceIndex))
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(pieceIndex));
    }

    @Override
    public Mono<SendMessagesNotifications> sendInterestedMessage() {
        return send(new InterestedMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIInterestedInHim(true));
    }

    @Override
    public Mono<SendMessagesNotifications> sendKeepAliveMessage() {
        return send(new KeepAliveMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendMessagesNotifications> sendNotInterestedMessage() {
        return send(new NotInterestedMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIInterestedInHim(false));
    }

    @Override
    public Mono<SendMessagesNotifications> sendPortMessage(short listenPort) {
        return send(new PortMessage(this.getMe(), this.getPeer(), listenPort));
    }

    @Override
    public Mono<SendMessagesNotifications> sendRequestMessage(int index, int begin, int blockLength) {
        int pieceLength = this.torrentInfo.getPieceLength(index);
        return this.allocatorStore.createRequestMessage(this.getMe(), this.getPeer(), index, begin, blockLength, pieceLength)
                .flatMap(this::send);
    }

    @Override
    public Mono<SendMessagesNotifications> sendUnchokeMessage() {
        return send(new UnchokeMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIChokingHim(false));
    }

    @Override
    public Flux<PeerMessage> sentPeerMessagesFlux() {
        return this.sentPeerMessagesFlux;
    }

    private Peer getPeer() {
        return peer;
    }

    private Peer getMe() {
        return me;
    }
}
