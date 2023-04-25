# SecureCRDT

This project implements a CRDT replica where data is encrypted with secret sharing and all operations over the data are done using secure multiparty protocols. This replica, if correctly deployed, enures that a single malicious hosting provider does not learn the contents of the data in the CRDT.

Currently, we support there secure implementation of the following CRDTs:

- Last Write Wins Register
- Max Value
- Positive Negative Counter
- Bounder Counter
- Grow Only Counter
- Grow Only Set

# System Architecture

A SecureCRDT Replica consists of three independent SMPC parties. Each SMPC party must be hosted in an independent trust domain (e.g: Cloud Provider).
A client that issue two operations to a replica:
1. an `update` request to add or update a data value in a CRDT.
2. a`query` operations on the retrieve the value in a CRDT

For each `update` operation, the client uses a secret share algorithm to encode the data and sends one share to each party. Therefore, no party has access to the plaintext value, and a malicious provider can not learn the contents of the CRDT.

To ensure data is consistent and to provide a correct result for `update` operations, the replica operates over the secret shared data by using multi-party protocols.

![System Architecture](docs/architechture.png)




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