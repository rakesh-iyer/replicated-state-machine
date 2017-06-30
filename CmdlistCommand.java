import java.util.*;

class CmdlistCommand extends Command {
    long startTimeStamp;
    long endTimeStamp;
    List<Command> commandList;

    CmdlistCommand() {
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
} 
