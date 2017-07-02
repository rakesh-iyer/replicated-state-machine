import java.util.*;

class CmdListRequestCommand extends Command {
    long startTimeStamp;

    CmdListRequestCommand() {
        setType("CMDLISTREQUEST_COMMAND");
    }

    long getStartTimeStamp() {
        return startTimeStamp;
    }

    void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public String toString() {
        return super.toString() + " startTimeStamp - " + startTimeStamp;
    }
}
