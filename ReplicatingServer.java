import java.util.*;
import java.util.concurrent.*;

class ReplicatingServer implements Runnable {
    BlockingQueue<Message> commandQueue = new LinkedBlockingQueue<Message>();
    NetworkParameterBounds bounds = new NetworkParameterBounds();
    MessageReceiver messageReceiver;
    List<Command> receivedCommands = new ArrayList<>();
    List<Command> executedCommands = new ArrayList<>();
    Map<String, Command> acceptedCommands = new HashMap<>();
    long acceptedCmdTimeStamp;
    long executedCmdTimeStamp;
    long sentCmdListTimeStamp;
    long sentVoteTimeStamp;
    boolean stopThread;
    int port;
    int [] peerPorts;
    Map<Integer, Long> voteMap = new HashMap<>();
    Map<Integer, Long> cmdListMap = new HashMap<>();
    ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    boolean doRecovery;

    ReplicatingServer(int port, int[] peerPorts, boolean doRecovery) {
        this.port = port;
        this.peerPorts = peerPorts;
        this.doRecovery = doRecovery;

        // initialize the vote map.
        for (int peerPort : peerPorts) {
            voteMap.put(peerPort, 0L);
            cmdListMap.put(peerPort, 0L);
        }

        messageReceiver = new MessageReceiver(commandQueue, port);
        new Thread(messageReceiver).start();
    }

    long getSentCmdListTimeStamp() {
        return sentCmdListTimeStamp;
    }

    void setSentCmdListTimeStamp(long sentCmdListTimeStamp) {
        this.sentCmdListTimeStamp = sentCmdListTimeStamp;
    }

    long getSentVoteTimeStamp() {
        return sentVoteTimeStamp;
    }

