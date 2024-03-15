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
    "small_config.json",
    "tiny_config.json",
    "message_delay.json"
]

index = 0
if (len(sys.argv) > 1):
    try:
        index = int(sys.argv[1])
    except:
        index = 0

if index > len(server_configs) - 1:
    print(f"\033[93mWarning: Index {index} is out of range for server_config list. Please provide an index between 0 and {len(server_configs) - 1}. Configurations available:\n{', '.join(server_configs)}.\033[0m")
    sys.exit(0)

server_config = server_configs[index]

print("Choosing config: " + server_config)

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
                f"{terminal} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config}' ; sleep 500\"")
            sys.exit()

with open(f"Client/src/main/resources/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            os.system(
                f"{terminal} sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {server_config}' ; sleep 500\"")
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
