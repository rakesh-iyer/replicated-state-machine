import java.util.concurrent.*;

class Command extends Message implements Delayed {
    String id;
    long timeStamp;

    public int compareTo(Delayed other) {
        long thisDelay = timeStamp - System.currentTimeMillis();
        long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);

        if (thisDelay > otherDelay) {
            return 1;
        } else if (thisDelay < otherDelay) {
            return -1;
        } else {
            return 0;
        }
    }

    public long getDelay(TimeUnit tu) {
        long delay = timeStamp - System.currentTimeMillis();

        return tu.convert(delay, TimeUnit.MILLISECONDS);
    }

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
