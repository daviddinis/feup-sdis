#args: <peer_access_point> <operation> <operands>

function usage {
	echo "sh client.sh <peer> <operation> [<operand1> [<operand2>]]"
	echo "Available operations: BACKUP, RESTORE, DELETE, RECLAIM and STATUS"
}


if [ "$#" -eq 0 ]; then
	echo "Usage:"
	usage
	exit 1;
fi


if [ "$2" != "BACKUP" -a "$2" != "RESTORE"  -a "$2" != "DELETE"  -a "$2" != "RECLAIM"  -a "$2" != "STATUS" ]; then
	echo "Invalid operation! Usage:"
	usage
	exit 1;
fi

if [ "$2" = "BACKUP" ]; then
	if [ "$#" -ne 4 ]; then
		echo "Invalid number of arguments for BACKUP"
		echo "sh client.sh <peer> BACKUP <file> <replicationdegree>"
		exit 1;
	fi
fi

if [ "$2" = "RESTORE" ]; then
	if [ "$#" -ne 3 ]; then
		echo "Invalid number of arguments for RESTORE"
		echo "sh. client.sh <peer> RESTORE <file>"
		exit 1;
	fi
fi

if [ "$2" = "DELETE" ]; then
	if [ "$#" -ne 3 ]; then
		echo "Invalid number of arguments for RESTORE"
		echo "sh. client.sh <peer> DELETE <file>"
		exit 1;
	fi
fi

if [ "$2" = "RECLAIM" ]; then
	if [ "$#" -ne 3 ]; then
		echo "Invalid number of arguments for RECLAIM"
		echo "sh. client.sh <peer> RECLAIM <max_space>"
		exit 1;
	fi
fi

#Client
#java cli.ClientInterface <peer_access_point> <operation> <operands>*
xterm -e "java cli.ClientInterface $1 $2 $3 $4 2> /dev/null
$SHELL" &
wait
