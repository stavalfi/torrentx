package main.tracker.response;

import lombok.Getter;
import lombok.ToString;
import main.Peer;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.joou.Unsigned.ushort;

@Getter
@ToString
public class AnnounceResponse extends TrackerResponse {


    private final int action;
    private final int transactionId;
    private final int interval;
    private final int leechersAmount;
    private final int seedersAmount;
    private final List<Peer> peers;

    public Flux<Peer> getPeers() {
        return Flux.fromStream(peers.stream());
    }

    /**
     * Offset      Size            Name            Value
     * 0           32-bit integer  action          1 // scrape
     * 4           32-bit integer  transaction_id
     * 8           32-bit integer  interval
     * 12          32-bit integer  leechersAmount
     * 16          32-bit integer  seedersAmount
     * 20 + 6 * n  32-bit integer  IP address
     * 24 + 6 * n  16-bit integer  TCP port
     * 20 + 6 * N
     */
    public AnnounceResponse(String ip, int port, ByteBuffer receiveData, int maxPeersWeWantToGet) {
        super(ip, port);
        this.action = receiveData.getInt();
        assert this.action == 1;
        this.transactionId = receiveData.getInt();
        this.interval = receiveData.getInt();
        this.leechersAmount = receiveData.getInt();
        this.seedersAmount = receiveData.getInt();

        this.peers = IntStream.range(0, Integer.min(maxPeersWeWantToGet, this.leechersAmount + this.seedersAmount))
                .mapToObj((int index) -> new Peer(castIntegerToInetAddress(receiveData.getInt()).getHostAddress(), ushort(receiveData.getShort())))
                .collect(Collectors.toList());
    }

    public static int packetResponseSize() {
        return 1000;
    }

    private static InetAddress castIntegerToInetAddress(int ip) {
        byte[] bytes = BigInteger.valueOf(ip).toByteArray();
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
}
