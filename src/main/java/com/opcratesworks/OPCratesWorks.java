package com.opcratesworks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class OPCratesWorks extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey typeKey, crateBlockKey;
    private final Random random = new Random();
    private final Map<UUID,String> editorCrate = new HashMap<>();
    private static final String EDITOR_PREFIX = "§5§lCRATE REWARD EDITOR";

    @Override public void onEnable() {
        saveDefaultConfig();
        typeKey = new NamespacedKey(this,"void_item");
        crateBlockKey = new NamespacedKey(this,"void_crate");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("opcrates")).setExecutor(this);
        getLogger().info("OPCratesWorks 2.0.0 enabled - normal vanilla items are untouched.");
    }

    private Component c(String s){ return LegacyComponentSerializer.legacySection().deserialize(s.replace('&','§')); }
    private ItemStack item(Material mat,String name,List<String> lore,String tag,int amount){
        ItemStack it=new ItemStack(mat,amount); ItemMeta m=it.getItemMeta();
        m.displayName(c(name)); m.lore(lore.stream().map(this::c).toList());
        m.getPersistentDataContainer().set(typeKey,PersistentDataType.STRING,tag); it.setItemMeta(m); return it;
    }
    private boolean tagged(ItemStack it,String tag){
        if(it==null||!it.hasItemMeta()) return false;
        String v=it.getItemMeta().getPersistentDataContainer().get(typeKey,PersistentDataType.STRING); return tag.equals(v);
    }
    public ItemStack key(int amount){ return item(Material.TRIPWIRE_HOOK,getConfig().getString("crate.key-name","&d&lVOID CRATE KEY"),getConfig().getStringList("crate.key-lore"),"key",amount); }
    public ItemStack crateItem(int amount){ return item(Material.SHULKER_BOX,getConfig().getString("crate.name","&5&lVOID CRATE"),List.of("&7Place this crate for players to open with a key."),"crate",amount); }

    public ItemStack saber(){
        ItemStack it=item(Material.NETHERITE_SWORD,"&d&lVOID LIGHTSABER",List.of("&7A separate custom weapon.","&5Damage: &f25","&5Unbreakable"),"saber",1);
        ItemMeta m=it.getItemMeta(); m.setUnbreakable(true); m.addEnchant(Enchantment.SHARPNESS,10,true); m.addEnchant(Enchantment.UNBREAKING,10,true); m.addEnchant(Enchantment.FIRE_ASPECT,3,true);
        try { m.setItemModel(new NamespacedKey("voidop","void_saber")); } catch(Exception ignored) {}
        it.setItemMeta(m); return it;
    }
    public ItemStack armor(String slot){
        Material mat=switch(slot){case "helmet"->Material.NETHERITE_HELMET;case "chest"->Material.NETHERITE_CHESTPLATE;case "legs"->Material.NETHERITE_LEGGINGS;default->Material.NETHERITE_BOOTS;};
        String name=switch(slot){case "helmet"->"&5&lVOID HELMET";case "chest"->"&5&lVOID CHESTPLATE";case "legs"->"&5&lVOID LEGGINGS";default->"&5&lVOID BOOTS";};
        ItemStack it=item(mat,name,List.of("&7Separate Void armor set.","&dStronger than Netherite","&8Normal armor is not modified."),"armor_"+slot,1); ItemMeta m=it.getItemMeta();
        m.setUnbreakable(true); m.addEnchant(Enchantment.PROTECTION,8,true); m.addEnchant(Enchantment.UNBREAKING,10,true);
        try { m.setItemModel(new NamespacedKey("voidop","void_"+slot)); } catch(Exception ignored) {}
        it.setItemMeta(m); return it;
    }
    private ItemStack filler(){ return new ItemStack(Material.PURPLE_STAINED_GLASS_PANE); }

    @EventHandler public void place(BlockPlaceEvent e){
        if(!tagged(e.getItemInHand(),"crate")) return;
        Block b=e.getBlockPlaced(); if(!(b.getState() instanceof TileState ts)) return;
        ts.getPersistentDataContainer().set(crateBlockKey,PersistentDataType.BYTE,(byte)1); ts.update(true,false);
        e.getPlayer().sendMessage(c("&d&lVoid Crate &7placed. Players need a key to open it."));
    }
    @EventHandler public void breakCrate(org.bukkit.event.block.BlockBreakEvent e){
        Block b=e.getBlock(); if(!(b.getState() instanceof TileState ts)) return;
        if(!ts.getPersistentDataContainer().has(crateBlockKey,PersistentDataType.BYTE)) return;
        e.setDropItems(false); b.getWorld().dropItemNaturally(b.getLocation().add(.5,.5,.5),crateItem(1));
    }
    @EventHandler public void interact(PlayerInteractEvent e){
        if(e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block b=e.getClickedBlock(); if(b==null) return;
        if(b.getState() instanceof TileState ts && ts.getPersistentDataContainer().has(crateBlockKey,PersistentDataType.BYTE)) {
            e.setCancelled(true); Player p=e.getPlayer(); ItemStack hand=e.getItem();
            if(!tagged(hand,"key")){ p.sendMessage(c("&cYou need a Void Crate Key.")); return; }
            if(hand.getAmount()<=1) p.getInventory().setItemInMainHand(null); else hand.setAmount(hand.getAmount()-1);
            openCrate(p,b.getLocation());
        }
    }
    private void openCrate(Player p,Location loc){
        Inventory inv=Bukkit.createInventory(null,27,c("&5&lVOID CRATE"));
        for(int i=0;i<27;i++) inv.setItem(i,filler());
        inv.setItem(13,new ItemStack(Material.ENDER_CHEST));
        inv.setItem(11,new ItemStack(Material.NETHER_STAR));
        inv.setItem(15,new ItemStack(Material.NETHER_STAR));
        inv.setItem(22,item(Material.TRIPWIRE_HOOK,"&d&lOPENING...",List.of("&7Your key was consumed."),"animation",1));
        p.openInventory(inv);
        Bukkit.getScheduler().runTaskLater(this,()->{ if(!p.isOnline())return; p.closeInventory(); ItemStack reward=rollReward(); if(reward!=null){ p.getInventory().addItem(reward); p.sendMessage(c("&d&lCRATE REWARD &8» &fYou won &d"+reward.getType().name()+" &fx"+reward.getAmount()+"&f!")); p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation().add(0,1,0),40,.4,.6,.4,.05); p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,1,1.2f); } },35L);
    }
    private ItemStack rollReward(){
        List<?> list=getConfig().getList("crate.rewards"); if(list==null||list.isEmpty()) return new ItemStack(Material.DIAMOND,1);
        double total=0; for(Object o:list) if(o instanceof Map<?,?> m) total+=Double.parseDouble(String.valueOf(m.getOrDefault("chance",0)));
        double r=random.nextDouble()*Math.max(total,1), acc=0; for(Object o:list){ if(!(o instanceof Map<?,?> m))continue; double ch=Double.parseDouble(String.valueOf(m.getOrDefault("chance",0))); acc+=ch; if(r<=acc){ String type=String.valueOf(m.getOrDefault("type","")); int amount=Integer.parseInt(String.valueOf(m.getOrDefault("amount",1))); if(type.equals("saber")) return copy(saber(),amount); if(type.equals("void_armor")) return copy(armor("chest"),amount); Material mat; try{mat=Material.valueOf(String.valueOf(m.getOrDefault("material","DIAMOND")));}catch(Exception ex){mat=Material.DIAMOND;} ItemStack it=new ItemStack(mat,Math.min(amount,mat.getMaxStackSize())); if(m.get("name")!=null){ItemMeta im=it.getItemMeta();im.displayName(c(String.valueOf(m.get("name"))));it.setItemMeta(im);} return it; }} return new ItemStack(Material.DIAMOND,1);
    }
    private ItemStack copy(ItemStack x,int amount){ItemStack n=x.clone(); n.setAmount(Math.min(amount,n.getMaxStackSize())); return n;}

    private void editor(Player p){ editorCrate.put(p.getUniqueId(),"default"); Inventory inv=Bukkit.createInventory(null,54,c(EDITOR_PREFIX+" &8- &fdefault"));
        for(int i=0;i<54;i++)inv.setItem(i,filler()); List<?> list=getConfig().getList("crate.rewards"); int slot=10; if(list!=null) for(Object o:list){ if(!(o instanceof Map<?,?> m)||slot>=44)continue; String type=String.valueOf(m.getOrDefault("type",m.getOrDefault("material","DIAMOND"))); ItemStack icon; if(type.equals("saber"))icon=saber(); else if(type.equals("void_armor"))icon=armor("chest"); else {Material mat;try{mat=Material.valueOf(type);}catch(Exception ex){mat=Material.DIAMOND;}icon=new ItemStack(mat,Integer.parseInt(String.valueOf(m.getOrDefault("amount",1))));} ItemMeta im=icon.getItemMeta(); List<String> l=new ArrayList<>(); l.add("&7Chance: &d"+m.getOrDefault("chance",0)+"%"); l.add("&8Left-click: edit chance"); l.add("&8Right-click: remove"); im.lore(l.stream().map(this::c).toList()); icon.setItemMeta(im); inv.setItem(slot,icon); slot++; }
        inv.setItem(49,item(Material.ANVIL,"&a&lSAVE / RELOAD",List.of("&7Saves current config."),"save",1)); inv.setItem(48,item(Material.HOPPER,"&b&lADD HELPER REWARD",List.of("&7Adds a diamond reward."),"add",1)); p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return; String title=LegacyComponentSerializer.legacySection().serialize(e.getView().title()); if(!title.startsWith(EDITOR_PREFIX)&&!title.contains("VOID CRATE"))return; e.setCancelled(true);
        if(title.startsWith(EDITOR_PREFIX)){ ItemStack it=e.getCurrentItem(); if(tagged(it,"save")){persist();p.sendMessage(c("&aCrate rewards saved."));p.closeInventory();} else if(tagged(it,"add")){int n=getConfig().getMapList("crate.rewards").size(); List<Map<String,Object>> l=getConfig().getMapList("crate.rewards"); Map<String,Object> m=new LinkedHashMap<>();m.put("material","DIAMOND");m.put("amount",1);m.put("chance",5.0);l.add(m);getConfig().set("crate.rewards",l);persist();editor(p);} else if(it!=null && it.hasItemMeta()){int slot=e.getRawSlot(); if(slot>=10&&slot<44){int idx=slot-10;List<Map<String,Object>> l=getConfig().getMapList("crate.rewards");if(idx<l.size()&&e.isRightClick()){l.remove(idx);getConfig().set("crate.rewards",l);persist();editor(p);}}} return;}
    }
    private void persist(){super.saveConfig();reloadConfig();}

    @EventHandler public void armorEffects(org.bukkit.event.player.PlayerItemHeldEvent e){}
    @EventHandler public void join(org.bukkit.event.player.PlayerJoinEvent e){applyVoidEffects(e.getPlayer());}
    @EventHandler public void move(org.bukkit.event.player.PlayerMoveEvent e){ if(e.getFrom().getBlockX()!=e.getTo().getBlockX()||e.getFrom().getBlockY()!=e.getTo().getBlockY()||e.getFrom().getBlockZ()!=e.getTo().getBlockZ()) applyVoidEffects(e.getPlayer()); }
    private void applyVoidEffects(Player p){
        ItemStack h=p.getInventory().getHelmet(),ch=p.getInventory().getChestplate,l=p.getInventory().getLeggings,b=p.getInventory().getBoots();
        boolean full=tagged(h,"armor_helmet")&&tagged(ch,"armor_chest")&&tagged(l,"armor_legs")&&tagged(b,"armor_boots");
        if(full){ p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE,50,1,true,false)); p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH,50,1,true,false)); p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,50,1,true,false)); p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,50,0,true,false)); }
    }

    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){ if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;} if(a.length==0){p.sendMessage(c("&d/opcrates key|crate|saber|armor|editor|reload"));return true;} switch(a[0].toLowerCase()){case "key"->p.getInventory().addItem(key(a.length>1?Integer.parseInt(a[1]):1));case "crate"->p.getInventory().addItem(crateItem(a.length>1?Integer.parseInt(a[1]):1));case "saber"->p.getInventory().addItem(saber());case "armor"->{p.getInventory().addItem(armor("helmet"));p.getInventory().addItem(armor("chest"));p.getInventory().addItem(armor("legs"));p.getInventory().addItem(armor("boots"));}case "editor"->editor(p);case "reload"->reloadConfig();} return true; }
}
