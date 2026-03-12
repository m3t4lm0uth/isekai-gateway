# Isekai Gateway

Velocity plugin that adds player flags and routing logic for event servers.

/isekaigateway trigger <player> 

  - flags a player as "EventRequired", and disconnects them from main.
  - on reconnection, attempts to route player to event server.
  - flags currently persist until proxy is rebooted.

# Future plans

- Incorporate database:
  - Flag persistence: store player flags in database so they persist beyond proxy reboot.
  - Backend updates: backend servers can report to db when certain events occur to flip "EventRequired" flag.
  - Maybe include persistent inventory management options?
    - This may be better as seperate plugin.
- Make disconnect screen more useful for players setting up necessary client for event server when connecting for the first time.
  - Potentially give option for player to use helper script that copies main MC instance on prism (to keep desired settings and server IP) and automatically grab correct modpack; perhaps even rebooting into it after it is done.
  - Else, perhaps link to README with manual instructions.
  - Goal is to make reconnection to moddded server as seamless as possible for user.
