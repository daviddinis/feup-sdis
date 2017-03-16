#!/bin/bash

if [ "$#" -lt 2 ]; then
	echo 'Insufficient number of arguments. Usage:'
	echo 'sh test.sh <operation> <operand1> <operand2>'
	exit 1;
fi


PWD=$(pwd)

#rmiregistry
xterm -e "rmiregistry " &

#Peers
xterm -e "java peers.PeerLauncher 1 1 peer1 224.0.1.2 1025 230.0.2.2  1028 225.2.3.2 2000 " & 

#Client
gnome-terminal -e "java cli.ClientInterface peer1 $2 $3 $4 " &

wait
