package com.utils;

import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.allocator.AllocatorStore;
import main.peer.Link;
import main.peer.algorithms.IncomingPeerMessagesNotifier;
import main.peer.algorithms.SendMessagesNotifications;
import main.peer.peerMessages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class RemoteFakePeer {
    private static Logger logger = LoggerFactory.getLogger(BlockDownloaderImpl.class);
    private Link link;

    public RemoteFakePeer(AllocatorStore allocatorStore, Link link, FakePeerType fakePeerType, String identifier,
                          IncomingPeerMessagesNotifier incomingPeerMessagesNotifier) {
        this.link = link;

        incomingPeerMessagesNotifier.getRequestMessageResponseFlux()
                .doOnNext(requestMessage -> logger.info(identifier + " - received a new request: " + requestMessage))
                .doOnNext(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            link.dispose();
                            logger.info(identifier + " - closed the connection as it should do at the first message he receive.");
                            return;
                        case RESPOND_WITH_DELAY_100:
                            blockThread(100);
                            logger.info(identifier + " - delayed in 100 mill the response. the fake peer did not response yet");
                            return;
                        case RESPOND_WITH_DELAY_3000:
                            blockThread(3 * 1000);
                            logger.info(identifier + " - delayed in 3000 mill the response. the fake peer did not response yet");
                            return;
                        case SEND_RESPOND_AFTER_TIMEOUT:
                            blockThread(2550);
                            logger.info(identifier + " - delayed in 2550 mill the response. the fake peer did not response yet");
                            return;
                    }
                })
                .index()
                .concatMap(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            // its important because the socket may close up-to 4 min so it may be still
                            // active and working so I don't want to send anything by mistake.
                            return Mono.empty();
                        case VALID_AND_SEND_CHOKE_AFTER_2_REQUESTS:
                            if (requestMessage.getT1() == 2)
                                return link.sendMessages()
                                        .sendChokeMessage()
                                        .map(__ -> requestMessage);
                            else if (requestMessage.getT1() > 2)
                                return Mono.empty();
                        default:
                            return Mono.just(requestMessage);
                    }
                },1)
                .map(Tuple2::getT2)
                .concatMap(requestMessage -> {
                    boolean doesFakePeerHaveThePiece = link.getPeerCurrentStatus().doesPeerHavePiece(requestMessage.getIndex());
                    logger.info(identifier + " - does he have the piece: " + doesFakePeerHaveThePiece + " for request: " + requestMessage);
                    if (!doesFakePeerHaveThePiece)
                        return Mono.empty();

                    int pieceLength = link.getTorrentInfo().getPieceLength(requestMessage.getIndex());
                    switch (fakePeerType) {
                        case SEND_LESS_DATA_THEN_REQUESTED:
                            int responseLength = requestMessage.getBlockLength() > 1 ? requestMessage.getBlockLength() - 1 : requestMessage.getBlockLength();
                            return sendPieceMessage(allocatorStore, link, identifier, requestMessage, pieceLength, responseLength);
                        case VALID:
                        case VALID_AND_SEND_CHOKE_AFTER_2_REQUESTS:
                        case RESPOND_WITH_DELAY_100:
                        case RESPOND_WITH_DELAY_3000:
                        case SEND_RESPOND_AFTER_TIMEOUT:
                            return sendPieceMessage(allocatorStore, link, identifier, requestMessage, pieceLength, requestMessage.getBlockLength());
                    }
                    // we will never be here... (by the current fake-peer-types).
                    return Mono.empty();
                },1)
                .publish()
                .autoConnect(0);
    }

    private Mono<SendMessagesNotifications> sendPieceMessage(AllocatorStore allocatorStore, Link link, String identifier, RequestMessage requestMessage, int pieceLength, int responseLength) {
        // create a piece message with random bytes.
        return allocatorStore.createPieceMessage(link.getMe(), link.getPeer(), requestMessage.getIndex(), requestMessage.getBegin(), responseLength, pieceLength)
                .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                        .doOnNext(__ -> logger.info(identifier + " - sent piece message to the app: " + pieceMessage + " for request: " + requestMessage)));
    }

    private void blockThread(int durationInMillis) {
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Link getLink() {
        return link;
    }
}
