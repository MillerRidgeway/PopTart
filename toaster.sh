tab="--tab-with-profile=Miller"
foo=""
cmd="bash -c 'java -cp libs/PopTart.jar peer.DiscoveryPeer 51000'"

mkdir /tmp/Peers/peerFiles{1..10}

cd ~/Projects/PopTart/build/

foo+=($tab -e "$cmd")
gnome-terminal "${foo[@]}"

for i in {1..10}
  do
    cmd="bash -c 'java -cp libs/PopTart.jar peer.MemberPeer 127.0.0.1 51000 /tmp/Peers/peerFiles${i}/'"
    foo=($tab -e "$cmd")
    gnome-terminal "${foo[@]}"
  done

cmd="bash -c 'java -cp libs/PopTart.jar peer.StoreClient 127.0.0.1 51000'"
foo=($tab -e "$cmd")
    gnome-terminal "${foo[@]}"
gnome-terminal "${foo[@]}"
