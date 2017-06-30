import java.util.*;
import java.util.concurrent.*;

class ReplicatingServer implements Runnable {
    BlockingQueue<Message> commandQueue = new LinkedBlockingQueue<Message>();
    NetworkParameterBounds bounds = new NetworkParameterBounds();
    MessageReceiver messageReceiver;
    List<Command> receivedCommands = new ArrayList<>();
    List<Command> acceptedCommands = new ArrayList<>();
    List<Command> executedCommands = new ArrayList<>();
    boolean stopThread;
    int port;
    int [] peerPorts;
    Map<Integer, Long> voteMap = new HashMap<>();
    boolean doRecovery;

    ReplicatingServer(int port, int[] peerPorts, boolean doRecovery) {
        this.port = port;
        this.peerPorts = peerPorts;
        this.doRecovery = doRecovery;

        // initialize the vote map.
        for (int peerPort : peerPorts) {
            voteMap.put(peerPort, 0L);
        }

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

    // broadcast for this topology is just to send to peers.
    void broadcast(Message m) {
        sendToPeers(m);
    }

    void sendToPeers(Message m) {
        m.setSenderPort(this.port);
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    void sendVoteCommand() {
        VoteCommand vc = new VoteCommand();

        vc.setId(UUID.randomUUID().toString());
        vc.setTimeStamp(getCurrentTimeStamp() - 2 * bounds.getLinkDelay());
        broadcast(vc);
    }

    void process() {
        try {
            while (!stopThread) {
                Command c = (Command)commandQueue.take();

                if (c.getType().equals("START_COMMAND")) {
                    StartCommand sc = (StartCommand)c;

                    if (sc.getTimeStamp() + bounds.getLinkDelay() < getCurrentTimeStamp()) {
                        // fail this command. send back response.
                        System.out.println("Command " + sc + " failed");
                        continue;
                    }

                    receivedCommands.add(sc);

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

                    acceptedCommands.add(ac);
                    broadcast(ac);
                } else if (c.getType().equals("ACCEPT_COMMAND")) {
                    AcceptCommand ac = (AcceptCommand)c;

                    // add command to accepted commands list.
                    acceptedCommands.add(ac);
                } else if (c.getType().equals("VOTE_COMMAND")) {
                    // update vote map.
                    VoteCommand vc = (VoteCommand)c;

                    int port = vc.getSenderPort();
                    long timeStamp = vc.getTimeStamp();

                    voteMap.put(port, timeStamp);

                    // find the latest timestamp for which commands are accepted and execute all commands until that.
                } else if (c.getType().equals("CMDLIST_COMMAND")) {
                    // detect if there is a gap in the intervals of knowledge and ask for the commands in between.
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
