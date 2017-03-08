package src.peers;

import java.net.InetAddress;

public class PeerService {

    private InetAddress mc_addr;
    private int mc_port;

    private InetAddress mdb_addr;
    private int mdb_port;  

    private InetAddress mdr_addr;
    private int mdr_port;

    public PeerService(InetAddress mc_addr,int mc_port,InetAddress mdb_addr,int mdb_port,
                       InetAddress mdr_addr,int mdr_port){

        this.mc_addr = mc_addr;
        this.mc_port = mc_port;

        this.mdb_addr = mdb_addr;
        this.mdb_port = mdb_port;

        this.mdr_addr = mdr_addr;
        this.mdr_port = mdr_port;

        System.out.println("Multicast channel addr: "+ mc_addr+" port: "+ mc_port);
        System.out.println("Multicast data backup addr: "+ mdb_addr+" port: "+ mdb_port);
        System.out.println("Multicast data restore addr: "+ mdr_addr+" port: "+ mdr_port);

    }

}
