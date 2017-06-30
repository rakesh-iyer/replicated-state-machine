import java.util.*;

class CmdIdListCommand extends Command {
    long startTimeStamp;
    long endTimeStamp;
    List<String> cmdIdList;

    CmdIdListCommand() {
        setType("CMDIDLIST_COMMAND");
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

    List<String> getCmdIdList() {
        return cmdIdList;
    }

    void setCmdIdList(List<String> cmdIdList) {
        this.cmdIdList = cmdIdList;
    }
}
