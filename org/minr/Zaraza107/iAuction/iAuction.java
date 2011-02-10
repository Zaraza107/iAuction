package org.minr.Zaraza107.iAuction;

import com.nijiko.permissions.PermissionHandler;
import java.io.*;
import java.util.HashMap;
import java.lang.String;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Listener;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.*;
import org.bukkit.command.*;
import com.nijikokun.bukkit.iConomy.iConomy;
import com.nijikokun.bukkit.iConomy.Messaging;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.util.Map;

/**
 * iAuction for iConomy for Bukkit
 *
 * @author Zaraza107
 */
public class iAuction extends JavaPlugin {

    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    public static Server server;
    public String currency = "";
    private Timer auctionTimer;
    private TimerTask auctionTT;
    public boolean isAuction = false;
    public int auction_time;
    public Player auction_owner;
    public Player winner;
    private int auction_item_id;
    private short auction_item_damage;
    private int auction_item_byte;
    private int auction_item_amount;
    private int auction_item_starting;
    private int auction_item_bid;
    private boolean win = false;
    private int currentBid;
    public Player timer_player;
    public String tag;
    public iConomy iConomy;
    public PermissionHandler Permissions;
    private boolean permissionsEnabled = false;
    
    private int maxTime;
    
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

    public iAuction(PluginLoader pluginLoader, Server instance,
            PluginDescriptionFile desc, File folder, File plugin,
            ClassLoader cLoader) throws IOException {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);

