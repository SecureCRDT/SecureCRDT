# Secure Conflict-free Replicated Data Types

Privacy-preserving implementation of CRDT's built with a MPC-based approach.


# How to install

```shell

$ mvn package
```

# How to run 3 separate players and one client:

If the setup is to be run in a distributed manner, remove the keyword "local" from the commands and set the ip for each 
player in the Standards.java file, before re-installing. If the setup is to be run with no resharing before propagation, 
remove the keyword "reshare" from the commands. Don't forget to replace the path to the project parent folder.

Open three terminal instances and run in each one, respectively:
```shell
$ java -cp /$PATH-TO-PROJECT-HOME/SecureCRDT/target/SecureCRDT-1.0.jar pt.uporto.dcc.securecrdt.crdt.CrdtPlayer 0 local reshare

$ java -cp /$PATH-TO-PROJECT-HOME/SecureCRDT/target/SecureCRDT-1.0.jar pt.uporto.dcc.securecrdt.crdt.CrdtPlayer 1 local reshare

$ java -cp /$PATH-TO-PROJECT-HOME/SecureCRDT/target/SecureCRDT-1.0.jar pt.uporto.dcc.securecrdt.crdt.CrdtPlayer 2 local reshare
```

Lastly, initiate the client (Don't forget to replace $CRDT with one of the following types: 
register, gcounter, pncounter, maxvalue, minboundedcounter, evergrowingset, setwithleakage):

```shell
$ java -cp /$PATH-TO-PROJECT-HOME/SecureCRDT/target/SecureCRDT-1.0.jar pt.uporto.dcc.securecrdt.client.Client $CRDT local
```

# Authors

The main author of the source code in this repository is [Pedro Jorge](https://github.com/0xpedrojorge).