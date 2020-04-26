package com.theprogrammingturkey.comz.listeners;

import com.theprogrammingturkey.comz.COMZombies;
import com.theprogrammingturkey.comz.commands.CommandManager;
import com.theprogrammingturkey.comz.commands.CommandUtil;
import com.theprogrammingturkey.comz.game.Game;
import com.theprogrammingturkey.comz.game.Game.ArenaStatus;
import com.theprogrammingturkey.comz.game.GameManager;
import com.theprogrammingturkey.comz.game.features.Barrier;
import com.theprogrammingturkey.comz.game.features.Door;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class OnPlayerChatEvent implements Listener
{

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent playerChat)
	{
		COMZombies plugin = COMZombies.getPlugin();
		Player player = playerChat.getPlayer();
		String message = playerChat.getMessage().replaceFirst(" ", "");

		if(plugin.isEditingASign.containsKey(player))
		{
			if(message.equalsIgnoreCase("done"))
			{
				Sign sign = plugin.isEditingASign.get(player);
				plugin.isEditingASign.remove(player);
				Bukkit.getServer().getPluginManager().callEvent(new SignChangeEvent(sign.getBlock(), player, sign.getLines()));
				CommandUtil.sendMessageToPlayer(player, "You are No longer editing a sign");
				playerChat.setCancelled(true);
				sign.update();
			}
		}
		if(plugin.isCreatingBarrier.containsKey(player))
		{
			Barrier b = plugin.isCreatingBarrier.get(player);
			if(b == null)
			{
				CommandUtil.sendMessageToPlayer(player, ChatColor.DARK_RED + "You have not selected a block for the barrier yet!");
				return;
			}
			if(message.equalsIgnoreCase("done"))
			{
				if(b.getRepairLoc() == null)
				{
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Barrier block for barrier " + b.getNum() + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now select where the repair sign will be located at.");
					playerChat.setCancelled(true);
				}
				else if(b.getSpawnPoint() == null)
				{
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Barrier block repair sign location for barrier " + b.getNum() + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now select the spawn point that is located behind the barrier.");
					playerChat.setCancelled(true);
				}
				else
				{
					final Game game = b.getGame();

					Runnable delayedSpawnFunc = () -> game.resetSpawnLocationBlocks();
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, delayedSpawnFunc, 1);

					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Spawn point for barrier number " + b.getNum() + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now type in the ammount the player will receive per repairation level of the barrier.");
					playerChat.setCancelled(true);
				}
			}
			else if(b.getSpawnPoint() != null && b.getBlock() != null && b.getRepairLoc() != null)
			{
				int price;
				try
				{
					price = Integer.parseInt(message);
				} catch(NumberFormatException ex)
				{
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + message + " is not a number!");
					return;
				}
				b.setReward(price);
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Barrier setup complete!");
				b.getGame().barrierManager.addBarrier(b);
				playerChat.setCancelled(true);
				plugin.isCreatingBarrier.remove(player);
			}
		}
		if(plugin.isCreatingDoor.containsKey(player))
		{
			Door door = plugin.isCreatingDoor.get(player);
			if(message.equalsIgnoreCase("done"))
			{
				if(door.hasBothLocations() && !door.areSpawnPointsFinal() && !door.arePointsFinal())
				{
					door.setPointsFinal(true);
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Door points for door number " + door.doorNumber + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now select any spawn points in the room the door leads to.");
					door.saveBlocks(door.p1, door.p2);
					playerChat.setCancelled(true);
				}
				else if(door.arePointsFinal() && !door.areSpawnPointsFinal() && !door.areSignsFinal())
				{
					door.setSpawnPointsFinal(true);
					if(door.getSpawnsInRoomDoorLeadsTo().size() == 0)
					{
						door.addSpawnPoint(null);
					}
					final Game game = GameManager.INSTANCE.getGame(door);

					Runnable delayedSpawnFunc = () -> game.resetSpawnLocationBlocks();
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, delayedSpawnFunc, 1);

					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Spawn points for door number " + door.doorNumber + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now select any signs that can open this door.");
					playerChat.setCancelled(true);
				}
				else if(door.arePointsFinal() && door.areSpawnPointsFinal() && !door.areSignsFinal())
				{
					door.setSignsFinal(true);
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Signs for door number " + door.doorNumber + " set!");
					CommandUtil.sendMessageToPlayer(player, ChatColor.GOLD + "Now type in a price for the doors.");
					playerChat.setCancelled(true);
				}
			}
			else if(message.equalsIgnoreCase("cancel"))
			{
				String[] args = new String[2];
				args[0] = "cancel";
				args[1] = "doorcreation";
				CommandManager.INSTANCE.onRemoteCommand(player, args);
				playerChat.setCancelled(true);
				return;
			}
			else if(door.arePointsFinal() && door.areSpawnPointsFinal() && door.areSignsFinal())
			{
				int price;
				try
				{
					price = Integer.parseInt(message);
				} catch(NumberFormatException ex)
				{
					CommandUtil.sendMessageToPlayer(player, ChatColor.RED + message + " is not a number!");
					return;
				}
				door.setPrice(price);
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Door setup complete!");
				playerChat.setCancelled(true);
				door.closeDoor();
				plugin.isCreatingDoor.remove(player);
			}
		}
		if(plugin.isArenaSetup.containsKey(player))
		{
			if(message.equalsIgnoreCase("p1"))
			{
				Game game = plugin.isArenaSetup.get(player);
				game.addPointOne(player, player.getLocation());
				playerChat.setCancelled(true);
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Point p1 set for arena : " + game.getName());
			}
			else if(message.equalsIgnoreCase("p2"))
			{
				Location loc = player.getLocation();
				Game game = plugin.isArenaSetup.get(player);
				if(!game.addPointTwo(player, loc))
				{
					playerChat.setCancelled(true);
					return;
				}
				playerChat.setCancelled(true);
				game.arena.setWorld(loc.getWorld());
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Point p2 set for arena : " + game.getName());
			}
			else if(message.equalsIgnoreCase("pw"))
			{
				Location loc = player.getLocation();
				Game game = plugin.isArenaSetup.get(player);
				if(!game.setPlayerTPLocation(player, loc))
				{
					playerChat.setCancelled(true);
					return;
				}
				playerChat.setCancelled(true);
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Player location location set for arena : " + game.getName());
			}
			else if(message.equalsIgnoreCase("sw"))
			{
				//TODO: Don't Limit to be inside arena
				Location loc = player.getLocation();
				Game game = plugin.isArenaSetup.get(player);
				if(!game.setSpectateLocation(player, loc))
				{
					playerChat.setCancelled(true);
					return;
				}
				playerChat.setCancelled(true);
				game.mode = ArenaStatus.WAITING;
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Spectator location set for arena : " + game.getName());
			}
			else if(message.equalsIgnoreCase("lw"))
			{
				Location loc = player.getLocation();
				Game game = plugin.isArenaSetup.get(player);
				if(!game.setLobbySpawn(player, loc))
				{
					playerChat.setCancelled(true);
					return;
				}
				playerChat.setCancelled(true);
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Lobby location set for arena : " + game.getName());
			}
			else if(message.equalsIgnoreCase("cancel"))
			{
				String[] args = new String[2];
				args[0] = "cancel";
				args[1] = "arenacreation";
				CommandManager.INSTANCE.onRemoteCommand(player, args);
				playerChat.setCancelled(true);
				return;
			}
		}
		if(plugin.isRemovingSpawns.containsKey(player))
		{
			if(message.equalsIgnoreCase("cancel"))
			{
				String[] args = new String[2];
				args[0] = "cancel";
				args[1] = "removespawns";
				CommandManager.INSTANCE.onRemoteCommand(player, args);
				playerChat.setCancelled(true);
			}
		}
		if(plugin.isRemovingDoors.containsKey(player))
		{
			if(message.equalsIgnoreCase("cancel"))
			{
				String[] args = new String[2];
				args[0] = "cancel";
				args[1] = "removedoor";
				CommandManager.INSTANCE.onRemoteCommand(player, args);
				playerChat.setCancelled(true);
			}
		}
	}
}
