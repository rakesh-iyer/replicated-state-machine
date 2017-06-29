class RSM {

    public static void main(String args[]) {
        if (args.length != 3) {
            System.out.println("We need 3 server ports to be specified");
            return;
        }

        try { 
            int port;
            int [] peerPorts = new int[2];
            boolean stopped = false;

            port = Integer.valueOf(args[0]);

            peerPorts[0] = Integer.valueOf(args[1]);
            peerPorts[1] = Integer.valueOf(args[2]);

            ReplicatingServer rs = new ReplicatingServer(port, peerPorts, false);

            Thread rsThread = new Thread(rs);

            rsThread.start();

            while (!stopped) {
                Thread.sleep(10000);
                rs.addCommand();
            }

            rsThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
