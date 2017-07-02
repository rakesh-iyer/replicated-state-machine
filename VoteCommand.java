import java.util.*;

class VoteCommand extends Command {
    long voteTimeStamp;

    long getVoteTimeStamp() {
        return voteTimeStamp;
    }

    void setVoteTimeStamp(long voteTimeStamp) {
        this.voteTimeStamp = voteTimeStamp;
    }

    VoteCommand() {
        setType("VOTE_COMMAND");
    }

    public String toString() {
        return super.toString() + " voteTimeStamp - " + voteTimeStamp;
    }
}
