if [ "$#" -lt 3 ]; then
	echo "Invalid number of arguments. Usage:"
	echo "sh client.sh operation> [<operand1> [<operand2>]]"
	exit 1;
fi

#Client
xterm -e "java cli.ClientInterface peer1 1 $1 $2 $3
$SHELL" &
wait
