import java.util.*;

class CmdListCommand extends Command {
    long startTimeStamp;
    long endTimeStamp;
    List<Command> cmdList;

    CmdListCommand() {
        setType("CMDLIST_COMMAND");
    }

    long getStartTimeStamp() {
        return startTimeStamp;
    }

    long getEndTimeStamp() {
        return endTimeStamp;
    }

    void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    void setEndTimeStamp(long endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    List<Command> getCmdList() {
        return cmdList;
    }

    void setCmdList(List<Command> cmdList) {
        this.cmdList = cmdList;
    }

    public String toString() {
        return super.toString() + " startTimeStamp - " + startTimeStamp + " endTimeStamp - " + endTimeStamp;
    }
}
