PopTart
========
Custom Java implementation of the Pastry P2P network. Utilizes distributed hash tables for request and content routing. 

Usage
=====
DiscoveryPeer HOST_ADDR HOST_PORT

MemberPeer DISCOVERY_ADDR DISCOVERY_PORT STORAGE_DIR ID

StorageClient DISCOVERY_ADDR DISCOVERY_PORT

The ID component of MemberPeer is optional. If it is not provided, it will be generated automatically.
