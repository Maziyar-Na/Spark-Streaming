# Spark-Streaming
JetStream vs Spark Streaming

1. Run PcapReader.py as a server(on localhost:9999). It will run a server in which extracts IP addresses from a .pcap file(name of the file is in the python program(test2.pcap) ), then writes the IPs into a file named ipFile.out

2. In another terminal, run apacheSpark.py in a linux machine that has Spark installed on it with this command: 
  /usr/local/bin/spark-submit apacheSpark.py localhost 9999 > ~/Desktop/output.out
  It will write the output of Spark Streaming program into output.out
  
  
