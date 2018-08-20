from scapy.all import *
import socket
import sys
import time

pcap = rdpcap('test2.pcap')

ips = set([(p[IP].fields['src'], p[IP].fields['dst']) for p in pcap if p.haslayer(IP) == 1])

ipFile= open("ipFile.out","w+")
for e in ips:
	res = e[0]+ " "+ e[1] 
	print res
	ipFile.write(res + "\n")

ipFile.close()

host = 'localhost'
port = 9999

#create an INET, STREAMing socket
serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#bind the socket to a public host,
# and a well-known port
serversocket.bind((host, 9999))
#become a server socket
serversocket.listen(5)

conn, addr = serversocket.accept()
print 'Connected by: ', addr

ipData = open('ipFile.out', 'r')

while True:
	i = 1
	try:
		for line in ipData:
			print i, "[dbg] the line is sending: ", line
			conn.sendall(line)
			i = i+1

	except socket.error:
		print "Error Occured."
		break

conn.close()