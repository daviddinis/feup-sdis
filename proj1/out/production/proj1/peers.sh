#!/bin/bash

PWD=$(pwd)

#rmiregistry
xterm -e "rmiregistry " 2> /dev/null & 
sleep 1

#Peers
xterm -e "java peers.PeerLauncher 1 1.4 peer1 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "java peers.PeerLauncher 2 1.4 peer2 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "java peers.PeerLauncher 3 1.4 peer3 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "java peers.PeerLauncher 4 1.4 peer4 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "java peers.PeerLauncher 5 1.4 peer5 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  

echo "Launched peers..." 
wait