    void setSentVoteTimeStamp(long sentVoteTimeStamp) {
        this.sentVoteTimeStamp = sentVoteTimeStamp;
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

    void sendToHost(Message m, int port) {
        MessageSender.send(m, port);
    }

    void sendToPeers(Message m) {
        m.setSenderPort(this.port);
        for (int peerPort : peerPorts) {
            MessageSender.send(m, peerPort);
        }
    }

    List<Command> getAcceptedCommandList(long startTimeStamp, long endTimeStamp) {
        List<Command> cmdList = new ArrayList<>();

        // no need to optimize on this for now as this will be a rare call.
        // ideally organize accepted command in a sorted ds that can be searched for key or ranges.
        for (Command c : acceptedCommands.values()) {
            if (c.getTimeStamp() < startTimeStamp || c.getTimeStamp() > endTimeStamp) {
                continue;
            }
            cmdList.add(c);
        }

        return cmdList;
    }

    class DelayedVote implements Runnable {
        long timeStamp;

        DelayedVote(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        Command createVoteCommand(long timeStamp) {
            VoteCommand vc = new VoteCommand();

            vc.setId(UUID.randomUUID().toString());
            vc.setVoteTimeStamp(timeStamp);

            return vc;
        }

        void sendVote() {
            Command vc = createVoteCommand(timeStamp);

            broadcast(vc);
        }

        public void run() {
            sendVote();
        }
    }

    class DelayedCommandList implements Runnable {
        long timeStamp;

        DelayedCommandList(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        Command createCmdListCommand(long endTimeStamp) {
            CmdListCommand clc = new CmdListCommand();

            long startTimeStamp = getSentCmdListTimeStamp() + 1;

            List<Command> cmdList = getAcceptedCommandList(startTimeStamp, endTimeStamp);

            clc.setStartTimeStamp(startTimeStamp);
            clc.setEndTimeStamp(endTimeStamp);
            clc.setCmdList(cmdList);

            return clc;
        }

        void sendCommandList() {
            Command cilc = createCmdListCommand(timeStamp);

            broadcast(cilc);
        }

        public void run() {
            sendCommandList();
        }
    }

    void scheduleDelayedVote(long timeStamp) {
        DelayedVote dv = this.new DelayedVote(timeStamp);

        long delay = timeStamp + 2 * bounds.getLinkDelay() - getCurrentTimeStamp();
        delay = delay > 0 ? delay : 0;

        setSentVoteTimeStamp(timeStamp);
        scheduledExecutor.schedule(dv, delay, TimeUnit.MILLISECONDS);
    }


    void scheduleDelayedCmdList(long timeStamp) {
        DelayedCommandList dcl = this.new DelayedCommandList(timeStamp);

        long delay = timeStamp + bounds.getVoteTime() + bounds.getBroadcastDelay() - getCurrentTimeStamp();
        delay = delay > 0 ? delay : 0;

        setSentCmdListTimeStamp(timeStamp);
        scheduledExecutor.schedule(dcl, delay, TimeUnit.MILLISECONDS);
    }

    void executeCommands(long endTimeStamp) {
        List<Command> cmdList = getAcceptedCommandList(executedCmdTimeStamp + 1, endTimeStamp);
        executedCmdTimeStamp = endTimeStamp;

        for (Command c : cmdList) {
            System.out.println("Executing " + c);
        }
    }

    void process() {
        try {
            while (!stopThread) {
                Command c = (Command)commandQueue.take();

                if (c.getType().equals("START_COMMAND")) {
                    StartCommand sc = (StartCommand)c;
                    long timeStamp = sc.getTimeStamp();

                    if (timeStamp + bounds.getLinkDelay() < getCurrentTimeStamp()) {
                        // fail this command. send back response.
                        System.out.println("Command " + sc + " failed");
                        continue;
                    }

                    receivedCommands.add(sc);

                    ReplicaCommand rc = new ReplicaCommand();

                    sc.copy(rc);

                    // send replica commands to peers.
                    sendToPeers(rc);

                    scheduleDelayedVote(timeStamp);
                } else if (c.getType().equals("REPLICA_COMMAND")) {
                    ReplicaCommand rc = (ReplicaCommand)c;
                    long timeStamp = rc.getTimeStamp();

                    if (timeStamp + 2 * bounds.getLinkDelay() < getCurrentTimeStamp()) {
                        // fail this command.
                        continue;
                    }

                    // accept command.
                    // broadcast command accepted to all.
                    AcceptCommand ac = new AcceptCommand();

                    rc.copy(ac);

                    acceptedCommands.put(ac.getId(), ac);
                    broadcast(ac);

                    scheduleDelayedVote(timeStamp);
                } else if (c.getType().equals("ACCEPT_COMMAND")) {
                    AcceptCommand ac = (AcceptCommand)c;

                    // add command to accepted commands list.
                    acceptedCommands.put(ac.getId(), ac);
                } else if (c.getType().equals("VOTE_COMMAND")) {
                    // update vote map.
                    VoteCommand vc = (VoteCommand)c;

                    int port = vc.getSenderPort();
                    long voteTimeStamp = vc.getVoteTimeStamp();

                    voteMap.put(port, voteTimeStamp);

                    // servers accepted command list is the knowledge at the best of its ability at time minTimeStamp.
                    // the correct list is the one obtained by unioning the individual servers command list for some time t.
                    // if you learnt of a new vote timestamp send your vote.
                    if (sentVoteTimeStamp < voteTimeStamp) {
                        scheduleDelayedVote(voteTimeStamp);
                        scheduleDelayedCmdList(voteTimeStamp);
                    } else if (sentCmdListTimeStamp < voteTimeStamp) {
                        scheduleDelayedCmdList(voteTimeStamp);
                    }
                } else if (c.getType().equals("CMDLIST_COMMAND")) {
                    CmdListCommand clc = (CmdListCommand) c;

                    // add the commands into the list. this simulates union of the command lists.
                    for (Command listCmd : clc.getCmdList()) {
                        acceptedCommands.put(listCmd.getId(), listCmd);
                    }

                    int port = clc.getSenderPort();
                    long startTimeStamp = clc.getStartTimeStamp();
                    long endTimeStamp = clc.getEndTimeStamp();

                    // detect if there is a gap in the intervals of knowledge and ask for the commands in between.
                    // this happens if this server failed for an extended period of time so we need to run a recovery scheme.
                    if (cmdListMap.get(port) < startTimeStamp - 1) {
                        CmdListRequestCommand clrc = new CmdListRequestCommand();

                        clrc.setStartTimeStamp(cmdListMap.get(port) + 1); // time of last known update of this.
                        // depending on topology this may involve multiple hops, but is a rare condition and does not affect progress.
                        sendToHost(clrc, port);
                    } else {
                        cmdListMap.put(port, endTimeStamp);

                        if (executedCmdTimeStamp < endTimeStamp) {
                            executeCommands(endTimeStamp);
                        }
                    }
                } else if (c.getType().equals("CMDLISTREQUEST_COMMAND")) {
                    CmdListRequestCommand clrc = (CmdListRequestCommand) c;
                    long startTimeStamp = clrc.getStartTimeStamp();
                    // what is the most recent time for which you can send commands.
                    long endTimeStamp = getCurrentTimeStamp() - 2 * bounds.getLinkDelay();

                    List<Command> cmdList = getAcceptedCommandList(startTimeStamp, endTimeStamp);

                    CmdListCommand clc = new CmdListCommand();

                    clc.setCmdList(cmdList);
                    clc.setStartTimeStamp(startTimeStamp);
                    clc.setEndTimeStamp(endTimeStamp);

                    broadcast(clc);
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
