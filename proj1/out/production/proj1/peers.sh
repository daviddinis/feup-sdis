#!/bin/bash

PWD=$(pwd)

#rmiregistry
xterm -e "rmiregistry " & 
sleep 1

#Peers
xterm -e "java peers.PeerLauncher 1 1 peer1 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " &  
xterm -e "java peers.PeerLauncher 2 1 peer2 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " & 
xterm -e "java peers.PeerLauncher 3 1 peer3 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " & 
xterm -e "java peers.PeerLauncher 4 1 peer4 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " & 

echo "Launched peers..." 
wait
