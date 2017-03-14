# feup-sdis
SDIS - Labs and Project Code
Repo for the code for the Lab classes and Projects for the **Distributed Systems (SDIS)** class of the **Master in Informatics and Computer Engineering (MIEIC)** at the **Faculty of Engineering of the University of Porto (FEUP)**.

Project 1 done cooperatively with [Nuno Ramos](https://github.com/NunoRamos).

To run the following programs you will need to be on the directory proj1/out/productions/src.

To run a server, execute (for example):
```
 java peers.PeerLauncher 1 2 peer1 224.0.1.2 1025 230.0.2.2 1028 225.2.3.2 2000
```
You will need to execute rmiregistry, so do:
```
rmiregistry
```

To run a client, execute (for example):
```
java cli.ClientInterface peer1 a BACKUP filename 2
```
