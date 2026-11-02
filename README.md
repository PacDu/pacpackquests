# PacPack Quests

PacPack Quests is a modern, lightweight questing mod for Fabric 1.21.11. It features a fully integrated in-game visual editor, allowing modpack creators and server admins to build, edit, and organize drag-and-drop quest trees directly in the game, you can open the menu by pressing the `O` key (default).

![Main Interface Preview](https://cdn.modrinth.com/data/cached_images/3c2b6a751e8d82c589926a8d913cb994a2055a70.jpeg)

⚠️ Disclaimer: This mod does not include any quests by default; you will need to create your own. However, I may add a default quest tree in the future for players who want a ready-to-play experience. ⚠️

## ✨ Features

* **In-Game Visual Editor:** Create, move, edit, duplicate and organize quests via a simple, lightweight GUI.
* **Various Quest Types:** Send players on adventures by requiring them to mine blocks, craft items, kill mobs, discover biomes/structures/dimensions, or open specific loot chests.
* **Interactive Selection Menus:** Browse, search, and select items, blocks, mobs, biomes, structures, dimensions and loot tables from a paginated visual grid. Write Minecraft commands with ease thanks to auto-completion. Select parent quests through an interactive screen.
* **Multiplayer Synchronization:** Full client-server sync. Edit quests live on your server, and all connected players will see the updates instantly.
* **Custom Categories:** Group your quests into distinct tabs.

## 🤝 Compatibility

PacPack Quests is designed to work seamlessly with other mods.
Notably, it features native integration and compatibility with **Lootr**. Quests requiring players to open loot chests work perfectly with Lootr's instanced chests, preventing any exploits while correctly tracking player progression.

## 🚀 Future Plans (Roadmap)

I am actively working on expanding the mod's capabilities! Here is a glimpse of what is planned:
* **Graphical Overhaul:** Enhancing the visual appeal of the quest menus and nodes for a more polished experience.
* **New Quest Types:** Adding even more ways to track player progression and actions.
* **New Reward Types:** Introducing more versatile rewards, notably the ability for admins to create a custom list of choices, offering players a random reward from that list upon completion.
* **Teams:** Adding a team system with shared quest progress.

Have an idea for a new feature, a new type of quest, or a cool reward mechanic? Let me know!

## 💬 Feedback & Community

Your feedback is incredibly valuable to help improve the mod!
If you have a suggestion, an idea for the roadmap, or if you encounter a bug, please feel free to:
* **Open an issue** on the [GitHub repository](https://github.com/PacDu/pacpackquests/issues).
* **Come chat** with me and the community on my [Discord server](https://discord.gg/U3gYa2YuQd).

----

## 🛠️ How to Use

The mod is very easy to use, but here are some explanations to help you get the hang of it.

⚠️ **All the instructions below that require the Edit mode are only available with admin privileges (Allow Commands enabled in singleplayer or OP permissions on a server)** ⚠️

### I - Quest Menu

This screen is the main screen, at the start you will have only one category with an empty grid giving you free rein to design your own quest trees.

* **The Edit Mode:** In the top right corner, you will have an "Edit" button (only visible to admins) letting you switch between the **`Normal Mode`** (for all players) and the **`Edit Mode`** (for admins).

### Normal Mode, Player View

* **Layout:** In the center, you'll find the quest grid with all the quests in the current category. On the left, you'll have the category tabs letting you switch between categories. At the bottom, the "Done" button closes the menu and the "Claim All" button lets you claim rewards for completed quests all at once.
* **Navigation:** Clicking (left, right, or middle button) and dragging allows you to pan across the quest grid, while the scroll wheel lets you zoom in and out. Clicking a category tab switches to that category.
* **Claim a Quest:** Clicking a completed quest grants you its reward.
* **Quest Info:** Hovering over a quest displays all its details and requirements.

### Edit Mode, Quest Creators

* **Quest Grid:** In the center of the screen, you'll find a 20x20 grid letting you create a quest by right-clicking on any empty grid cell.
* **Editing a Quest:** Right-clicking on a quest will let you edit all its elements (except its ID) or delete it. Left-click & drag will allow you to move a quest on the grid. Middle-clicking on a quest will copy it and let you paste it in any empty grid cell, you can cancel it by pressing Escape.
* **Category Tabs:** Beside the category tabs, you'll find a button to edit a category's name and a button to delete the entire category. Under the last category tab, a "+" button will let you create a new category.

### II - Edit Menu

This screen lets you edit every quest option or delete the quest.

* **Title:** Give the quest a title, you can use a translation key here, maximum of 96 characters.
* **Task:** Select a task type among the 8 possible choices:
    * **Mine Block:** requires the player to mine the target block.
    * **Kill Mob:** requires the player to kill the target mob.
    * **Craft Item:** requires the player to craft the target item in a crafting table.
    * **Explore Biome:** requires the player to enter the target biome.
    * **Explore Structure:** requires the player to enter the target structure.
    * **Explore Dimension:** requires the player to enter the target dimension.
    * **Obtain Item:** requires the player to have the target item in their inventory.
    * **Open a Loot Chest:** requires the player to open a loot chest, such as those found in generated structures.
* **Task Target:** The target of the task.
* **Required Amount:** The required amount to reach in order for the user to complete the quest. Unavailable for biomes and dimensions. Maximum of 9,999,999 for all tasks except `Obtain Item` that is limited to 2,304 because it's the maximum amount of items in a player's inventory (4 x 9 x 64).
* **Unique ID:** The quest ID, used to name the associated JSON file and identify the quest among the others by the mod. Defaults to a formatted version of the title. Must be unique, if the default given ID isn't unique, you'll be prompted to manually edit it with what you want.
* **Parent Quests:** Prerequisite quests that the player must complete before unlocking this one.
* **Reward:** The type of the reward given to the player when they complete the quest:
    * **Item:** gives an item to the player.
        * **Reward Item:** the item given to the player as a reward.
    * **XP:** gives an amount of XP to the player.
    * **XP Level(s):** gives an amount of XP levels to the player.
    * **Command:** executes a Minecraft command at claim.
        * **Reward Command to Execute:** the command executed when the player claims the quest reward.
* **Reward Amount:** the amount of reward given to the player. For command rewards, the amount corresponds to the number of executions of the command. Maximum of 9,999,999 for all rewards except for commands, which are capped at 256 to prevent crashes or server lag.
* **Icon:** The icon that will be displayed in the quest grid. Defaults to the task target icon but can be manually changed if desired.

### III - Selection Screen

This screen lets you choose an element among a list of entries.
* **The Search Field:** On top you'll find a search field, similar to JEI's,
    * By starting a search with `@`, it will search only in the mod name of the elements, for example `@draconic` will show you only the elements of the mod Draconic Evolution.
    * By starting the search with a `#`, it will search only in the tag names.
    * By typing a normal search, it will search in the element names and additional info, for example, searching "speed" will show you, among others, the swiftness and slowness arrows because they have the speed percentage effect in their additional info.
* **The grid:** A grid of all the possible elements. Hovering over an element will show you its name, mod name and additional info. Clicking on an element will select it and close the screen.

### IV - Parent Selection Screen

This screen lets you select all parent quests. It features the same grid and navigation controls as the Quest Menu. Toggle parents by clicking on quests, then click the `Done` button to confirm your selection.

### V - Command Edit Screen

This screen lets you write a Minecraft command like in a command block. The command will be executed [Reward Amount] times when the player will claim the quest reward. The command description accept translation keys.