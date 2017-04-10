#!/bin/bash

PWD=$(pwd)

#rmiregistry
echo "Launching rmi registry"
xterm -e "rmiregistry " 2> /dev/null & 
sleep 1

#Peers
xterm -e "java peers.PeerLauncher 1 1.0 peer1 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "java peers.PeerLauncher 2 1.0 peer2 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "java peers.PeerLauncher 3 1.0 peer3 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "java peers.PeerLauncher 4 1.0 peer4 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "java peers.PeerLauncher 5 1.0 peer5 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  

echo "Launched peers..." 
wait
