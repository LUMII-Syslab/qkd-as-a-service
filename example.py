#!/usr/bin/python3

import zmq
import msgpack
import sys

if len(sys.argv) != 2:
  print("Norādiet parametrā vietrādi! Piemēram: tcp://192.168.1.1:5560")
  exit()

servurl = sys.argv[1]
context = zmq.Context()

print("Savienojas ar vietrādi", servurl)
socket = context.socket(zmq.SUB)
socket.setsockopt_string(zmq.SUBSCRIBE, "")
socket.connect(servurl)

while True:
  message = socket.recv()
  print("4")
  list = msgpack.unpackb(message, raw=False)
  print("5 len=",len(list))
  if len(list) == 2:
    print("KeyID", list[0], "Key ", end='')
    for i in range(len(list[1])):
      print('{:02x}'.format(list[1][i]), end='')
    print()
  else:
    print("ERROR: Wrong list len ", len(list))