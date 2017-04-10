#!/bin/bash

PWD=$(pwd)

#rmiregistry
echo "Launching rmi registry"
xterm -e "cd bin && rmiregistry " 2> /dev/null & 
sleep 1

#Peers
xterm -e "cd bin && java peers.PeerLauncher 2.0 1 peer1 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "cd bin && java peers.PeerLauncher 2.0 2 peer2 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "cd bin && java peers.PeerLauncher 2.0 3 peer3 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null & 
xterm -e "cd bin && java peers.PeerLauncher 2.0 4 peer4 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  
xterm -e "cd bin && java peers.PeerLauncher 2.0 5 peer5 224.0.0.0 4445 224.0.0.1  4446 224.0.0.2 2000 " 2> /dev/null &  

echo "Launched enhanced peers..." 
wait
