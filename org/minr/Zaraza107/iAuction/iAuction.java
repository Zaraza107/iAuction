package org.minr.Zaraza107.iAuction;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.String;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.*;
import org.bukkit.command.*;

import com.nijikokun.bukkit.iConomy.iConomy;
import com.nijikokun.bukkit.iConomy.Messaging;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import com.bukkit.dthielke.herochat.HeroChatPlugin;
import com.bukkit.dthielke.herochat.Channel;

/**
 * iAuction for iConomy for Bukkit #172+ (Craftbukkit #354+)
 *
 * @author Zaraza107
 * @version 2.3 beta
 */
public class iAuction extends JavaPlugin {

    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    public static Server server;
    public String currency = "";
    private Timer auctionTimer;
    private TimerTask auctionTT;
    public boolean isAuction = false;
    public int auctionTime;
    public Player auctionOwner;
    public Player winner;
    private int auctionItemId;
    private short auctionItemDamage;
    private int auction_item_byte;
    private int auctionItemAmount;
    private int auctionItemStarting;
    private int auctionItemBid;
    private boolean win = false;
    private int currentBid;
    public Player timerPlayer;
    public String tag;
    public iConomy iConomy;
    public PermissionHandler Permissions;
    private boolean permissionsEnabled = false;
    public static HeroChatPlugin herochat;
    
    private int maxTime;
    
    private boolean hcEnabled;
    private String hcChannelName;
    
    private String tagColor;
    private String warningColor;
    private String auctionStatusColor;
    private String auctionTimeColor;
    private String helpMainColor;
    private String helpCommandColor;
    private String helpOrColor;
    private String helpObligatoryColor;
    private String helpOptionalColor;
    private String infoPrimaryColor;
    private String infoSecondaryColor;

    /*
     * Internal Properties controllers
     */
    public static iProperty Item;
    public static HashMap<String, String> items;
    public static iProperty Settings;

    public void onEnable() {
    	server = getServer();
        PluginManager pm = server.getPluginManager();
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(Messaging.bracketize(pdfFile.getName()) + " (version " + pdfFile.getVersion() + ") is enabled!");

        Plugin ic = server.getPluginManager().getPlugin("iConomy");
        if (ic == null) {
            System.out.println(Messaging.bracketize("iAuction") + " Warning! iConomy plugin is not loaded!");
        } else {
            iConomy = (iConomy)ic;
            currency = this.iConomy.currency;
        }

        Item = new iProperty("items.db");
        setupItems();
        setupPermissions();
        setupSettings();
    }