        server = instance;
    }

    public void onEnable() {
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

    public void auctionStart(Player player, String[] msg) {
    	if(permissionsEnabled && !this.Permissions.has(player, "auction.allow"))
    	{
    		Messaging.send(player, tag + warningColor + "You don't have perrmisions to start an auction!");
    		return;
    	}
    	if(isAuction)
    	{
    		Messaging.send(player, tag + warningColor + "There is already an auction running!");
    		return;
    	}
    	if(msg.length != 5) {
            Messaging.send(player, tag + warningColor + "Invalid syntax.");
            Messaging.send(player, warningColor + "/auction start|s <time> <item> <amount> <starting price>");
            return;
        }
        auction_owner = player;
        timer_player = player;
        int[] id = new int[]{-1, 0};
        int count = 0;
        try {
        	auction_time = Integer.parseInt(msg[1]);
        	auction_item_amount = Integer.parseInt(msg[3]);
            auction_item_starting = Integer.parseInt(msg[4]);
        } catch (NumberFormatException ex) {
            Messaging.send(player, tag + warningColor + "Invalid syntax.");
            Messaging.send(player, warningColor + "/auction start|s <time> <item> <amount> <starting price>");
            return;
        }
        
        if(maxTime != 0 && auction_time > maxTime)
        {
        	Messaging.send(player, tag + warningColor + "Too long! Maximal time is " + maxTime);
        	return;
        }

        id = Items.validate(msg[2]);

        if (id[0] == -1 || id[0] == 0) {
            Messaging.send(tag + warningColor + "Invalid item id."); return;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item.getTypeId() == id[0]) {
                MaterialData data = item.getData();

                if(id[1] != 0) {
                    if (data.getData() == (byte)id[1]) {
                    	auction_item_damage = item.getDurability();
                    }
                } else {
                	auction_item_damage = item.getDurability();
                }
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item.getTypeId() == id[0]) {
                MaterialData data = item.getData();
                
                if (id[1] != 0) {
                    if (data.getData() == (byte)id[1]) {
                        if(item.getDurability() == auction_item_damage) {
                            count++;
                        }
                    }
                } else {
                    if(item.getDurability() == auction_item_damage) {
                        count++;
                    }
                }
            }
        }
        
        if (auction_item_amount < count) {
            Messaging.send(tag + warningColor + "Sorry but you have only " + auctionTimeColor + count + warningColor + " of that item."); return;
        }
        auction_item_id = id[0];
        auction_item_byte = id[1];
        currentBid = auction_item_starting;
        PlayerInventory inventory = player.getInventory();

        if (auctionCheck(player, inventory, auction_item_id, auction_item_byte, auction_item_amount, auction_time, auction_item_starting)) {
            isAuction = true;
            inventory.removeItem(new ItemStack[]{new ItemStack(auction_item_id, auction_item_amount)});

            final int interval = auction_time;
            final iAuction plug = this;

            auctionTT = new TimerTask() {

                int i = interval;
                double half = java.lang.Math.floor(i / 2);
                iAuction pl = plug;

                @Override
                public void run() {
                    if (half <= 10) {
                        if (i == interval || i == 10 || i == 3 || i == 2) {
                            Messaging.broadcast(tag + auctionTimeColor + i + " seconds left to bid!");
                        }
                    } else {
                        if (i == interval || i == half || i == 10 || i == 3 || i == 2) {
                             Messaging.broadcast(tag + auctionTimeColor + i + " seconds left to bid!");
                        }
                    }
                    if (i == 1) {
                        Messaging.broadcast(tag + auctionTimeColor + i + " seconds left to bid!");
                    }
                    if (i == 0) {
                        pl.auctionStop(timer_player);
                    }
                    i--;
                }
            };
            Messaging.broadcast(tag + auctionStatusColor + "Auction Started!");
            auctionInfo(server, null);

            auctionTimer = new Timer();
            auctionTimer.scheduleAtFixedRate(auctionTT, 0L, 1000L);
        }
    }

    public boolean auctionCheck(Player player, PlayerInventory inventory, int id, int data, int amount, int time, int price) {
        if (time > 10) {
            ItemStack[] stacks = inventory.getContents();
            int size = 0;
            for (int i = 0; i < stacks.length; i++) {
                if (stacks[i].getTypeId() == id) {
                	if(isTool(id) == false || stacks[i].getDurability() == 0)
                		size += stacks[i].getAmount();
                }
            }
            if (amount <= size) {
                if (price >= 0) {
                    return true;
                } else {
                    Messaging.send(player, tag + warningColor + " The starting price has to be at least 0!");
                    return false;
                }
            } else {
                Messaging.send(player, tag + warningColor + "You don't have enough " + Items.name(id, data) + " to do that!");
                Messaging.send(player, warningColor + "NOTE: You can't auction damaged tools.");
                return false;
            }
        } else {
            Messaging.send(player, tag + warningColor + "Time must be longer than 10 seconds!");
            return false;
        }
    }
    
    public boolean isTool(int id)
    {
    	if((id > 255 && id < 260) || (id > 266 && id < 280) || (id > 282 && id < 287) || (id > 289 && id < 295) || (id > 297 && id < 318))
    		return true;
    	else
    		return false;
    }

    public void auctionInfo(Server server, Player player) {
        if (server != null) {
            Messaging.broadcast(tag + infoPrimaryColor + "Auctioned Item: " + infoSecondaryColor + Items.name(auction_item_id, auction_item_byte) + infoPrimaryColor + " [" + infoSecondaryColor + auction_item_id + infoPrimaryColor + "]");
            Messaging.broadcast(tag + infoPrimaryColor + "Amount: " + infoSecondaryColor + auction_item_amount);
            Messaging.broadcast(tag + infoPrimaryColor + "Starting Price: " + infoSecondaryColor + auction_item_starting + " " + this.iConomy.currency);
            Messaging.broadcast(tag + infoPrimaryColor + "Owner: " + infoSecondaryColor + auction_owner.getDisplayName());
        }
        if (player != null) {
        	if (isAuction) {
                Messaging.send(player, tagColor + "-----[ " + auctionStatusColor + "Auction Information" + tagColor + " ]-----");
                Messaging.send(player, tag + infoPrimaryColor + "Auctioned Item: " + infoSecondaryColor + Items.name(auction_item_id, auction_item_byte) + infoPrimaryColor + " [" + infoSecondaryColor + auction_item_id + infoPrimaryColor + "]");
                Messaging.send(player, tag + infoPrimaryColor + "Amount: " + infoSecondaryColor + auction_item_amount);
                Messaging.send(player, tag + infoPrimaryColor + "Current bid: " + infoSecondaryColor + currentBid + " " + this.iConomy.currency);
                Messaging.send(player, tag + infoPrimaryColor + "Owner: " + infoSecondaryColor + auction_owner.getDisplayName());
                if (winner != null)
                    Messaging.send(player, tag + infoPrimaryColor + "Current Winner: " + infoSecondaryColor + winner.getDisplayName());
            } else
                Messaging.send(player, tag + warningColor + "No auctions in session at the moment!");
        }
    }

    public void auctionStop(Player player) {
    	if((permissionsEnabled && this.Permissions.has(player, "auction.end")) || player == auction_owner || player.isOp())
    	{
    		if (!isAuction) {
    			Messaging.send(player, tag + warningColor + "No auctions in session at the moment!");
    			return;
    		} else {
    			isAuction = false;
    			auctionTimer.cancel();

    			if (win) {
    				PlayerInventory winv = winner.getInventory();

    				Messaging.broadcast(auctionStatusColor + "-- Auction Ended -- Winner [ " + winner.getDisplayName() + " ] -- ");
    				Messaging.send(winner, tag + auctionStatusColor + "Enjoy your items!");
    				Messaging.send(auction_owner, tag + auctionStatusColor + "Your items have been sold for " + currentBid + " " + this.iConomy.currency + "!");

    				int balance = this.iConomy.db.get_balance(winner.getName());
    				balance -= currentBid;
    				this.iConomy.db.set_balance(winner.getName(), balance);
    				balance = this.iConomy.db.get_balance(auction_owner.getName());
    				balance += currentBid;
    				this.iConomy.db.set_balance(auction_owner.getName(), balance);
    				winv.addItem(new ItemStack[]{new ItemStack(auction_item_id, auction_item_amount, auction_item_damage, (byte)auction_item_byte)});
    			} else {
    				Messaging.broadcast("&2 -- Auction ended with no bids --");
    				Messaging.send(auction_owner, tag + auctionStatusColor + "Your items have been returned to you!");
    				auction_owner.getInventory().addItem(new ItemStack[]{new ItemStack(auction_item_id, auction_item_amount, auction_item_damage, (byte)auction_item_byte)});
    			}

    			auction_item_id = 0;
    			auction_item_amount = 0;
    			auction_item_starting = 0;
    			currentBid = 0;
    			auction_item_bid = 0;
    			winner = null;
    			auction_owner = null;
    			win = false;
    		}
    	}
    	else
    		Messaging.send(player, tag + warningColor + "You have no perrmisions to stop that auction!");
    }

    public void auctionBid(Player player, String[] msg) {
    	if(permissionsEnabled && !this.Permissions.has(player, "auction.allow"))
    	{
    		Messaging.send(player, tag + warningColor + "You don't have perrmisions to bid an auction!");
    		return;
    	}
    	if(msg.length != 2 && msg.length != 3)
    	{
            Messaging.send(player, tag + warningColor + " Invalid syntax.");
            Messaging.send(player, warningColor + "/auction bid|b <bid> (maximum bid)");
            return;
    	}
    	if(player == auction_owner)
    	{
    		Messaging.send(player, tag + warningColor + "You can't bid on your own auction!");
    		return;
    	}
    	
        String name = player.getName();
    	int balance = this.iConomy.db.get_balance(name);
    	int bid;
    	try {
    		bid = Integer.parseInt(msg[1]);
    	} catch(NumberFormatException ex) {
            Messaging.send(player, tag + warningColor + " Invalid syntax.");
            Messaging.send(player, warningColor + "/auction bid|b <bid> (maximum bid)");
            return;
    	}
    	int sbid;
    	
    	if(msg.length == 2)
    		sbid = 0;
    	else
    		try {
    			sbid = Integer.parseInt(msg[2]);
    		} catch (NumberFormatException ex) {
                Messaging.send(player, tag + warningColor + " Invalid syntax.");
                Messaging.send(player, warningColor + "/auction bid|b <bid> (maximum bid)");
                return;
    		}
    	if(bid > balance || sbid > balance)
    	{
    		Messaging.send(player, tag + warningColor + "You don't have enough money!");
    		Messaging.send(player, warningColor + "Your current balance is: " + balance + " " + this.iConomy.currency);
    		return;
    	}
        
        if (isAuction) {
            if (bid > currentBid) {
                win = true;

                if (bid > auction_item_bid) {
                    currentBid = bid;
                    auction_item_bid = sbid;
                    winner = player;
                    Messaging.broadcast(tag + auctionStatusColor + "Bid raised to " + auctionTimeColor + bid + " " + this.iConomy.currency + auctionStatusColor + " by " + auctionTimeColor + player.getDisplayName());
                } else {
                    Messaging.send(player, tag + auctionStatusColor + "You have been outbid by " + auctionTimeColor + winner.getDisplayName() + auctionStatusColor + "'s secret bid!");
                    Messaging.broadcast(tag + auctionStatusColor + "Bid raised! Currently stands at: " + auctionTimeColor + (bid + 1) + " " + this.iConomy.currency );
                    currentBid = bid + 1;

                }
            } else {
                Messaging.send(player, tag + warningColor + "Your bid was too low.");
            }
        } else {
            Messaging.send(player, tag + warningColor + "There is no auction running at the moment.");
        }
    }
}