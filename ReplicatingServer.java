import java.util.*;
import java.util.concurrent.*;

class ReplicatingServer implements Runnable {
    BlockingQueue<Message> commandQueue = new LinkedBlockingQueue<Message>();
    NetworkParameterBounds bounds = new NetworkParameterBounds();
    MessageReceiver messageReceiver;
    List<Command> acceptedCommands = new ArrayList<>();
    boolean stopThread;
    int port;
    int [] peerPorts;
    boolean doRecovery;

    ReplicatingServer(int port, int[] peerPorts, boolean doRecovery) {
        this.port = port;
        this.peerPorts = peerPorts;
        this.doRecovery = doRecovery;

        messageReceiver = new MessageReceiver(commandQueue, port);
        new Thread(messageReceiver).start();
    }

    void addCommand() {
        StartCommand sc = new StartCommand();

        sc.setId(UUID.randomUUID().toString());
        sc.setTimeStamp(getCurrentTimeStamp());
        sc.setData("Data");
        try {
            MessageSender.send(sc, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long getCurrentTimeStamp() {
        return System.currentTimeMillis();
    }

    void sendToPeers(Message m) {
        m.setSenderPort(this.port);
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    void process() {
        try {
            while (!stopThread) {
                Command c = (Command)commandQueue.take();

                if (c.getType().equals("START_COMMAND")) {
                    StartCommand sc = (StartCommand)c;

                    if (sc.getTimeStamp() + bounds.getLinkDelay() < getCurrentTimeStamp()) {
                        // fail this command.
                        continue;
                    }

                    ReplicaCommand rc = new ReplicaCommand();

                    sc.copy(rc);

                    // send replica commands to peers.
                    sendToPeers(rc);
                } else if (c.getType().equals("REPLICA_COMMAND")) {
                    ReplicaCommand rc = (ReplicaCommand)c;

                    if (rc.getTimeStamp() + 2 * bounds.getLinkDelay() < getCurrentTimeStamp()) {
                        // fail this command.
                        continue;
                    }

                    // accept command.
                    // broadcast command accepted to all.
                    AcceptCommand ac = new AcceptCommand();

                    rc.copy(ac);
                    sendToPeers(ac);
                } else if (c.getType().equals("ACCEPT_COMMAND")) {
                    AcceptCommand ac = (AcceptCommand)c;

                    // add command to accepted commands list.
                    acceptedCommands.add(ac);
                } else if (c.getType().equals("VOTE_COMMAND")) {
                    // use for protocol.
                } else {
                    System.out.println("Unknown Command " + c);
                }
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        process();
    }
}
