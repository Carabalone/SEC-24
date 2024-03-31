#!/usr/bin/env python

import os
import json
import sys
import signal


# Terminal Emulator used to spawn the processes
terminal = "kitty"

# Blockchain node configuration file name
server_configs = [
    "regular_config.json",
    "message_delay.json",
    "message_delay_small_client.json",
    "silent_leader.json",
    "drop_config.json",
    "dictator_leader.json",
    "bad_consensus_config.json",
    "fake_leader_config.json",
    "leader_spoofing.json"
]

client_configs = [
    "regular_config.json",
    "greedy_config.json"
    ]

index = 0
index_client = 0
block_size = 2

try:
    index = int(sys.argv[1])
    index_client = int(sys.argv[2])
except:
    pass
try:
    if sys.argv[1] == "h":
        print(f"\033[93m Provide an index between 0 and {len(server_configs) - 1}. Configurations available:\n{', '.join(enumerate(server_configs))}.\033[0m")
        sys.exit()
except:
    index = 0
    index_client = 0

if index > len(server_configs) - 1:
    print(f"\033[93mWarning: Index {index} is out of range for server_config list. Please provide an index between 0 and {len(server_configs) - 1}. Configurations available:\n{', '.join(server_configs)}.\033[0m")
    sys.exit(0)

server_config = server_configs[index]
client_config = client_configs[index_client]

print("Choosing server config: " + server_config)
print("Choosing client config: " + client_config)

def quit_handler(*args):
    os.system(f"pkill -i {terminal}")
    sys.exit()


# Compile classes
os.system("mvn clean install")

# Spawn blockchain nodes
with open(f"Service/src/main/resources/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            os.system(
                f"{terminal} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config} {client_config} {block_size}' ; sleep 500\"")
            sys.exit()

with open(f"Client/src/main/resources/{client_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            os.system(
                f"{terminal} sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {server_config} {client_config}' ; sleep 500\"")
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
