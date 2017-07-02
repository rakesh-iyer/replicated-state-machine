import java.util.*;
import java.util.concurrent.*;

class ReplicatingServer implements Runnable {
    BlockingQueue<Message> commandQueue = new LinkedBlockingQueue<Message>();
    NetworkParameterBounds bounds = new NetworkParameterBounds();
    MessageReceiver messageReceiver;
    List<Command> receivedCommands = new ArrayList<>();
    List<Command> executedCommands = new ArrayList<>();
    Map<String, Command> acceptedCommands = new HashMap<>();
    long acceptedCommandTimeStamp;
    long sentCmdIdListTimeStamp;
    boolean stopThread;
    int port;
    int [] peerPorts;
    Map<Integer, Long> voteMap = new HashMap<>();
    Map<Integer, Long> cmdListMap = new HashMap<>();
    DelayedCommandWorker delayedCommandWorker = this.new DelayedCommandWorker();
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
        delayedCommandWorker.start();
    }

    class DelayedCommandWorker extends Thread {
        DelayQueue<Command> delayedCommandQueue = new DelayQueue<>();

        public void run() {
            process();
        }

        void addDelayedCommand(Command c) {
            delayedCommandQueue.add(c);
        }

        void process() {
            try {
                while (!stopThread) {
                    Command c = delayedCommandQueue.take();

                    broadcast(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    Command createVoteCommand(long timeStamp) {
        VoteCommand vc = new VoteCommand();

        vc.setId(UUID.randomUUID().toString());
        vc.setVoteTimeStamp(timeStamp);

        return vc;
    }

    Command createCmdIdListCommand(long endTimeStamp) {
        CmdIdListCommand cilc = new CmdIdListCommand();

        long startTimeStamp = sentCmdIdListTimeStamp + 1;

        List<Command> cmdList = getAcceptedCommandList(startTimeStamp, endTimeStamp);
        List<String> cmdIdList = new ArrayList<>();
        for (Command c : cmdList) {
            cmdIdList.add(c.getId());
        }

        cilc.setStartTimeStamp(sentCmdIdListTimeStamp + 1);
        cilc.setEndTimeStamp(getCurrentTimeStamp() - 2 * bounds.getLinkDelay());
        cilc.setCmdIdList(cmdIdList);

        return cilc;
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

    void scheduleVote(long timeStamp) {
        Command vc = createVoteCommand(timeStamp);

        vc.setTimeStamp(timeStamp + 2 * bounds.getLinkDelay());
        delayedCommandWorker.addDelayedCommand(vc);
    }

    void scheduleCommandIdList(long timeStamp) {
        Command cilc = createCmdIdListCommand(timeStamp);

        cilc.setTimeStamp(timeStamp + 2 * bounds.getLinkDelay() + bounds.getBroadcastDelay());
        delayedCommandWorker.addDelayedCommand(cilc);
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

                    scheduleVote(timeStamp);
                    scheduleCommandIdList(timeStamp);
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

                    scheduleVote(timeStamp);
                    scheduleCommandIdList(timeStamp);
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

                    // find the latest timestamp for which commands are accepted and execute all commands until that.
                } else if (c.getType().equals("CMDIDLIST_COMMAND")) {
                    CmdIdListCommand cilc = (CmdIdListCommand)c;
                    boolean fetchCommands = true;

                    int port = cilc.getSenderPort();
                    long startTimeStamp = cilc.getStartTimeStamp();
                    long endTimeStamp = cilc.getEndTimeStamp();

                    // detect if there is a gap in the intervals of knowledge and ask for the commands in between.
                    if (cmdListMap.get(port) == startTimeStamp-1) {
                        // make sure you have all the cmdids
                        for (String cmdId : cilc.getCmdIdList()) {
                            if (acceptedCommands.get(cmdId) == null) {
                                break;
                            }
                        }

                        fetchCommands = false;
                    }

                    if (fetchCommands) {
                        CmdListRequestCommand clrc = new CmdListRequestCommand();

                        clrc.setStartTimeStamp(acceptedCommandTimeStamp+1); // time of last known update of this.
                        broadcast(clrc);
                    } else {
                        acceptedCommandTimeStamp = endTimeStamp;
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
                } else if (c.getType().equals("CMDLIST_COMMAND")) {
                    CmdListCommand clc = (CmdListCommand) c;

                    for (Command listCmd : clc.getCmdList()) {
                        acceptedCommands.put(listCmd.getId(), listCmd);
                    }
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
