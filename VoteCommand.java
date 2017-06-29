import java.util.*;

class VoteCommand extends Command {
    long startTimeStamp;
    long endTimeStamp;
    Map<String, Boolean> votes = new HashMap<>();

    VoteCommand() {
        setType("VOTE_COMMAND");
    }

    long getStartTimeStamp() {
        return startTimeStamp;
    }

    void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    long getEndTimeStamp() {
        return endTimeStamp;
    }

    void setEndTimeStamp(long endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    Map<String, Boolean> getVotes() {
        return votes;
    }
}
