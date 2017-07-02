import java.util.concurrent.*;

class Command extends Message {
    String id;
    long timeStamp;

    void copy(Command c) {
        c.setId(id);
        c.setTimeStamp(timeStamp);
        c.setData(data);
    }

    String getId() {
        return id;
    }

    long getTimeStamp() {
        return timeStamp;
    }

    void setId(String id) {
        this.id = id; 
    }

    void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String toString() {
        return super.toString() + " id:" + id + " timeStamp:" + timeStamp;
    }
}
