#args: <peer_access_point> <operation> <operands>

if [ "$#" -lt 2 ]; then
	echo "Invalid number of arguments. Usage:"
	echo "sh client.sh <peer> <operation> [<operand1> [<operand2>]]"
	exit 1;
fi
accesspoint="peer"
accesspoint=$accesspoint$1

echo $accesspoint

#Client
#java cli.ClientInterface <peerAccessPoint> <protocol_version> <operation> <operands>*
xterm -e "java cli.ClientInterface $accesspoint 1 $2 $3 $4
$SHELL" &
wait
