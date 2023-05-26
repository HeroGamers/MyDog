<h1 align="center">MyDog</h1>
<div align="center">

[![Maven Test](https://github.com/HeroGamers/MyDog/actions/workflows/maven-test.yml/badge.svg)](https://github.com/HeroGamers/MyDog/actions/workflows/maven-test.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=HeroGamers_MyDog&metric=alert_status)](https://sonarcloud.io/dashboard?id=HeroGamers_MyDog)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=HeroGamers_MyDog&metric=ncloc)](https://sonarcloud.io/dashboard?id=HeroGamers_MyDog)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=HeroGamers_MyDog&metric=bugs)](https://sonarcloud.io/dashboard?id=HeroGamers_MyDog)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=HeroGamers_MyDog&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=HeroGamers_MyDog)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=HeroGamers_MyDog&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=HeroGamers_MyDog)
</div>

Tamed wolves but at least 10x better.

___

## Main Plugin Features
*	Dogs teleport upon chunk-unloading, on player-teleport as well as if the player is more than 200 blocks away from their dog, so that the Dogs can always follow their owner. Requires the Dog to not sit (all other Tameables can also be set to teleport on chunk-unload and player-teleportation (not distance) in the config).
*	500+ unique randomly generated dog names.
*	Dogs can gain experience and level up by killing mobs.
*	Configurable option for gaining XP from killing players.
*	Dogs get more health and deal more damage the higher their level is.
*	A beautiful overview over your Dog's stats, including XP, health, damage and last known location.
*	Update the color of your Dog's nametag when their collar gets coloured.
*	Puppies, from breeding two Dogs, become Dogs in MyDog as well.
*	Nametag and name updates when the Dogs collar gets coloured, or the Dog gets a nametag applied (deny the rename permission for nametag-only).
*	The ability to add currently existing Tamed Wolves to the MyDog system, and register them as new Dogs (interact with them).
*	Configurable all the way from the sound on LevelUp to the messages displayed (excluding error messages for now...)

### ToDo Feature-list
To view the list of features, please visit the [project page](https://github.com/users/HeroGamers/projects/1).

For new feature requests, please open a GitHub Issue.

## Resources
*	Resource page on [Spigot](https://www.spigotmc.org/resources/mydog.70260/).
*	Project page on [Bukkit](https://dev.bukkit.org/projects/mydog).

## Permissions
```YAML
mydog.*:
    description: Gives access to all MyDog commands
    default: false
mydog.teleport:
    description: Player's tamed wolves will teleport to the player
    default: true
mydog.help:
    description: Player can view the MyDog command list
    default: true
mydog.putdown:
    description: Player can kill their Dog with a command
    default: true
mydog.free:
    description: Player can free their Dog with a command
    default: true
mydog.comehere:
    description: Player can force their Dogs to load and teleport to the position of the player
    default: true
mydog.dogs:
    description: Player can get an overview about their Dogs
    default: true
mydog.stats:
    description: Player can view stats about their Dogs
    default: true
mydog.rename:
    description: Player can rename their Dogs using the command
    default: true
mydog.setid:
    description: Player can set a custom ID to their Dogs
    default: true
mydog.reload:
    description: Player can reload the configuration(s)
    default: false
mydog.save:
    description: Player can save the configuration(s)
    default: false
```

## Bug reports and issues
Visit the [Issues Tab](https://github.com/DoggyCraftDK/MyDog/issues).
