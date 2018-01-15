package com.steps;

import christophedetroyer.torrent.TorrentParser;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.peer.*;
import main.tracker.*;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import main.tracker.response.TrackerResponse;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.Mockito.mock;

public class MyStepdefs {

    // what is the torrent we are talking about
    private TorrentInfo torrent;

    // we will test this sequence
    private Mono<? extends TrackerResponse> trackerResponseMono;

    @Given("^new torrent file: \"([^\"]*)\".$")
    public void newTorrentFile(String torrentFilePath) throws Throwable {
        String torrentFilesLocation = "src/test/resources/";
        this.torrent = new TorrentInfo(TorrentParser.parseTorrent(torrentFilesLocation + torrentFilePath));
    }

    @Then("^change the torrent-info-hash to a valid but not exist hash.$")
    public void changeTheTorrentInfoHashToAValidButNotExistHash() throws Throwable {
        String fakeTorrentHashInfo = "0123456789012345678901234567890123456789"; // 40 hex numbers
        List<Tracker> originalTrackers = this.torrent.getTrackerList();
        // define our mock object
        this.torrent = mock(TorrentInfo.class);
        Mockito.when(this.torrent.getTorrentInfoHash()).thenReturn(fakeTorrentHashInfo);
        Mockito.when(this.torrent.getTrackerList()).thenReturn(originalTrackers);
    }

    @Given("^extra not-responding trackers to the tracker-list.$")
    public void extraNotRespondingTrackersToTheTrackerList() throws Throwable {
        String torrentHashInfo = this.torrent.getTorrentInfoHash();
        this.torrent.getTrackerList().stream().findFirst().ifPresent(tracker -> {
            List<Tracker> fakeTrackers = Arrays.asList(
                    new Tracker("wrongUrl.com", 8090), // wrong url (but valid url) and a random port
                    new Tracker(tracker.getTracker(), tracker.getPort() + 1) // wrong port
            );
            List<Tracker> trackers = new LinkedList<>();
            trackers.addAll(fakeTrackers);
            trackers.addAll(this.torrent.getTrackerList());
            trackers.addAll(fakeTrackers);
            // define our mock object
            this.torrent = mock(TorrentInfo.class);
            Mockito.when(this.torrent.getTorrentInfoHash()).thenReturn(torrentHashInfo);
            Mockito.when(this.torrent.getTrackerList()).thenReturn(trackers);
        });
    }

    @Given("^invalid url of a tracker.$")
    public void invalidUrlOfATracker() throws Throwable {
        String torrentHashInfo = this.torrent.getTorrentInfoHash();
        this.torrent = mock(TorrentInfo.class);
        Mockito.when(this.torrent.getTorrentInfoHash()).thenReturn(torrentHashInfo);
        Mockito.when(this.torrent.getTrackerList()).thenReturn(Collections.singletonList(new Tracker("invalid.url.123", 123)));
    }

    @Then("^application send signal \"([^\"]*)\" to tracker.$")
    public void applicationSendSignal(RequestSignalType requestToTracker) throws Throwable {
        Map<RequestSignalType, Function<Tracker, Mono<? extends TrackerResponse>>> getResponseByRequestType =
                new HashMap<RequestSignalType, Function<Tracker, Mono<? extends TrackerResponse>>>() {{
                    put(RequestSignalType.Connect,
                            (Tracker tracker) -> ConnectToTracker.connect(tracker.getTracker(), tracker.getPort()));
                    put(RequestSignalType.Announce,
                            (Tracker tracker) -> ConnectToTracker.connect(tracker.getTracker(), tracker.getPort())
                                    .flatMap(connectResponse ->
                                            AnnounceToTracker.announce(connectResponse, torrent.getTorrentInfoHash())));
                    put(RequestSignalType.Scrape,
                            (Tracker tracker) -> ConnectToTracker.connect(tracker.getTracker(), tracker.getPort())
                                    .flatMap(connectResponse ->
                                            ScrapeToTracker.scrape(connectResponse, Collections.singletonList(torrent.getTorrentInfoHash()))));
                }};

        // if I get an errorSignal signal containing one of those errors,
        // then I will communicate with the next tracker in the tracker-list.
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        this.trackerResponseMono = Flux.fromStream(this.torrent.getTrackerList().stream())
                // given a tracker, communicate with him and get the signal containing the response.
                .flatMap(getResponseByRequestType.get(requestToTracker).andThen(getResponseMono ->
                        // if an errorSignal signal from the following list occurs, ignore it.
                        getResponseMono.onErrorResume(communicationErrorsToIgnore, error -> Mono.empty())))
                .take(1)
                // it will send errorSignal signal if the
                // sequence have more then one element
                // so I used take(1) to ensure that.
                .single();
    }

