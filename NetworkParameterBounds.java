class NetworkParameterBounds {
    long initialDelay = 1; // user to server latency.
    long linkDelay = 1; // each communication link.
    long broadcastDelay = 3; // should be a factor of link delay.
    long responseLatency = 10; // anything beyond this is a failed command.
    long voteTime = 5; // Factor of link and broadcast delay.

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
    
    
