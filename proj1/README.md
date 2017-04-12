## SDIS - Project 1 - Distributed Backup Service

Project done cooperatively with [Nuno Ramos](https://github.com/NunoRamos).
---
#### Introduction
The goal of this project was to develop a **distributed backup service** for a local area network (LAN). The idea is to use the free disk space of the computers in a LAN for backing up files in other computers in the same LAN. The service is provided by servers in an environment that is assumed cooperative (rather than hostile). Nevertheless, each server retains control over its own disks and, if needed, may reclaim the space it made available for backing up other computers' files.

The service provides 4 subprotocols:
1. **Backup** - Divide a file into chunks of 64000 bytes and send it to the other peers, who will store them
1. **Restore** - Get the chunks from the peers and rewrite the backed up file
1. **Delete** - Instruct all the peers to destroy the chunks they have belonging to a certain file
1. **Reclaim** - Update the space a peer has made available to store chunks. If a peer detects a chunk's replication degree is under the desired value, it will initiate the backup subprotocol for that chunk

These protocols all have enhanced versions, used by the enhanced peers:
1. **Backup** - the chunks are stored to an exact replication degree
1. **Restore** - the chunks are sent back to the peer that requested them by a TCP Socket, instead of a Multicast
1. **Delete** - A peer that was not online when the delete instruction was sent will now be warned when he turns back online
1. **Reclaim** - If the peer that initiated the backup subprotocol fails, another peer will take its place and request the backup.

#### Compiling:
While on the proj1 directory, run the following on your terminal.

```bash
bash scripts/compile.sh
```

#### Running:

To start the peers, run the following instruction on the terminal, while on the proj1 directory:

```bash
# normal version of the peers
bash scripts/peers.sh

# enhanced peers
bash scripts/enhancedpeers.sh
```

You may want to customize the scripts to use your preferred ports and addresses

To test the protocols, a test client is provided, which calls a peer's client interface through RMI. To run this application, run this instruction on your terminal, while on the proj1 directory:

```bash
bash scripts/client.sh <access_point> <OPERATION> <operands>*

# backup example
bash scripts/client.sh peer1 BACKUP file1 2

# restore example
bash scripts/client.sh peer1 RESTORE myfile.pdf

# delete example
bash scripts/client.sh peer1 DELETE thisfile.txt

# reclaim example - value in 10 ^ 3 bytes
bash scripts/client.sh peer1 RECLAIM 6400
```

The files used as an argument for the script should be on the my_files directory of the peer folder
(.../feup-sdis/proj1/bin/my_peers/<peer-id>/my_files)
