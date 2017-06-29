class Command {
    String id;
    long timeStamp;
    String data;

    String getId() {
        return id;
    }

    long getTimeStamp() {
        return timeStamp;
    }

    String getData() {
        return data;
    }

    void setId(String id) {
        this.id = id; 
    }

    void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    void setData(String data) {
        this.data = data;
    }
}
