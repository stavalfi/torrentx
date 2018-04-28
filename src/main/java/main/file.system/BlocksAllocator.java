package main.file.system;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.BitSet;

public interface BlocksAllocator {

    Mono<AllocatedBlock> allocate();

    void free(AllocatedBlock allocatedBlock);

    // I can't send the actual AllocatedBlock
    // here because someone will maybe save
    // it but it's not safe because it's content
    // will be changed in the future.
    Flux<Integer> allocated$();

    Flux<Integer> frees$();

    BitSet getFreeBlocksStatus();

    int getBlockLength();

    int getAmountOfBlocks();
}
