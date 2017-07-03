// a lot of these delays are higher due to intra-process latencies through the communication network of the simulation.
class NetworkParameterBounds {
    long initialDelay = 100; // user to server latency.
    long linkDelay = 100; // each communication link.
    long broadcastDelay = 300; // should be a factor of link delay.
    long responseLatency = 1000; // anything beyond this is a failed command.
    long voteTime = 500; // Factor of link and broadcast delay.

    long getInitialDelay() {
        return initialDelay;
    }

    long getLinkDelay() {
        return linkDelay;
    }

    long getBroadcastDelay() {
        return broadcastDelay;
    }

    long getResponseLatency() {
        return responseLatency;
    }

    long getVoteTime() {
        return voteTime;
    }
}
    
    