    @Then("^application receive signal \"([^\"]*)\" from tracker.$")
    public void applicationReceiveSignal(ResponseSignalType responseSignalType) throws Throwable {
        StepVerifier.create(this.trackerResponseMono)
                .expectNextMatches((TrackerResponse response) ->
                        response.getClass() == responseSignalType.getSignal())
                .expectComplete()
                .verify();
    }

    @Then("^application receive error signal \"([^\"]*)\" from tracker.$")
    public void applicationReceiveErrorSignalFromTracker(ErrorSignalType errorSignalType) throws Throwable {
        StepVerifier.create(this.trackerResponseMono)
                .expectError(errorSignalType.getErrorSignal())
                .verify();
    }

    @Then("^change our peer-id to an unregistered peer-id.$")
    public void changeOurPeerIdToAnUnregisteredPeerId() throws Throwable {

    }

    @Then("^change the torrent-info-hash to a invalid torrent-info-hash.$")
    public void changeTheTorrentInfoHashToAInvalidTorrentInfoHash() throws Throwable {
    }

    @Then("^application send communication request to peer.$")
    public void applicationSendCommunicationRequestToPeer() throws Throwable {
    }

    @Then("^application receive communication response from peer: \"([^\"]*)\".$")
    public void applicationReceiveCommunicationResponseFromPeer(String peersConnectionStatus) throws Throwable {
    }


    private enum PeerMessageType {
        BitFieldMessage(BitFieldMessage.class),
        CancelMessage(CancelMessage.class),
        ChokeMessage(ChokeMessage.class),
        HaveMessage(HaveMessage.class),
        InterestedMessage(InterestedMessage.class),
        IsAliveMessage(IsAliveMessage.class),
        NotInterestedMessage(NotInterestedMessage.class),
        PieceMessage(PieceMessage.class),
        PortMessage(PortMessage.class),
        RequestMessage(RequestMessage.class),
        UnchokeMessage(UnchokeMessage.class);

        final Class<? extends PeerMessage> signal;

        PeerMessageType(Class<? extends PeerMessage> signal) {
            this.signal = signal;
        }

        public Class<? extends PeerMessage> getSignal() {
            return this.signal;
        }
    }

    private enum RequestSignalType {
        Connect,
        Announce,
        Scrape
    }

    private enum ResponseSignalType {
        Connect(ConnectResponse.class),
        Announce(AnnounceResponse.class),
        Scrape(ScrapeResponse.class);
        final Class<? extends TrackerResponse> signal;

        ResponseSignalType(Class<? extends TrackerResponse> signal) {
            this.signal = signal;
        }

        public Class<? extends TrackerResponse> getSignal() {
            return this.signal;
        }
    }

    private enum ErrorSignalType {
        IOException(java.io.IOException.class),
        UnknownHostException(java.net.UnknownHostException.class),
        SecurityException(java.lang.SecurityException.class),
        BadResponseException(main.tracker.BadResponseException.class);

        final Class<? extends Throwable> errorSignal;

        ErrorSignalType(Class<? extends Throwable> errorSignal) {
            this.errorSignal = errorSignal;
        }

        public Class<? extends Throwable> getErrorSignal() {
            return this.errorSignal;
        }
    }
}

