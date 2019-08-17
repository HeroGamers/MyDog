MyDog
======

[![Build Status](https://travis-ci.com/DoggyCraftDK/MyDog.svg?branch=master)](https://travis-ci.com/DoggyCraftDK/MyDog)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DoggyCraftDK_MyDog&metric=alert_status)](https://sonarcloud.io/dashboard?id=DoggyCraftDK_MyDog)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=DoggyCraftDK_MyDog&metric=ncloc)](https://sonarcloud.io/dashboard?id=DoggyCraftDK_MyDog)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=DoggyCraftDK_MyDog&metric=bugs)](https://sonarcloud.io/dashboard?id=DoggyCraftDK_MyDog)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=DoggyCraftDK_MyDog&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=DoggyCraftDK_MyDog)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=DoggyCraftDK_MyDog&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=DoggyCraftDK_MyDog)

Tamed wolves but at least 10x better

**Plugin Features:**
*	Dogs teleport upon chunk-unloading, so the Dogs can follow their owner. Requires the Dog to not sit.
*	Randomly generated names.
*	Dog Stats.
*	Dog Levels.
*	Dogs list.
*	Comehere command.
*	Nametag and name updates when the Dogs collar gets coloured or the Dog gets a nametag applied.
*	Automatically make newly bred Wolves into a Dog.
*	Config option for killing players for XP.
*	Configurables all the way from the sound to the messages displayed (excluding error messages for now...)

**Resources:** [Spigot Resource](https://www.spigotmc.org/resources/mydog.70260/)

**TODO Feature-list:**
*	Add inventory to Dogs, so they can wear armor and take less damage.
*	Try to hide the nametag, and only show on hover, if possible.

**Permissions:**
```YAML
 mydog.teleport:
    description: Player's tamed wolves will teleport to the player
    default: true
mydog.help:
    description: Player can view the MyDog command list
    default: true
mydog.putdown:
    description: Player can kill their Dog with a command
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
mydog.reload:
    description: Player can reload the configuration(s)
    default: false
mydog.save:
    description: Player can save the configuration(s)
    default: false
```
