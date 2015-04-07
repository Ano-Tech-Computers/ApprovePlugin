package com.floyd.bukkit.approve;


import java.io.*;


import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;


import java.util.regex.*;
import java.sql.*;

/**
* Approve plugin for Bukkit
*
* @author FloydATC
*/
public class ApprovePlugin extends JavaPlugin {
    
    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

    public static DbPool dbpool = null;
    
    String baseDir = "plugins/ApprovePlugin";
    String configFile = "settings.txt";

	public static final Logger logger = Logger.getLogger("Minecraft.ApprovePlugin");
    
//    public ApprovePlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here
    	
        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	loadSettings();
    	initDbPool();
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        Connection dbh = null;
        PreparedStatement sth = null;
        Integer count = 0;
        
        if (cmdname.equalsIgnoreCase("approve")) {
        	// Help
        	if (args.length == 0 || args.length > 1) {
        		respond(player, "[SMF] Approve who? Expected a player name.");
        		return true;
        	}
        	if (args.length == 1) {
        		if (dbpool == null) {
        			logger.info("[SMF] Retrying dbpool initialization...");
        			initDbPool();
        		}
       	        if (dbpool != null) { 
       	        	dbh = dbpool.getConnection();
       	        	if (dbh != null) {
       	        		Player p = getServer().getPlayer(args[0]);
       	        		if (p != null) {
       	        			try {
								sth = dbh.prepareStatement(settings.get("db_query"));
	       	        			sth.setNString(1, p.getName());
	       	        			count = sth.executeUpdate();	// FIXME!!
							} catch (SQLException e) {
								e.printStackTrace();
								logger.warning("[SMF] SQL error: "+e.getLocalizedMessage());
							}
       	        			if (count > 0) {
   	        					respond(p, "[SMF] Your forum registration has been approved.");
       	        				if (player != p) {
           	        				respond(player, "[SMF] OK, "+p.getName()+" approved.");
       	        				}
       	        			} else {
       	        				respond(player, "[SMF] Failed, "+p.getName()+" is not waiting for approval. See '/help approve'");
       	        			}
       	        		} else {
                    		respond(player, "[SMF] Player must be online for approval, please check spelling and try again.");
       	        		}
       	        		dbpool.releaseConnection(dbh);
       	        	} else {
                		respond(player, "[SMF] Database not responding, please try again later.");
                		logger.warning("[SMF] Database not responding");       	        	
                	}
       	        } else {
            		respond(player, "[SMF] Database not available, please try again later.");
            		logger.warning("[SMF] Database not available");       	        	
            	}
        		return true;
        	}
        }

        return false;
    }

    private void initDbPool() {
    	try {
	    	dbpool = new DbPool(
	    		settings.get("db_url"), 
	    		settings.get("db_user"), 
	    		settings.get("db_pass"),
	    		Integer.valueOf(settings.get("db_min")),
	    		Integer.valueOf(settings.get("db_max"))
	    	);
    	} catch (RuntimeException e) {
    		logger.warning("[SMF] Init error: "+e.getLocalizedMessage());
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
    
    // Code from author of Permissions.jar
    
    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("db_url", "");
		settings.put("db_user", "");
		settings.put("db_pass", "");
		settings.put("db_min", "2");
		settings.put("db_max", "10");
		settings.put("db_query", "UPDATE members SET is_activated=1 WHERE memberName=? AND is_activated=3");
		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], pair[1]);
    			}
    		}
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "[SMF] Error reading " + e.getLocalizedMessage() + ", using defaults" );
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    
    private void respond(Player player, String message) {
    	if (player == null) {
        	// Strip color codes
        	Pattern pattern = Pattern.compile("\\§[0-9a-f]");
        	Matcher matcher = pattern.matcher(message);
        	message = matcher.replaceAll("");
        	// Print message to console
    		System.out.println(message);
    	} else {
    		player.sendMessage(message);
    	}
    }
    

}

