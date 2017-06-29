class NetworkParameterBounds {
    long initialDelay; // user to server latency.
    long linkDelay; // each communication link.
    long broadcastDelay; // should be a factor of link delay.
    long responseLatency; // anything beyond this is a failed command.
    long overlapTime; // You need to detect any time this is violated.

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

    long getOverlapTime() {
        return overlapTime;
    }
}
    
    