    public void onDisable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(Messaging.bracketize("iAuction") + " version " + pdfFile.getVersion() + " is disabled.");
    }

    /**
     * Setup Items
     */
    public void setupItems() {
        Map mappedItems = null;
        items = new HashMap<String, String>();

        try {
            mappedItems = Item.returnMap();
        } catch (Exception ex) {
            System.out.println(Messaging.bracketize("iAuction") + " could not open items.db!");
        }

        if (mappedItems != null) {
            for (Object item : mappedItems.keySet()) {
                String left = (String) item;
                String right = (String) mappedItems.get(item);
                String id = left.trim();
                String itemName;
                //log.info("Found " + left + "=" + right + " in items.db");
                if (id.matches("[0-9]+") || id.matches("[0-9]+,[0-9]+")) {
                    //log.info("matches");
                    if (right.contains(",")) {
                        String[] synonyms = right.split(",");
                        itemName = synonyms[0].replaceAll("\\s", "");
                        items.put(id, itemName);
                        //log.info("Added " + id + "=" + itemName);
                        for (int i = 1; i < synonyms.length; i++) {
                            itemName = synonyms[i].replaceAll("\\s", "");
                            items.put(itemName, id);
                            //log.info("Added " + itemName + "=" + id);
                        }
                    } else {
                        itemName = right.replaceAll("\\s", "");
                        items.put(id, itemName);
                        //log.info("Added " + id + "=" + itemName);
                    }
                } else {
                    itemName = left.replaceAll("\\s", "");
                    id = right.trim();
                    items.put(itemName, id);
                    //log.info("Added " + itemName + "=" + id);
                }
            }
        }
    }
    
    public void setupSettings() {
    	String path = "plugins" + File.separator + "iAuction" + File.separator;
    	new File(path).mkdir();
    	
    	Settings = new iProperty(path + "iAuction.settings");
    	
    	maxTime = Settings.getInt("maximal-time", 0);
    	hcEnabled = Settings.getBoolean("enable-herochat", false);
    	if(hcEnabled) {
    		hcChannelName = Settings.getString("herochat-channel-name", null);
    		if(hcChannelName != null) {
    	        Plugin hc = server.getPluginManager().getPlugin("HeroChat");
    	        if (hc == null) {
    	            System.out.println(Messaging.bracketize("iAuction") + " Warning! HeroChat plugin is enabled but could not get loaded!");
    	        } else {
    	            herochat = (HeroChatPlugin)hc;
    	        }
    		}
    	}
    	
    	tagColor = ChatColor.valueOf(Settings.getString("tag-color", "yellow").toUpperCase()).toString();
    	warningColor = ChatColor.valueOf(Settings.getString("warning-color", "red").toUpperCase()).toString();
    	auctionStatusColor = ChatColor.valueOf(Settings.getString("auction-status-color", "dark_green").toUpperCase()).toString();
    	auctionTimeColor = ChatColor.valueOf(Settings.getString("auction-time-color", "dark_aqua").toUpperCase()).toString();
    	helpMainColor = ChatColor.valueOf(Settings.getString("help-main-color", "yellow").toUpperCase()).toString();
    	helpCommandColor = ChatColor.valueOf(Settings.getString("help-command-color", "aqua").toUpperCase()).toString();
    	helpOrColor = ChatColor.valueOf(Settings.getString("help-or-color", "blue").toUpperCase()).toString();
    	helpObligatoryColor = ChatColor.valueOf(Settings.getString("help-obligator-color", "dark_red").toUpperCase()).toString();
    	helpOptionalColor = ChatColor.valueOf(Settings.getString("help-optional-color", "light_purple").toUpperCase()).toString();
    	infoPrimaryColor = ChatColor.valueOf(Settings.getString("info-primary-color", "blue").toUpperCase()).toString();
    	infoSecondaryColor = ChatColor.valueOf(Settings.getString("info-secondary-color", "aqua").toUpperCase()).toString();
    	
    	tag = tagColor + "[AUCTION] ";
    }

    public void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (this.Permissions == null) {
            if (test != null) {
                this.Permissions = ((Permissions)test).getHandler();
                System.out.println(Messaging.bracketize("iAuction") + " Permission system enabled.");
                permissionsEnabled = true;
            } else {
                System.out.println(Messaging.bracketize("iAuction") + " Permission system not enabled. Disabling support.");
                permissionsEnabled = false;
            }
        }
    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
    	String commandName = cmd.getName();
    	
    	if(sender instanceof Player)
    	{
    		if(commandName.equalsIgnoreCase("auction"))
    		{
    			try {
    				if(args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))
    					auctionHelp((Player)sender);
    				else if(args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("s"))
    					auctionStart((Player)sender, args);
    				else if(args[0].equalsIgnoreCase("bid") || args[0].equalsIgnoreCase("b"))
    					auctionBid((Player)sender, args);
    				else if(args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e"))
    					auctionStop((Player)sender);
    				else if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i"))
    					auctionInfo(null, (Player)sender);
    				return true;
    			} catch(ArrayIndexOutOfBoundsException ex) {
    				return false;
    			}
    		}
    	}
    	return false;
    }
    
    public void auctionHelp(Player player) {
    	String or = helpOrColor + "|";
        Messaging.send(player, tagColor + " -----[ " + auctionStatusColor + "Auction Help" + tagColor + " ]----- ");
        Messaging.send(player, helpCommandColor + "/auction help" + or + helpCommandColor + "?" + helpMainColor + " - Returns this");
        Messaging.send(player, helpCommandColor + "/auction start" + or + helpCommandColor + "s" + helpObligatoryColor + " <time> <item> <amount> <starting price>");
        Messaging.send(player, helpMainColor + "Starts an auction for " + helpObligatoryColor + "<time> " + helpMainColor + "seconds with " + helpObligatoryColor + "<amount>");
        Messaging.send(player, helpMainColor + "of " + helpObligatoryColor + "<item> " + helpMainColor + "for " + helpObligatoryColor + "<starting price>");
        Messaging.send(player, helpCommandColor + "/auction bid" + or + helpCommandColor + "b" + helpObligatoryColor + " <bid> " + helpOptionalColor + "(maximum bid)" + helpMainColor + " - Bids the auction.");
        Messaging.send(player, helpMainColor + "If you set a " + helpOptionalColor + "(maximum bid) " + helpMainColor + "and the " + helpObligatoryColor + "<bid> " + helpMainColor + "is greater than the");
        Messaging.send(player, helpMainColor + "current, you will outbid that bid if it is lower than your maximum.");
        Messaging.send(player, helpCommandColor + "/auction end" + or + helpCommandColor + "e" + helpMainColor + " - Ends current auction.");
        Messaging.send(player, helpCommandColor + "/auction info" + or + helpCommandColor + "i" + helpMainColor + " - Returns auction information.");
    }
    
    public void warn(Player player, String msg) {
    	player.sendMessage(tag + warningColor + msg);
    }
    
    public void help(Player player) {
    	player.sendMessage(warningColor + "Use \"/auction ?\" to recieve help.");
    }
    
    public void broadcast(String msg) {
    	if(hcEnabled && hcChannelName != null) {
    		Channel chan = herochat.getChannel(hcChannelName);
    		herochat.sendMessage(chan, "iAuction", msg);
    	}
    	else
    		server.broadcastMessage(tag + msg);
    }

    public void auctionStart(Player player, String[] msg) {
    	if(!permissionsEnabled || this.Permissions.has(player,"auction.start")) {
    		if(!isAuction) {
    			if(msg.length == 5) {
    		        auctionOwner = player;
    		        timerPlayer = player;
    		        
    		        try {
    		        	auctionTime = Integer.parseInt(msg[1]);
    		        	auctionItemAmount = Integer.parseInt(msg[3]);
    		            auctionItemStarting = Integer.parseInt(msg[4]);
    		        } catch (NumberFormatException ex) {
    		            warn(player, "Invalid syntax.");
    		            help(player);
    		            return;
    		        }
    		        if(maxTime != 0 && auctionTime > maxTime)
    		        {
    		        	warn(player, "Too long! Maximal time is " + maxTime);
    		        	return;
    		        }
    		        
		            int[] id = new int[]{-1, 0};
		            int count = 0;
    		        
		            try {
		            	id = Items.validate(msg[2]);
		            } catch(Exception e) {
		            	warn(player, "Invalid item."); return;
		            }

		            if (id[0] == -1 || id[0] == 0) {
		                warn(player, "Invalid item."); return;
		            }

		            for (ItemStack item : player.getInventory().getContents()) {
		                if (item.getTypeId() == id[0]) {
		                    MaterialData data = item.getData();

		                    if(id[1] != 0) {
		                        if (data.getData() == (byte)id[1]) {
		                        	auctionItemDamage = item.getDurability();
		                        }
		                    } else {
		                    	auctionItemDamage = item.getDurability();
		                    }
		                }
		            }
		            
		        	for (ItemStack item : player.getInventory().getContents()) {
		                if (item.getTypeId() == id[0]) {
		                    MaterialData data = item.getData();
		                    
		                    if (id[1] != 0) {
		                        if (data.getData() == (byte)id[1]) {
		                            if(item.getDurability() == auctionItemDamage) {
		                                count++;
		                            }
		                        }
		                    } else {
		                        if(item.getDurability() == auctionItemDamage) {
		                            count++;
		                        }
		                    }
		                }
		            }
		        	
		            if (auctionItemAmount >= count) {
		            	auctionItemId = id[0];
		            	auction_item_byte = id[1];
		            	currentBid = auctionItemStarting;
		            	PlayerInventory inventory = player.getInventory();
		            
		            	if (auctionCheck(player, inventory, auctionItemId, auction_item_byte, auctionItemAmount, auctionTime, auctionItemStarting)) {
		            		isAuction = true;
		            		inventory.removeItem(new ItemStack[]{new ItemStack(auctionItemId, auctionItemAmount)});

		            		final int interval = auctionTime;
		            		final iAuction plug = this;

		            		auctionTT = new TimerTask() {
		            			int i = interval;
		            			double half = java.lang.Math.floor(i / 2);
		            			iAuction pl = plug;

		            			@Override
		            			public void run() {
		            				if (half <= 10) {
		            					if (i == interval || i == 10 || i == 3 || i == 2) {
		            						broadcast(auctionTimeColor + i + " seconds left to bid!");
		            					}
		            				} else {
		            					if (i == interval || i == half || i == 10 || i == 3 || i == 2) {
		            						broadcast(auctionTimeColor + i + " seconds left to bid!");
		            					}
		            				}
		                        	if (i == 1) {
		                        		broadcast(auctionTimeColor + i + " seconds left to bid!");
		                        	}
		                        	if (i == 0) {
		                        		pl.auctionStop(timerPlayer);
		                        	}
		                        	i--;
		            			}
		            		};
		            		broadcast(auctionStatusColor + "Auction Started!");
		            		auctionInfo(server, null);

		            		auctionTimer = new Timer();
		            		auctionTimer.scheduleAtFixedRate(auctionTT, 0L, 1000L);
		            	}
		            } else {
		                warn(player, "Sorry but you have only " + auctionTimeColor + count + warningColor + " of that item.");
		            }
    			} else {
    				warn(player, "Invalid syntax.");
    				help(player);
    			}
    		} else {
    			warn(player, "There is already an auction running!");
    		}
    	} else {
    		warn(player, "You don't have perrmisions to start an auction!");
    	}
    }

    public boolean auctionCheck(Player player, PlayerInventory inventory, int id, int data, int amount, int time, int price) {
        if (time > 10) {
            ItemStack[] stacks = inventory.getContents();
            int size = 0;
            for (int i = 0; i < stacks.length; i++) {
                if (stacks[i].getTypeId() == id) {
                	if(Items.isDamageable(id) == false || stacks[i].getDurability() == 0)
                		size += stacks[i].getAmount();
                }
            }
            if (amount <= size) {
                if (price >= 0) {
                    return true;
                } else {
                    warn(player, " The starting price has to be at least 0!");
                    return false;
                }
            } else {
                warn(player, "You don't have enough " + Items.name(id, data) + " to do that!");
                warn(player, "NOTE: You can't auction damaged tools.");
                return false;
            }
        } else {
            warn(player, "Time must be longer than 10 seconds!");
            return false;
        }
    }

    public void auctionInfo(Server server, Player player) {
        if (server != null) {
            broadcast(infoPrimaryColor + "Auctioned Item: " + infoSecondaryColor + Items.name(auctionItemId, auction_item_byte) + infoPrimaryColor + " [" + infoSecondaryColor + auctionItemId + infoPrimaryColor + "]");
            broadcast(infoPrimaryColor + "Amount: " + infoSecondaryColor + auctionItemAmount);
            broadcast(infoPrimaryColor + "Starting Price: " + infoSecondaryColor + auctionItemStarting + " " + this.iConomy.currency);
            broadcast(infoPrimaryColor + "Owner: " + infoSecondaryColor + auctionOwner.getDisplayName());
        }
        if (player != null) {
        	if (isAuction) {
                Messaging.send(player, tagColor + "-----[ " + auctionStatusColor + "Auction Information" + tagColor + " ]-----");
                Messaging.send(player, tag + infoPrimaryColor + "Auctioned Item: " + infoSecondaryColor + Items.name(auctionItemId, auction_item_byte) + infoPrimaryColor + " [" + infoSecondaryColor + auctionItemId + infoPrimaryColor + "]");
                Messaging.send(player, tag + infoPrimaryColor + "Amount: " + infoSecondaryColor + auctionItemAmount);
                Messaging.send(player, tag + infoPrimaryColor + "Current bid: " + infoSecondaryColor + currentBid + " " + this.iConomy.currency);
                Messaging.send(player, tag + infoPrimaryColor + "Owner: " + infoSecondaryColor + auctionOwner.getDisplayName());
                if (winner != null)
                    Messaging.send(player, tag + infoPrimaryColor + "Current Winner: " + infoSecondaryColor + winner.getDisplayName());
            } else
                warn(player, "No auctions in session at the moment!");
        }
    }

    public void auctionStop(Player player) {
    	if((permissionsEnabled && this.Permissions.has(player, "auction.end")) || player == auctionOwner || player.isOp())
    	{
    		if (isAuction) {
    			isAuction = false;
    			auctionTimer.cancel();

    			if (win) {
    				PlayerInventory winv = winner.getInventory();

    				broadcast(auctionStatusColor + "-- Auction Ended -- Winner [ " + winner.getDisplayName() + " ] -- ");
    				Messaging.send(winner, tag + auctionStatusColor + "Enjoy your items!");
    				Messaging.send(auctionOwner, tag + auctionStatusColor + "Your items have been sold for " + currentBid + " " + this.iConomy.currency + "!");

    				int balance = this.iConomy.db.get_balance(winner.getName());
    				balance -= currentBid;
    				this.iConomy.db.set_balance(winner.getName(), balance);
    				balance = this.iConomy.db.get_balance(auctionOwner.getName());
    				balance += currentBid;
    				this.iConomy.db.set_balance(auctionOwner.getName(), balance);
    				winv.addItem(new ItemStack[]{new ItemStack(auctionItemId, auctionItemAmount, auctionItemDamage, (byte)auction_item_byte)});
    			} else {
    				broadcast(auctionStatusColor + "-- Auction ended with no bids --");
    				Messaging.send(auctionOwner, tag + auctionStatusColor + "Your items have been returned to you!");
    				auctionOwner.getInventory().addItem(new ItemStack[]{new ItemStack(auctionItemId, auctionItemAmount, auctionItemDamage, (byte)auction_item_byte)});
    			}

    			auctionItemId = 0;
    			auctionItemAmount = 0;
    			auctionItemStarting = 0;
    			currentBid = 0;
    			auctionItemBid = 0;
    			winner = null;
    			auctionOwner = null;
    			win = false;
    		} else {
    			warn(player, "No auctions in session at the moment!");
    			return;
    		}
    	}
    	else
    		warn(player, "You have no perrmisions to stop that auction!");
    }

    public void auctionBid(Player player, String[] msg) {
    	if(!permissionsEnabled || !this.Permissions.has(player, "auction.bid")) {
    		if(msg.length == 2 || msg.length == 3) {
    			if(player != auctionOwner) {
    				String name = player.getName();
    		    	int balance = this.iConomy.db.get_balance(name);
    		    	int bid;
    		    	int sbid;
    		    	try {
    		    		bid = Integer.parseInt(msg[1]);
    		    		if(msg.length == 2)
    		    			sbid = 0;
    		    		else
    		    			sbid = Integer.parseInt(msg[2]);
    		    	} catch(NumberFormatException ex) {
    		            warn(player, " Invalid syntax.");
    		            help(player);
    		            return;
    		    	}
    		    	if(bid <= balance && sbid <= balance) {
    		            if (isAuction) {
    		                if (bid > currentBid) {
    		                    win = true;

    		                    if (bid > auctionItemBid) {
    		                        currentBid = bid;
    		                        auctionItemBid = sbid;
    		                        winner = player;
    		                        broadcast(auctionStatusColor + "Bid raised to " + auctionTimeColor + bid + " " + this.iConomy.currency + auctionStatusColor + " by " + auctionTimeColor + player.getDisplayName());
    		                    } else {
    		                        Messaging.send(player, tag + auctionStatusColor + "You have been outbid by " + auctionTimeColor + winner.getDisplayName() + auctionStatusColor + "'s secret bid!");
    		                        broadcast(auctionStatusColor + "Bid raised! Currently stands at: " + auctionTimeColor + (bid + 1) + " " + this.iConomy.currency );
    		                        currentBid = bid + 1;

    		                    }
    		                } else {
    		                    warn(player, "Your bid was too low.");
    		                }
    		            } else {
    		                warn(player, "There is no auction running at the moment.");
    		            }
    		    	} else {
    		    		warn(player, "You don't have enough money!");
    		    		warn(player, "Your current balance is: " + balance + " " + this.iConomy.currency);
    		    	}
    			} else {
    	    		warn(player, "You can't bid on your own auction!");
    			}
    		} else {
                warn(player, " Invalid syntax.");
                help(player);
    		}
    	} else {
    		warn(player, "You don't have perrmisions to bid an auction!");
    	}
    }
}