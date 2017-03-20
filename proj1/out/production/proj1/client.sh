#Client
xterm -e "java cli.ClientInterface peer1 1 $1 $2
$SHELL" &
wait
