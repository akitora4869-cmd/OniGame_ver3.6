package jp.muimi.onigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;
import java.time.Duration;

public final class OniGamePlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private GameState state=GameState.WAITING;
    private final Set<UUID> participants=new HashSet<>(), players=new HashSet<>(), escaped=new HashSet<>(), dead=new HashSet<>(), playerBots=new HashSet<>(), escapedPlayerBots=new HashSet<>(), deadPlayerBots=new HashSet<>();
    private final Map<UUID,PlayerSkill> selectedSkill=new HashMap<>();
    private final Map<UUID,String> selectedPresetNames=new HashMap<>();
    private final Map<UUID,LinkedHashSet<PassiveSkill>> selectedPassives=new HashMap<>();
    private final LinkedHashSet<OniPassiveSkill> selectedOniPassives=new LinkedHashSet<>();
    private final LinkedHashSet<OniPassiveSkill> activeOniPassives=new LinkedHashSet<>();
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Map<String,Integer> heartHp=new HashMap<>();
    private final Map<String,Material> fakeHeartOriginals=new HashMap<>();
    private final Set<String> fakeHeartKeys=new HashSet<>();

    private final Map<String,Long> heartAlertAt=new HashMap<>();
    private final Map<UUID,String> repairingHeart=new HashMap<>();
    private final Map<UUID,Long> repairProgressAt=new HashMap<>(), nextSkillCheckAt=new HashMap<>();
    // Queue lobby utility GUIs before protection plugins can swallow the interaction.
    private final Set<UUID> lobbyUtilityGuiQueued=new HashSet<>();
    private final Map<UUID,Long> playerHitGraceUntil=new HashMap<>(), downEscapeUntil=new HashMap<>();
    private final Set<UUID> extraLifeConsumed=new HashSet<>();
    private final Map<UUID,ItemStack[]> playerBotEscapeEquipment=new HashMap<>();
    private final Map<UUID,Double> heartProgressCarry=new HashMap<>();
    private final Map<UUID,Integer> doubleStakesGreatStreak=new HashMap<>();
    private final Map<UUID,SkillCheck> skillChecks=new HashMap<>();
    private final Map<UUID,String> playerBotWorkingHeart=new HashMap<>();
    private final Map<UUID,Long> nextPlayerBotSkillCheck=new HashMap<>();
    private final Map<UUID,Long> playerBotNextAttackAt=new HashMap<>();
    private final Map<UUID,PlayerSkill> playerBotSkills=new HashMap<>();
    private final Map<UUID,LinkedHashSet<PassiveSkill>> playerBotPassives=new HashMap<>();
    private final Map<UUID,String> playerBotPresetNames=new HashMap<>();
    private final Map<UUID,Long> playerBotSkillReadyAt=new HashMap<>(), playerBotSprintUntil=new HashMap<>();
    private UUID azakujiAllyId;
    private long azakujiFightUntil, azakujiEscapeUntil, azakujiTauntUntil, azakujiNextLeapAt, azakujiNextMeleeAt, azakujiLastCombatAt;

    private final Map<UUID,Double> playerBotStamina=new HashMap<>();
    private final Map<UUID,String> playerBotTargetHeart=new HashMap<>();
    private final Map<UUID,Location> playerBotLastLocation=new HashMap<>();
    private final Map<UUID,Integer> playerBotStuckTicks=new HashMap<>();
    private final Map<UUID,Location> safetyLastLocation=new HashMap<>(), safetyLastGoodLocation=new HashMap<>();
    private final Map<UUID,Integer> safetyStuckChecks=new HashMap<>();
    private final Map<UUID,Long> safetyRescueCooldownUntil=new HashMap<>(), safetyAbuseWindowStartedAt=new HashMap<>(), safetyAbuseLockUntil=new HashMap<>(), lastCombatAt=new HashMap<>();
    private final Map<UUID,Integer> safetyAbuseAttempts=new HashMap<>();
    private final Map<UUID,BotSkillCheck> playerBotSkillChecks=new HashMap<>();
    private final Map<UUID,HealingTask> healingTasks=new HashMap<>();
    private final Map<UUID,Long> lastSeenAt=new HashMap<>(), heartbeatAt=new HashMap<>();
    private final Map<UUID,Double> stamina=new HashMap<>();
    private final Map<UUID,Long> dashJumpPenaltyAt=new HashMap<>();
    private final Map<UUID,Long> passiveLeapReadyAt=new HashMap<>();
    private final Map<UUID,Long> sneakStartedAt=new HashMap<>(),parrySneakAt=new HashMap<>(),repairStartedAt=new HashMap<>(),reversalAt=new HashMap<>(),fakeNoiseUntil=new HashMap<>();
    private final Map<String,Long> technicalPassiveReadyAt=new HashMap<>();
    private final Map<String,Long> timedSkillUntil=new HashMap<>();
    private final Map<UUID,Location> sealingCircles=new HashMap<>();
    private final Map<UUID,Long> sealingCircleUntil=new HashMap<>();
    private final Map<UUID,Vector> lastMoveDirection=new HashMap<>();
    private final Map<UUID,Boolean> wasSprinting=new HashMap<>();
    private final Set<UUID> aftermindPrimed=new HashSet<>();
    private final Set<UUID> trainingPlayers=new HashSet<>();
    private UUID trainingDummy;
    private final Set<UUID> chased=new HashSet<>();
    private final Set<UUID> predationTriggered=new HashSet<>();
    private final Set<UUID> heavenlyArrival=new HashSet<>();
    private final Set<UUID> bugeiActive=new HashSet<>(); private final Set<UUID> jakutsukiPierceCharging=new HashSet<>();
    private final Map<UUID,Location> blackMirrorLocations=new HashMap<>();
    private final Map<UUID,UUID> blackMirrorEntities=new HashMap<>();
    private final Set<UUID> jakutsukiSnakes=new HashSet<>();
    private long exitSealedUntil=0L;
    private final Map<String,Material> temporaryTestHeartOriginals=new HashMap<>();
    private final Map<String,Material> temporaryLootChestOriginals=new HashMap<>();
    private final Set<String> lootChestKeys=new HashSet<>();
    private final Map<UUID,Set<String>> openedLootChests=new HashMap<>();
    private final Map<String,Inventory> personalLootInventories=new HashMap<>();
    private final Map<UUID,ChestOpeningTask> chestOpeningTasks=new HashMap<>();
    private UUID oni, testParticipant, oniBot, forcedOni, configuredFriendlyNpcId, oniBotTacticalTarget; private OniType oniType=OniType.DAKKO, selectedOniType; private int brokenHearts, totalHearts, secondsLeft; private BukkitTask ticker, sidebarTicker, chaseTicker, repairTicker, staminaTicker, botTicker, bgmTicker, gameStartBgmTicker, dawnTicker, crimsonTicker, finalEffectTestTicker; private UUID finalEffectTestPlayer; private boolean finalPhase, playerSideTest; private String activeBgmSound, activeGameStartBgmSound;
    private final Set<UUID> dakkoCloneBots=new HashSet<>();
    private final Set<UUID> eliteBotMatchIds=new HashSet<>();
    private final Set<UUID> lobbyGuideDisplays=new HashSet<>();
    private final Map<UUID,Long> dakkoCloneExpiresAt=new HashMap<>(), dakkoCloneNextAttackAt=new HashMap<>();
    private final Set<UUID> chaseBgmPlayers=new HashSet<>();
    private final Map<UUID,BukkitTask> chaseReleaseTasks=new HashMap<>();
    private final Map<UUID,List<BukkitTask>> normalBgmFadeTasks=new HashMap<>();
    private Location activeLobby, activeExit, activePlayerSpawn; private long botNextAbilityAt, botNextMeleeAt, botRestUntil, oniChaseStartedAt, nextBloodScentAt; private double botStamina=20; private int botDakkoSkillCycle;
    private SavedWorldBorder savedTestWorldBorder;
    private NamespacedKey actionKey;
    private record SkillCheck(long startedAt,long targetAt,long endsAt){}
    private record BotSkillCheck(String heartKey,long resolveAt,boolean success,boolean great){}
    private record HealingTask(Location start,long startedAt,long endsAt,double amount,String consumableAction){}
    private record ChestOpeningTask(String chestKey,Location start,long startedAt,long endsAt){}
    private record TestArenaLayout(Location playerSpawn,Location oniSpawn,Location exit,List<Location> hearts,List<Location> lootChests){}
    private record SavedWorldBorder(World world,Location center,double size,int warningDistance,int warningTime,double damageAmount,double damageBuffer){}
    private static final class MenuHolder implements InventoryHolder{private final String type;private Inventory inventory;private MenuHolder(String type){this.type=type;}@Override public Inventory getInventory(){return inventory;}}

    // v0.36.1: token passive runtime state
    private final Map<UUID,Integer> threePhaseMask=new HashMap<>();
    private final Map<UUID,Integer> footstepTokens=new HashMap<>();
    private final Map<UUID,Integer> deadlineTokens=new HashMap<>();
    private final Map<UUID,Integer> bloodMarkTokens=new HashMap<>();
    private final Map<UUID,Long> footstepNextGainAt=new HashMap<>();
    private final Map<UUID,Long> deadlineNearSince=new HashMap<>();
    private final Map<UUID,Long> deadlineNextGainAt=new HashMap<>();
    private final Set<UUID> threePhaseReady=new HashSet<>();
    private final Set<UUID> huntRecordVictims=new HashSet<>();
    private final Set<UUID> grudgeResistReady=new HashSet<>();
    private final Map<UUID,Integer> grudgeTokens=new HashMap<>();
    private final Map<UUID,Long> oniChaseLostAt=new HashMap<>(), oniLastHitAt=new HashMap<>();
    private final Map<UUID,Location> oniScentTrailLocation=new HashMap<>();
    private final Map<UUID,Long> oniScentTrailUntil=new HashMap<>(), oniSkillSealUntil=new HashMap<>();
    private final Map<UUID,Integer> oniBloodFrenzyHits=new HashMap<>(), oniDisruptionStacks=new HashMap<>();
    private long oniLastAnyChaseAt=0L, oniHuntersInstinctAt=0L, oniHuntingGroundSince=0L;
    private Location oniHuntingGroundAnchor;

    @Override public void onEnable(){saveDefaultConfig(); String forcedOniRaw=getConfig().getString("forced-oni-uuid","");if(forcedOniRaw!=null&&!forcedOniRaw.isBlank())try{forcedOni=UUID.fromString(forcedOniRaw);}catch(IllegalArgumentException ignored){forcedOni=null;} actionKey=new NamespacedKey(this,"action"); getServer().getPluginManager().registerEvents(this,this); PluginCommand c=getCommand("onigame"); c.setExecutor(this); c.setTabCompleter(this); restoreLobbyUtilities();refreshLobbyGuideDisplays();sidebarTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateSidebars,1,20); chaseTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateHorrorState,5,5);repairTicker=Bukkit.getScheduler().runTaskTimer(this,this::updatePlayerActions,2,2);staminaTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateStamina,5,5);botTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateBots,10,10);getLogger().info("鬼げぇむ v0.36.3 enabled");}
    @Override public void onDisable(){removeLobbyGuideDisplays();clearFinalEffectTest();if(ticker!=null)ticker.cancel();if(dawnTicker!=null)dawnTicker.cancel();if(crimsonTicker!=null)crimsonTicker.cancel();resetFinalCrimsonVisuals();if(sidebarTicker!=null)sidebarTicker.cancel();if(chaseTicker!=null)chaseTicker.cancel();if(repairTicker!=null)repairTicker.cancel();if(staminaTicker!=null)staminaTicker.cancel();if(botTicker!=null)botTicker.cancel();for(UUID id:new HashSet<>(fakeNoiseUntil.keySet())){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}fakeNoiseUntil.clear();for(UUID id:new HashSet<>(trainingPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)removeTrainingItems(p);}trainingPlayers.clear();removeTrainingDummy();removeBlackMirrors();removeJakutsukiSnakes();removeDakkoCloneBots();removeOniBot();restoreFakeHearts();restoreTemporaryTestHearts();restoreLootChests();restoreTestWorldBorder();restoreLobbyUtilities();}

    private String cc(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    private void msg(CommandSender s,String m){s.sendMessage(cc("&8[&c鬼げぇむ&8] &f"+m));}
    private void all(String m){Bukkit.broadcastMessage(cc("&8[&c鬼げぇむ&8] &f"+m));}
    private boolean isAzakujiLocked(){return getConfig().getBoolean("content-locks.azakuji-hiro",false);}
    private boolean isJakutsukiLocked(){return getConfig().getBoolean("content-locks.jakutsuki",false);}
    private boolean isOniTypeLocked(OniType type){return type==OniType.JAKUTSUKI&&isJakutsukiLocked();}
    private int oniSkillUnlockHearts(String action){
        return switch(action){
            case "dakko_clone"->getConfig().getInt("dakko-skills.clone.unlock-after-broken-hearts",1);
            case "dakko_fox_fire"->getConfig().getInt("dakko-skills.fox-fire.unlock-after-broken-hearts",2);
            case "dakko_heavenly_arrival"->getConfig().getInt("dakko-skills.heavenly-arrival.unlock-after-broken-hearts",3);
            case "kishin_slam"->getConfig().getInt("kishin-skills.slam.unlock-after-broken-hearts",1);
            case "kishin_roar"->getConfig().getInt("kishin-skills.roar.unlock-after-broken-hearts",2);
            case "kishin_iron_body"->getConfig().getInt("kishin-skills.iron-body.unlock-after-broken-hearts",3);
            default->0;
        };
    }
    private boolean oniSkillUnlocked(String action){return brokenHearts>=oniSkillUnlockHearts(action);}
    private void refreshOniUnlockedSkills(Player p,boolean announce){
        if(p==null)return;
        if(oniType==OniType.DAKKO){
            if(oniSkillUnlocked("dakko_clone")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("dakko_clone","&6分霊","dakko_clone"));if(announce)msg(p,"&6分霊 &fが解放された！");}
            if(oniSkillUnlocked("dakko_fox_fire")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("dakko_fox_fire","&6&l狐火","dakko_fox_fire"));if(announce)msg(p,"&6狐火 &fが解放された！");}
            if(oniSkillUnlocked("dakko_heavenly_arrival")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("dakko_heavenly_arrival","&d&l天来","dakko_heavenly_arrival"));if(announce)msg(p,"&d天来 &fが解放された！");}
        }else if(oniType==OniType.KISHIN){
            if(oniSkillUnlocked("kishin_slam")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("kishin_slam","&4地砕","kishin_slam"));if(announce)msg(p,"&4地砕 &fが解放された！");}
            if(oniSkillUnlocked("kishin_roar")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("kishin_roar","&4&l鬼吼","kishin_roar"));if(announce)msg(p,"&4鬼吼 &fが解放された！");}
            if(oniSkillUnlocked("kishin_iron_body")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("kishin_iron_body","&6&l剛身","kishin_iron_body"));if(announce)msg(p,"&6剛身 &fが解放された！");}
        }
    }
    private boolean ensureOniUnlocked(CommandSender sender,OniType type){if(!isOniTypeLocked(type))return true;msg(sender,"&8EX鬼 蛇窟姫（ジャクツキ） &fは現在ロックされています。");return false;}
    private void resetLockedAzakujiSelections(){
        for(UUID id:new HashSet<>(selectedPresetNames.keySet()))if("AZAKUJI_HIRO".equals(selectedPresetNames.get(id))){selectedPresetNames.remove(id);selectedSkill.put(id,PlayerSkill.SPRINT);LinkedHashSet<PassiveSkill> ps=selectedPassives.computeIfAbsent(id,k->new LinkedHashSet<>());ps.clear();ps.add(PassiveSkill.LIGHT_FOOTED);ps.add(PassiveSkill.DEEP_BREATH);Player p=Bukkit.getPlayer(id);if(p!=null)msg(p,"&7字九字ひろがロックされたため、構成を通常プリセット相当へ戻しました。");}
    }

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] a){
        if(a.length==0||a[0].equalsIgnoreCase("help")){help(sender);return true;}
        if(a[0].equalsIgnoreCase("join")&&sender instanceof Player p){if(state!=GameState.WAITING){msg(p,"試合中は参加できません。");return true;} participants.add(p.getUniqueId()); selectedSkill.putIfAbsent(p.getUniqueId(),PlayerSkill.SPRINT);msg(p,"参加しました。現在 &e"+participants.size()+"人 &7――ロビーのスキル設定チェストから構成を選べます。");return true;}
        if(a[0].equalsIgnoreCase("leave")&&sender instanceof Player p){participants.remove(p.getUniqueId());selectedSkill.remove(p.getUniqueId());selectedPresetNames.remove(p.getUniqueId());msg(p,"参加を取り消しました。");return true;}
        if(a[0].equalsIgnoreCase("skill")&&sender instanceof Player p){PlayerSkill sk=a.length>1?PlayerSkill.parse(a[1]):null;if(sk==null){msg(p,"sprint / invisible / smoke / strike / heal / obsession / landing から選択してください。");return true;}selectedSkill.put(p.getUniqueId(),sk);selectedPresetNames.remove(p.getUniqueId());msg(p,"スキルを &b"+sk.display+" &fに設定しました。");return true;}
        if(a[0].equalsIgnoreCase("skills")&&sender instanceof Player p){if(state!=GameState.WAITING){msg(p,"スキル選択はロビーで行ってください。");return true;}openSkillMenu(p);return true;}
        if(a[0].equalsIgnoreCase("training")&&sender instanceof Player p&&a.length>1&&a[1].equalsIgnoreCase("refresh")){if(!isInTrainingArea(p.getLocation())){msg(p,"練習エリア内で使用してください。");return true;}giveTrainingItems(p);msg(p,"練習用スキルとパッシブを更新しました。鬼スキルは心臓破壊数に関係なく全解放です。");return true;}
        if(!sender.hasPermission("onigame.admin")){msg(sender,"権限がありません。");return true;}
        if(a[0].equalsIgnoreCase("training")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og training <pos1|pos2|clear|info|dummy|refresh>");return true;}
            if(a[1].equalsIgnoreCase("pos1")||a[1].equalsIgnoreCase("pos2")){String path="locations.training-"+a[1].toLowerCase(Locale.ROOT);LocationStore.set(getConfig(),path,p.getLocation().getBlock().getLocation());saveConfig();msg(p,"練習エリアの &e"+a[1].toUpperCase(Locale.ROOT)+" &fを現在地に設定しました。");return true;}
            if(a[1].equalsIgnoreCase("clear")){getConfig().set("locations.training-pos1",null);getConfig().set("locations.training-pos2",null);saveConfig();for(UUID id:new HashSet<>(trainingPlayers)){Player q=Bukkit.getPlayer(id);if(q!=null)leaveTraining(q);}removeTrainingDummy();msg(p,"練習エリア設定を解除しました。");return true;}
            if(a[1].equalsIgnoreCase("info")){Location one=LocationStore.get(getConfig(),"locations.training-pos1"),two=LocationStore.get(getConfig(),"locations.training-pos2");msg(p,"練習エリア: "+(one!=null&&two!=null?"&a設定済み &7("+one.getBlockX()+","+one.getBlockY()+","+one.getBlockZ()+" ～ "+two.getBlockX()+","+two.getBlockY()+","+two.getBlockZ()+")":"&c未設定"));return true;}
            if(a[1].equalsIgnoreCase("dummy")){if(a.length>2&&a[2].equalsIgnoreCase("clear")){removeTrainingDummy();getConfig().set("locations.training-dummy",null);saveConfig();msg(p,"練習ダミーを解除しました。");return true;}if(!isInTrainingArea(p.getLocation())){msg(p,"練習エリア内で実行してください。");return true;}Location spawn=p.getLocation().clone();LocationStore.set(getConfig(),"locations.training-dummy",spawn);saveConfig();spawnTrainingDummy(spawn);msg(p,"現在地に練習用鬼ダミーを設置しました。");return true;}
            msg(p,"/og training <pos1|pos2|clear|info|dummy [clear]|refresh>");return true;
        }
        if(a[0].equalsIgnoreCase("arenasetup")&&sender instanceof Player p){
            if(state!=GameState.WAITING){msg(p,"&c試合中は通常アリーナを再設定できません。");return true;}
            if(a.length>1&&a[1].equalsIgnoreCase("clear")){
                getConfig().set("arena-setup.enabled",false);
                getConfig().set("arena-setup.center",null);
                getConfig().set("locations.player-spawn",null);
                getConfig().set("locations.oni-spawn",null);
                getConfig().set("locations.exit",null);
                getConfig().set("locations.hearts",new ArrayList<String>());
                saveConfig();msg(p,"&7通常アリーナの一括設定を解除しました。");return true;
            }
            if(a.length>1&&a[1].equalsIgnoreCase("info")){
                Location center=LocationStore.get(getConfig(),"arena-setup.center");
                int hearts=getConfig().getStringList("locations.hearts").size();
                msg(p,"通常アリーナ: "+(getConfig().getBoolean("arena-setup.enabled",false)?"&a有効":"&c無効")
                    +" &7/ 中心: "+(center==null?"未設定":center.getBlockX()+","+center.getBlockY()+","+center.getBlockZ())
                    +" &7/ 心臓候補: &e"+hearts+" &7/ ボーダー: &e"+getConfig().getInt("arena-setup.size",200)+"x"+getConfig().getInt("arena-setup.size",200));
                return true;
            }
            configureNormalArena(p);return true;
        }
        if(a[0].equalsIgnoreCase("ally")){
            if(state!=GameState.WAITING){msg(sender,"&c友軍NPCの設定はゲーム開始前のみ変更できます。");return true;}
            if(a.length<2||a[1].equalsIgnoreCase("status")){
                String preset=getConfig().getString("friendly-npc.preset","");
                msg(sender,"友軍NPC: "+(preset==null||preset.isBlank()?"&7なし":"&a"+friendlyPresetDisplay(preset)+" &7("+preset+")"));
                return true;
            }
            if(a[1].equalsIgnoreCase("clear")||a[1].equalsIgnoreCase("off")){
                getConfig().set("friendly-npc.preset","");saveConfig();msg(sender,"&7友軍NPCを無効にしました。");return true;
            }
            if(a[1].equalsIgnoreCase("list")){
                msg(sender,"友軍NPCプリセット: &eIGAMI_KYOYA, AZANAMI_MISAKI, ARIKAWA_FUUKA, AZANAMI_REN, MEDIC, AKASAKA_HIIRO, KAGAYA_RION, AMANAI_IONA"+(isAzakujiLocked()?"":" , AZAKUJI_HIRO"));
                return true;
            }
            String preset=parseFriendlyPreset(a[1]);
            if(preset==null){msg(sender,"&c不明なプリセットです。 &7/og ally list で一覧を確認してください。");return true;}
            if("AZAKUJI_HIRO".equals(preset)&&isAzakujiLocked()){msg(sender,"&6字九字ひろ &fは現在ロックされています。");return true;}
            getConfig().set("friendly-npc.preset",preset);saveConfig();
            msg(sender,"次回のゲームに友軍NPC &a"+friendlyPresetDisplay(preset)+" &fを参加させます。");
            return true;
        }
        if(a[0].equalsIgnoreCase("unstuck")&&sender instanceof Player p){
            if(!getConfig().getBoolean("safety-rescue.enabled",true)||!getConfig().getBoolean("safety-rescue.player-command-enabled",true)){msg(p,"&c詰み救済は現在無効です。");return true;}
            if(state!=GameState.RUNNING){msg(p,"&7このコマンドは試合中のみ使用できます。");return true;}
            UUID id=p.getUniqueId();boolean active=players.contains(id)||id.equals(oni);
            if(!active||dead.contains(id)||escaped.contains(id)||p.getGameMode()==GameMode.SPECTATOR){msg(p,"&c現在は詰み救済を使用できません。");return true;}
            long now=System.currentTimeMillis(),lock=safetyAbuseLockUntil.getOrDefault(id,0L);
            if(now<lock){msg(p,"&c詰み救済は悪用防止ロック中です。 &7残り "+Math.max(1,(lock-now+999)/1000)+"秒");return true;}
            long ready=safetyRescueCooldownUntil.getOrDefault(id,0L);
            if(now<ready){msg(p,"&7詰み救済の再使用まで &e"+Math.max(1,(ready-now+999)/1000)+"秒");return true;}
            String denied=manualUnstuckDeniedReason(p);
            if(denied!=null){registerUnstuckAbuseAttempt(p,denied);return true;}
            if(rescueEntityToSafePlace(p,true)){safetyAbuseAttempts.remove(id);safetyAbuseWindowStartedAt.remove(id);msg(p,"&a安全な場所へ移動しました。");}
            else msg(p,"&c周囲に安全な移動先を見つけられませんでした。");
            return true;
        }
        if(a[0].equalsIgnoreCase("lobbyall")){
            Location lobby=LocationStore.get(getConfig(),"locations.lobby");
            if(lobby==null||lobby.getWorld()==null){msg(sender,"&cロビー地点が未設定です。 &7/og set lobby で設定してください。");return true;}
            int moved=0;
            for(Player target:Bukkit.getOnlinePlayers()){
                if(target.teleport(lobby)){target.setFallDistance(0);moved++;}
            }
            all("&6管理者によりロビーへ集合しました。");
            msg(sender,"&aオンラインプレイヤー "+moved+"人 &fをロビーへ移動しました。");
            return true;
        }
        if(a[0].equalsIgnoreCase("lobbyhide")){
            if(a.length>1&&a[1].equalsIgnoreCase("restore")){restoreLobbyUtilities();refreshLobbyGuideDisplays();msg(sender,"&aロビー設備を保存データから復元しました。");return true;}
            List<Location> targets=lobbyUtilityLocations();
            msg(sender,"ロビー設備自動退避: &e"+targets.size()+"地点 &7/ 保存スナップショット: "+(getConfig().getBoolean("lobby-hide.snapshot-active",false)?"&aあり":"&fなし"));
            return true;
        }
        if(a[0].equalsIgnoreCase("gmbook")&&sender instanceof Player p){p.getInventory().addItem(createGmBook());msg(p,"&6GM操作本 &fを渡しました。");return true;}
        if(a[0].equalsIgnoreCase("onimenu")&&sender instanceof Player p){if(state!=GameState.WAITING){msg(p,"鬼選択はロビーで行ってください。");return true;}openOniMenu(p);return true;}
        if(a[0].equalsIgnoreCase("skillchest")&&sender instanceof Player p){Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内のチェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.skill-chest",block.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"このチェストをロビーのスキル設定チェストに登録しました。上部に案内表示も設置しました。");return true;}
        if(a[0].equalsIgnoreCase("onichest")&&sender instanceof Player p){Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内のチェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.oni-chest",block.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"このチェストを鬼選択・パッシブ設定用に登録しました。上部に案内表示も設置しました。");return true;}
        if(a[0].equalsIgnoreCase("guide")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og guide <rules|refresh|clear>");return true;}
            if(a[1].equalsIgnoreCase("rules")){LocationStore.set(getConfig(),"locations.rules-text",p.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"&a現在地にゲームルール説明を設置しました。");return true;}
            if(a[1].equalsIgnoreCase("refresh")){refreshLobbyGuideDisplays();msg(p,"&aロビー案内表示を再生成しました。");return true;}
            if(a[1].equalsIgnoreCase("clear")){getConfig().set("locations.rules-text",null);saveConfig();refreshLobbyGuideDisplays();msg(p,"&eゲームルール説明を解除しました。");return true;}
            msg(p,"/og guide <rules|refresh|clear>");return true;
        }
        if(a[0].equalsIgnoreCase("exlock")){
            if(a.length==1||a[1].equalsIgnoreCase("status")){msg(sender,"EXロック: アザクジ="+(isAzakujiLocked()?"&cLOCK":"&aOPEN")+"&f / ジャクツキ="+(isJakutsukiLocked()?"&cLOCK":"&aOPEN"));return true;}
            String target=a[1].toLowerCase(Locale.ROOT);if(!target.equals("azakuji")&&!target.equals("jakutsuki")){msg(sender,"/og exlock <azakuji|jakutsuki> <lock|unlock|status>");return true;}
            if(a.length<3||a[2].equalsIgnoreCase("status")){boolean locked=target.equals("azakuji")?isAzakujiLocked():isJakutsukiLocked();msg(sender,(target.equals("azakuji")?"字九字ひろ":"蛇窟姫")+" は現在 "+(locked?"&cLOCK":"&aOPEN")+" &fです。");return true;}
            boolean lock;if(a[2].equalsIgnoreCase("lock")||a[2].equalsIgnoreCase("on"))lock=true;else if(a[2].equalsIgnoreCase("unlock")||a[2].equalsIgnoreCase("off"))lock=false;else{msg(sender,"lock / unlock / status を指定してください。");return true;}
            String path=target.equals("azakuji")?"content-locks.azakuji-hiro":"content-locks.jakutsuki";getConfig().set(path,lock);saveConfig();
            if(target.equals("azakuji")&&lock)resetLockedAzakujiSelections();
            if(target.equals("jakutsuki")&&lock&&selectedOniType==OniType.JAKUTSUKI)selectedOniType=OniType.DAKKO;
            msg(sender,(target.equals("azakuji")?"EXプリセット 字九字ひろ":"EX鬼 蛇窟姫（ジャクツキ）")+" を "+(lock?"&cロック":"&aアンロック")+" &fしました。ロック中は選択UIに表示されません。");return true;
        }
        if(a[0].equalsIgnoreCase("forceoni")){if(a.length<2){msg(sender,"/og forceoni <プレイヤー|clear> を指定してください。");return true;}if(a[1].equalsIgnoreCase("clear")){forcedOni=null;getConfig().set("forced-oni-uuid",null);saveConfig();msg(sender,"手動の鬼指定を解除しました。次回は鬼指定ブロック、またはランダム抽選を使用します。");return true;}Player target=Bukkit.getPlayerExact(a[1]);if(target==null){msg(sender,"そのプレイヤーはオンラインではありません。");return true;}forcedOni=target.getUniqueId();getConfig().set("forced-oni-uuid",forcedOni.toString());saveConfig();msg(sender,"次回の鬼を &c"+target.getName()+" &fに指定しました。&7（/og join が必要です）");return true;}
        if(a[0].equalsIgnoreCase("oniblock")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){getConfig().set("locations.oni-selector-block",null);saveConfig();msg(p,"鬼指定ブロックを解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null){msg(p,"6ブロック以内の登録したいブロックへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.oni-selector-block",block.getLocation());saveConfig();msg(p,"&c"+block.getType().name()+" &fを鬼指定ブロックに登録しました。次回開始時、このブロックの上にいる参加者を鬼候補にします。");return true;}
        if(a[0].equalsIgnoreCase("practicechest")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){getConfig().set("locations.practice-loot-chest",null);saveConfig();msg(p,"開錠練習チェストを解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内の練習用チェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.practice-loot-chest",block.getLocation());saveConfig();msg(p,"&6このチェストをロビー限定の開錠練習チェストに登録しました。");return true;}
        if(a[0].equalsIgnoreCase("practiceheart")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){Location old=LocationStore.get(getConfig(),"locations.practice-heart");if(old!=null){String key=LocationStore.encode(old);heartHp.remove(key);for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player q=Bukkit.getPlayer(id);if(q!=null)stopRepair(q,"&7練習地点が解除されたため中断しました。");}}getConfig().set("locations.practice-heart",null);saveConfig();msg(p,"心臓破壊練習地点を解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null){msg(p,"6ブロック以内の練習用心臓ブロックへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.practice-heart",block.getLocation());saveConfig();Material heartMat=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN"));if(heartMat==null)heartMat=Material.CRYING_OBSIDIAN;block.setType(heartMat);msg(p,"&4このブロックをロビー限定の心臓破壊練習地点に登録しました。");return true;}
        if(a[0].equalsIgnoreCase("heartmode")&&a.length>1){String mode=a[1].toLowerCase();if(!mode.equals("random")&&!mode.equals("manual")){msg(sender,"random または manual を指定してください。");return true;}getConfig().set("heart-placement-mode",mode);saveConfig();msg(sender,"心臓配置を &e"+(mode.equals("random")?"ランダム":"手動マーカー")+" &fに設定しました。");return true;}
        if(a[0].equalsIgnoreCase("marker")&&sender instanceof Player p){p.getInventory().addItem(createHeartMarker());msg(p,"心臓地点マーカーを渡しました。設置すると自動で透明になります。");return true;}
        if(a[0].equalsIgnoreCase("markers")){int count=scanHeartMarkers(true).size();msg(sender,"有効な心臓地点マーカー: &e"+count+"個");return true;}
        if(a[0].equalsIgnoreCase("finaltest")&&sender instanceof Player p){
            if(a.length>1&&a[1].equalsIgnoreCase("clear")){clearFinalEffectTest();msg(p,"&a最終フェーズ演出テストを解除しました。");return true;}
            startFinalEffectTest(p);msg(p,"&4最終フェーズ演出テストを開始しました。 &7解除: /og finaltest clear");return true;
        }
        if(a[0].equalsIgnoreCase("bgm")&&a.length>1){if(a[1].equalsIgnoreCase("off")){getConfig().set("final-phase-bgm.enabled",false);saveConfig();stopGameStartBgm();stopFinalPhaseBgm();msg(sender,"最終局面BGMを無効にしました。");}else{getConfig().set("final-phase-bgm.enabled",true);getConfig().set("final-phase-bgm.sound",a[1]);saveConfig();msg(sender,"最終局面BGMを &e"+a[1]+" &fに設定しました。");}return true;}
        if(a[0].equalsIgnoreCase("bgmtest")&&sender instanceof Player p){playConfiguredBgm(p);msg(p,"最終局面BGMをテスト再生しました。");return true;}
        if(a[0].equalsIgnoreCase("bgmstop")){stopGameStartBgm();stopFinalPhaseBgm();msg(sender,"BGMを停止しました。");return true;}
        if(a[0].equalsIgnoreCase("testbook")&&sender instanceof Player p){if(!isTestGm(p)){msg(p,"この本は参加者0人のテストプレイ中のみ取得できます。");return true;}giveTestPlayBook(p);return true;}
        if(a[0].equalsIgnoreCase("testdestroy")&&sender instanceof Player p){if(!isTestGm(p)){msg(p,"この操作は参加者0人のテストプレイ中のみ使用できます。");return true;}Block target=p.getTargetBlockExact(getConfig().getInt("test-heart-target-range",20));if(target==null||!heartHp.containsKey(LocationStore.encode(target.getLocation()))){msg(p,"視線の先に未破壊の心臓がありません。");return true;}destroyHeart(target);msg(p,"視線先の心臓をテスト操作で破壊しました。");return true;}
        if(a[0].equalsIgnoreCase("testborder")&&a.length>1){boolean enabled;if(a[1].equalsIgnoreCase("on"))enabled=true;else if(a[1].equalsIgnoreCase("off"))enabled=false;else{msg(sender,"on または off を指定してください。");return true;}getConfig().set("test-arena.world-border-enabled",enabled);saveConfig();msg(sender,"テスト区域のワールドボーダーを "+(enabled?"&a有効":"&c無効")+" &fにしました。次回のテスト開始時に反映されます。");return true;}
        if(a[0].equalsIgnoreCase("botgame")&&sender instanceof Player p){OniType type=a.length>1?OniType.parse(a[1]):selectedOniType;if(type==null||isOniTypeLocked(type))type=OniType.DAKKO;if(a.length>1&&OniType.parse(a[1])==OniType.JAKUTSUKI&&!ensureOniUnlocked(p,OniType.JAKUTSUKI))return true;start(type,p,true);return true;}
        if(a[0].equalsIgnoreCase("onibotgame")&&sender instanceof Player p){OniType requested=a.length>1?OniType.parse(a[1]):selectedOniType;if(a.length>1&&requested==OniType.JAKUTSUKI&&!ensureOniUnlocked(p,requested))return true;OniType type=requested;if(type==null||isOniTypeLocked(type))type=OniType.DAKKO;int count=getConfig().getInt("player-bot.default-count",5);if(a.length>2)try{count=Integer.parseInt(a[2]);}catch(NumberFormatException ignored){}startOniVsBots(type,p,Math.max(1,Math.min(8,count)));return true;}
        if((a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch"))&&sender instanceof Player p){OniType requested=a.length>1?OniType.parse(a[1]):selectedOniType;if(a.length>1&&requested==OniType.JAKUTSUKI&&!ensureOniUnlocked(p,requested))return true;OniType type=requested;if(type==null||isOniTypeLocked(type))type=OniType.DAKKO;int count=5;if(a.length>2)try{count=Integer.parseInt(a[2]);}catch(NumberFormatException ignored){}startFullAiBotMatch(type,p,Math.max(1,Math.min(8,count)));return true;}
        if(a[0].equalsIgnoreCase("set")&&sender instanceof Player p&&a.length>1){
            switch(a[1].toLowerCase()){
                case "lobby" -> LocationStore.set(getConfig(),"locations.lobby",p.getLocation());
                case "player" -> LocationStore.set(getConfig(),"locations.player-spawn",p.getLocation());
                case "oni" -> LocationStore.set(getConfig(),"locations.oni-spawn",p.getLocation());
                case "exit" -> LocationStore.set(getConfig(),"locations.exit",p.getLocation());
                default -> {msg(p,"lobby / player / oni / exit を指定してください。");return true;}
            } saveConfig();msg(p,a[1]+"地点を設定しました。");return true;
        }
        if(a[0].equalsIgnoreCase("heart")&&sender instanceof Player p){List<String> list=getConfig().getStringList("locations.hearts");String encoded=LocationStore.encode(p.getLocation());if(list.contains(encoded)){list.remove(encoded);msg(p,"この心臓候補を削除しました。");}else{list.add(encoded);msg(p,"心臓候補を追加しました（"+list.size()+"件）。");}getConfig().set("locations.hearts",list);saveConfig();return true;}
        if(a[0].equalsIgnoreCase("chest")&&sender instanceof Player p){List<String> list=getConfig().getStringList("locations.loot-chests");String encoded=LocationStore.encode(p.getLocation());if(list.contains(encoded)){list.remove(encoded);msg(p,"このルートチェスト候補を削除しました。");}else{list.add(encoded);msg(p,"ルートチェスト候補を追加しました（"+list.size()+"件）。");}getConfig().set("locations.loot-chests",list);saveConfig();return true;}
        if(a[0].equalsIgnoreCase("chests")){msg(sender,"登録済みルートチェスト候補: &e"+getConfig().getStringList("locations.loot-chests").size()+"個");return true;}
        if(a[0].equalsIgnoreCase("start")){OniType type=a.length>1?OniType.parse(a[1]):null;if(type==OniType.JAKUTSUKI&&!ensureOniUnlocked(sender,type))return true;start(type,sender,false);return true;}
        if(a[0].equalsIgnoreCase("stop")){end(false,"管理者が試合を終了しました");return true;}
        if(a[0].equalsIgnoreCase("version")){msg(sender,"&dOniGame Runtime &f0.36.3 &7| GUI schema: &fskills 2p / passives 4p &7| Expected RP: &fv37");return true;}
        if(a[0].equalsIgnoreCase("status")){String mode=getConfig().getString("heart-placement-mode","random");Player forcedPlayer=forcedOni==null?null:Bukkit.getPlayer(forcedOni);boolean hasOniBlock=LocationStore.get(getConfig(),"locations.oni-selector-block")!=null;msg(sender,"状態: "+state+" / 参加: "+participants.size()+" / ぷれいやー: "+players.size()+" / ぷれいやーBot: "+playerBots.size()+" / 心臓: "+brokenHearts+"/"+totalHearts+" / 配置: "+mode+" / 選択鬼: "+(selectedOniType==null?"ランダム":selectedOniType.display)+" / 鬼指定: "+(forcedOni==null?"なし":forcedPlayer==null?forcedOni.toString():forcedPlayer.getName())+" / 鬼指定台: "+(hasOniBlock?"設定済み":"なし"));return true;}
        help(sender);return true;
    }
    private TextDisplay spawnLobbyGuide(Location location,String text){
        if(location==null||location.getWorld()==null)return null;
        TextDisplay display=location.getWorld().spawn(location,TextDisplay.class);
        display.setText(cc(text));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setDefaultBackground(false);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(260);
        display.setViewRange(18.0f);
        display.setPersistent(false);
        lobbyGuideDisplays.add(display.getUniqueId());
        return display;
    }
    private void removeLobbyGuideDisplays(){
        for(UUID id:new HashSet<>(lobbyGuideDisplays)){
            Entity entity=Bukkit.getEntity(id);
            if(entity!=null)entity.remove();
        }
        lobbyGuideDisplays.clear();
    }
    private void refreshLobbyGuideDisplays(){
        removeLobbyGuideDisplays();
        if(!getConfig().getBoolean("lobby-guide.enabled",true))return;
        Location skill=LocationStore.get(getConfig(),"locations.skill-chest");
        if(skill!=null)spawnLobbyGuide(skill.clone().add(.5,getConfig().getDouble("lobby-guide.chest-height",1.65),.5),
            getConfig().getString("lobby-guide.skill-chest-text","&b&l【スキル選択】\n&f右クリックでスキル・パッシブを設定"));
        Location oniChest=LocationStore.get(getConfig(),"locations.oni-chest");
        if(oniChest!=null)spawnLobbyGuide(oniChest.clone().add(.5,getConfig().getDouble("lobby-guide.chest-height",1.65),.5),
            getConfig().getString("lobby-guide.oni-chest-text","&c&l【鬼設定】\n&f鬼・鬼パッシブを設定"));
        Location rules=LocationStore.get(getConfig(),"locations.rules-text");
        if(rules!=null)spawnLobbyGuide(rules.clone().add(0,getConfig().getDouble("lobby-guide.rules-height",1.8),0),
            getConfig().getString("lobby-guide.rules-text","&4&l◆ 鬼げぇむ ◆\n&fぷれいやー: 心臓を破壊し、出口から脱出\n&c鬼: 心臓を守り、ぷれいやーを全滅させる\n&6全心臓破壊後は残り3分の最終フェーズ"));
    }
    private List<Location> lobbyUtilityLocations(){
        List<Location> out=new ArrayList<>();
        String[] paths={"locations.skill-chest","locations.oni-chest","locations.practice-loot-chest","locations.practice-heart","locations.oni-selector-block"};
        Set<String> seen=new HashSet<>();
        for(String path:paths){
            Location loc=LocationStore.get(getConfig(),path);
            if(loc==null||loc.getWorld()==null)continue;
            loc=loc.getBlock().getLocation();
            String key=LocationStore.encode(loc);
            if(seen.add(key))out.add(loc);
        }
        return out;
    }
    private void hideLobbyUtilities(){
        if(!getConfig().getBoolean("lobby-hide.enabled",true))return;
        // Recover an old interrupted snapshot first instead of overwriting it.
        if(getConfig().getBoolean("lobby-hide.snapshot-active",false))restoreLobbyUtilities();
        removeLobbyGuideDisplays();
        for(UUID id:new HashSet<>(trainingPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)leaveTraining(p);}
        removeTrainingDummy();
        List<Location> targets=lobbyUtilityLocations();
        getConfig().set("lobby-hide.snapshot",null);
        int index=0;
        for(Location loc:targets){
            Block block=loc.getBlock();
            String base="lobby-hide.snapshot."+index++;
            getConfig().set(base+".location",LocationStore.encode(loc));
            getConfig().set(base+".material",block.getType().name());
            getConfig().set(base+".block-data",block.getBlockData().getAsString());
            BlockState state=block.getState();
            if(state instanceof org.bukkit.inventory.InventoryHolder holder){
                getConfig().set(base+".inventory",Arrays.asList(holder.getInventory().getContents()));
                holder.getInventory().clear();
            }
            block.setType(Material.AIR,false);
        }
        getConfig().set("lobby-hide.snapshot-count",index);
        getConfig().set("lobby-hide.snapshot-active",index>0);
        saveConfig();
    }
    private void restoreLobbyUtilities(){
        if(!getConfig().getBoolean("lobby-hide.snapshot-active",false))return;
        int count=Math.max(0,getConfig().getInt("lobby-hide.snapshot-count",0));
        for(int i=0;i<count;i++){
            String base="lobby-hide.snapshot."+i;
            String encoded=getConfig().getString(base+".location","");
            Location loc=LocationStore.decode(encoded);
            if(loc==null||loc.getWorld()==null)continue;
            Material material=Material.matchMaterial(getConfig().getString(base+".material","AIR"));
            if(material==null)material=Material.AIR;
            Block block=loc.getBlock();
            block.setType(material,false);
            String data=getConfig().getString(base+".block-data","");
            if(data!=null&&!data.isBlank()){
                try{block.setBlockData(Bukkit.createBlockData(data),false);}catch(IllegalArgumentException ex){getLogger().warning("Lobby block data restore failed: "+data);}
            }
            List<?> saved=getConfig().getList(base+".inventory");
            BlockState state=block.getState();
            if(saved!=null&&state instanceof org.bukkit.inventory.InventoryHolder holder){
                Inventory inv=holder.getInventory();
                inv.clear();
                for(int slot=0;slot<Math.min(inv.getSize(),saved.size());slot++){
                    Object value=saved.get(slot);
                    if(value instanceof ItemStack item)inv.setItem(slot,item);
                }
            }
        }
        getConfig().set("lobby-hide.snapshot",null);
        getConfig().set("lobby-hide.snapshot-count",0);
        getConfig().set("lobby-hide.snapshot-active",false);
        saveConfig();
    }

    private void help(CommandSender s){s.sendMessage(cc("&c&l鬼げぇむ &7- commands\n&e/onigame join|leave / skills &7参加・スキルUI\n&e/onigame skill <sprint|invisible|smoke|strike|heal>\n&e/onigame gmbook / onimenu &7GM本・鬼構成UI\n&e/onigame unstuck &7詰み位置から安全な場所へ救済移動\n&e/onigame lobbyall &7オンラインの全員をロビーへ集合\n&e/onigame ally <preset|list|status|clear> &7開始時の友軍NPCを設定\n&e/onigame arenasetup [info|clear] &7通常試合の200x200区域を一括設定\n&e/onigame exlock <azakuji|jakutsuki> <lock|unlock|status> &7EX解禁管理\n&e/onigame skillchest &7視線先チェストをぷれいやー設定用に登録\n&e/onigame onichest &7視線先チェストを鬼設定用に登録\n&e/onigame guide <rules|refresh|clear> &7ロビーのゲーム説明TextDisplay\n&e/onigame lobbyhide [info|restore] &7ロビー設備の自動退避状態\n&e/onigame forceoni <player|clear> &7次回の鬼を手動指定\n&e/onigame oniblock [clear] &7視線先ブロックを鬼指定台に登録\n&e/onigame practicechest [clear] &7ロビーの開錠練習チェスト\n&e/onigame practiceheart [clear] &7ロビーの心臓破壊練習\n&e/onigame training <pos1|pos2|clear|info|dummy> &7ロビースキル練習場\n&e/onigame heartmode <random|manual>\n&e/onigame marker / markers &7手動配置マーカー\n&e/onigame chest / chests &7ルートチェスト候補\n&e/onigame finaltest [clear] &7最終フェーズ演出を即テスト/解除\n&e/onigame bgm <sound|off> / bgmtest / bgmstop\n&e/onigame botgame [dakko|kishin|jakutsuki] &7ぷれいやー対鬼Bot\n&e/onigame onibotgame [dakko|kishin|jakutsuki] [1-8] &7鬼対ぷれいやーBot\n&e/onigame aibotmatch [dakko|kishin|jakutsuki] [1-8] &7鬼Bot vs 友軍AIだけの観戦テスト（既定5人）\n&e/onigame testbook / testdestroy &7[ソロテスト専用]\n&e/onigame set <lobby|player|oni|exit> &7[管理]\n&e/onigame heart &7ランダム候補を追加/削除\n&e/onigame start [dakko|kishin|jakutsuki] / stop / status"));}

    private void start(OniType forced,CommandSender starter,boolean playerTest){
        if(state!=GameState.WAITING){all("すでに試合中です。");return;}
        participants.removeIf(id->Bukkit.getPlayer(id)==null);
        boolean soloTest=participants.isEmpty();
        if(soloTest){if(!(starter instanceof Player gm)){msg(starter,"参加者0人のテスト開始はゲーム内の管理者が実行してください。");return;}testParticipant=gm.getUniqueId();participants.add(testParticipant);msg(gm,playerTest?"&e開始者をぷれいやー、NPCを鬼にしたBotテストを開始します。":"&e参加者0人のため、開始者を鬼にしてBOT補充モードで開始します。");}
        Player testGm=soloTest?Bukkit.getPlayer(testParticipant):null;
        Location ps=LocationStore.get(getConfig(),"locations.player-spawn"), os=LocationStore.get(getConfig(),"locations.oni-spawn"), ex=LocationStore.get(getConfig(),"locations.exit"), lobby=LocationStore.get(getConfig(),"locations.lobby");
        OniType upcomingOniType=forced!=null?forced:(selectedOniType!=null?selectedOniType:OniType.values()[new Random().nextInt(2)]);
        oniType=upcomingOniType;
        String placementMode=getConfig().getString("heart-placement-mode","random");
        List<Location> heartCandidates=placementMode.equalsIgnoreCase("manual")?scanHeartMarkers(true):selectRandomHearts();
        Collections.shuffle(heartCandidates);
        int requiredRealHearts=oniType==OniType.JAKUTSUKI?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8)):heartCandidates.size();
        int requiredFakeHearts=oniType==OniType.JAKUTSUKI?Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4)):0;
        List<Location> hearts=new ArrayList<>();
        List<Location> fakeHearts=new ArrayList<>();
        if(oniType==OniType.JAKUTSUKI){
            if(heartCandidates.size()>=requiredRealHearts+requiredFakeHearts){
                hearts.addAll(heartCandidates.subList(0,requiredRealHearts));
                fakeHearts.addAll(heartCandidates.subList(requiredRealHearts,requiredRealHearts+requiredFakeHearts));
            }
        }else hearts.addAll(heartCandidates);
        boolean automaticTestHearts=soloTest;
        boolean automaticNormalArenaHearts=!soloTest&&getConfig().getBoolean("arena-setup.enabled",false);
        List<Location> automaticTestLoot=null;
        if(soloTest&&testGm!=null){Location base=testGm.getLocation().clone();TestArenaLayout layout=createRandomTestArena(base);ps=layout.playerSpawn();os=layout.oniSpawn();ex=layout.exit();heartCandidates=new ArrayList<>(layout.hearts());Collections.shuffle(heartCandidates);hearts.clear();fakeHearts.clear();if(oniType==OniType.JAKUTSUKI){if(heartCandidates.size()>=requiredRealHearts+requiredFakeHearts){hearts.addAll(heartCandidates.subList(0,requiredRealHearts));fakeHearts.addAll(heartCandidates.subList(requiredRealHearts,requiredRealHearts+requiredFakeHearts));}}else hearts.addAll(heartCandidates);automaticTestLoot=layout.lootChests();lobby=base;msg(testGm,"&a実行地点を中心とした200×200のテスト区域へ、開始地点・心臓・物資・出口をランダム配置しました。");}
        if(ps==null||os==null||ex==null||hearts.isEmpty()||(oniType==OniType.JAKUTSUKI&&(hearts.size()<requiredRealHearts||fakeHearts.size()<requiredFakeHearts))){all(oniType==OniType.JAKUTSUKI?"蛇窟姫戦には心臓候補地点が最低 "+(requiredRealHearts+requiredFakeHearts)+" 箇所必要です。（本物"+requiredRealHearts+"＋偽物"+requiredFakeHearts+"）":"開始地点・鬼地点・出口・現在の配置方式に対応する心臓地点を設定してください。");if(testParticipant!=null){participants.remove(testParticipant);testParticipant=null;}return;}
        List<Location> lootChests=automaticTestLoot!=null?automaticTestLoot:selectLootChestLocations(hearts,ps);
        resetRuntime(); oniType=upcomingOniType; List<UUID> ids=new ArrayList<>(participants); Collections.shuffle(ids);playerSideTest=playerTest;if(playerTest){oni=null;players.addAll(ids);}else{UUID chosen=chooseOni(ids);if(chosen==null){all("鬼候補を決定できませんでした。");return;}oni=chosen;ids.remove(chosen);players.addAll(ids);}activeOniPassives.addAll(selectedOniPassives);
        activeLobby=lobby;activeExit=ex;activePlayerSpawn=ps;if(soloTest&&getConfig().getBoolean("test-arena.world-border-enabled",true))applyTestWorldBorder(lobby);else if(automaticNormalArenaHearts)applyNormalArenaWorldBorder();
        Material heartMat=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN")); if(heartMat==null)heartMat=Material.CRYING_OBSIDIAN;
        for(Location l:hearts){if(automaticTestHearts||automaticNormalArenaHearts)temporaryTestHeartOriginals.put(LocationStore.encode(l),l.getBlock().getType());l.getBlock().setType(heartMat);heartHp.put(LocationStore.encode(l),getConfig().getInt("heart-max-health",100));}
        if(oniType==OniType.JAKUTSUKI)for(Location l:fakeHearts){String key=LocationStore.encode(l);fakeHeartOriginals.put(key,l.getBlock().getType());fakeHeartKeys.add(key);l.getBlock().setType(heartMat);}
        for(Location l:lootChests){String key=LocationStore.encode(l);temporaryLootChestOriginals.put(key,l.getBlock().getType());l.getBlock().setType(Material.CHEST);lootChestKeys.add(key);}
        totalHearts=heartHp.size();if(oniType==OniType.JAKUTSUKI)all("&8蛇窟姫の領域 &7――鬼の心臓は &c"+totalHearts+"個&7。さらに偽心臓が紛れ込んでいる……");secondsLeft=getConfig().getInt("game-seconds",1200);for(UUID id:new HashSet<>(trainingPlayers)){Player q=Bukkit.getPlayer(id);if(q!=null)leaveTraining(q);}removeTrainingDummy();hideLobbyUtilities();state=GameState.RUNNING;startGameStartBgm();
        int playerSideTarget=oniType==OniType.JAKUTSUKI?8:getPlayerBotTargetCount();
        String oniName;if(playerTest){LivingEntity bot=spawnOniBot(os);oniName="鬼Bot";for(UUID id:players)setupPlayer(Bukkit.getPlayer(id),ps);int added=fillPlayerBotsToTarget(ps,playerSideTarget);if(soloTest)giveTestPlayBook(testGm);if(added>0)all("&bぷれいやーBot "+added+"体 &fを追加し、ぷれいやー陣営を"+playerSideTarget+"枠まで補充しました。");}else{Player op=Bukkit.getPlayer(oni);setupOni(op,os);for(UUID id:players)setupPlayer(Bukkit.getPlayer(id),ps);int added=fillPlayerBotsToTarget(ps,playerSideTarget);if(soloTest)giveTestPlayBook(op);oniName=op.getName();if(added>0)all("&bぷれいやーBot "+added+"体 &fを追加し、ぷれいやー陣営を"+playerSideTarget+"枠まで補充しました。");}
        spawnConfiguredFriendlyNpc(ps);
        all("&4&l鬼げぇむ 開始！ &f鬼は &c"+oniName+" &7(&c"+oniType.display+"&7) &fです。心臓を破壊し、討伐または脱出せよ。");
        showGameStartTitle();
        ticker=Bukkit.getScheduler().runTaskTimer(this,this::tick,20,20);
    }
    private boolean hasHumanAzakujiPreset(){
        for(UUID id:players)if("AZAKUJI_HIRO".equals(selectedPresetNames.get(id)))return true;
        return "AZAKUJI_HIRO".equals(parseFriendlyPreset(getConfig().getString("friendly-npc.preset","")));
    }
    private int activePlayerSideCountExcludingAzakuji(){
        int count=0;
        for(UUID id:players)if(!dead.contains(id)&&!escaped.contains(id)&&Bukkit.getPlayer(id)!=null)count++;
        for(UUID id:playerBots)if(!id.equals(azakujiAllyId)&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id) instanceof LivingEntity e&&e.isValid()&&!e.isDead())count++;
        return count;
    }
    private Location azakujiReinforcementSpawn(){
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null)return p.getLocation().clone().add(2,0,2);}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;Entity e=Bukkit.getEntity(id);if(e instanceof LivingEntity le&&le.isValid()&&!le.isDead())return le.getLocation().clone().add(2,0,2);}
        Location configured=LocationStore.get(getConfig(),"locations.player-spawn");return configured==null?null:configured.clone();
    }
    private void checkAzakujiReinforcement(){
        if(oniType!=OniType.JAKUTSUKI||azakujiAllyId!=null||hasHumanAzakujiPreset())return;
        int threshold=Math.max(1,getConfig().getInt("azakuji-ally.join-conditions.player-count-threshold",4));
        int active=activePlayerSideCountExcludingAzakuji();
        int remaining=Math.max(0,totalHearts-brokenHearts);
        boolean lowPlayers=active<=threshold;
        boolean lowHearts=remaining<=Math.max(1,getConfig().getInt("azakuji-ally.join-conditions.remaining-hearts",3));
        if(!lowPlayers&&!lowHearts)return;
        Location spawn=azakujiReinforcementSpawn();if(spawn==null)return;
        LivingEntity ally=spawnAzakujiAlly(spawn,playerBots.size()+1);
        all("&6&l字九字ひろ &fが友軍として参戦した！");
        if(lowPlayers&&lowHearts)all("&7戦力減少と心臓の崩壊を察知し、アザクジが戦場へ駆けつけた。");
        else if(lowPlayers)all("&7ぷれいやー陣営が &c"+active+"人 &7まで減少――アザクジが救援に入る。");
        else all("&7鬼の心臓が残り &c"+remaining+"個 &7――アザクジが決戦へ参戦する。");
        all("&6字九字ひろ &e「待たせたな。ここからは俺も前に出るぞ！」");
        if(ally!=null){ally.getWorld().playSound(ally.getLocation(),Sound.ENTITY_LIGHTNING_BOLT_THUNDER,.8f,1.3f);ally.getWorld().spawnParticle(Particle.TOTEM,ally.getLocation().add(0,1,0),70,.8,1.0,.8,.08);}
    }
    private UUID chooseOni(List<UUID> ids){
        if(ids.isEmpty())return null;
        if(forcedOni!=null){
            if(ids.contains(forcedOni)){Player p=Bukkit.getPlayer(forcedOni);all("&c手動指定 &7により、"+(p==null?"指定プレイヤー":p.getName())+" が鬼になります。");return forcedOni;}
            Player p=Bukkit.getPlayer(forcedOni);all("&e手動指定された "+(p==null?"プレイヤー":p.getName())+" は参加していないため、鬼指定ブロックまたはランダム抽選へ切り替えます。");
        }
        Location selector=LocationStore.get(getConfig(),"locations.oni-selector-block");
        if(selector!=null){
            List<UUID> standing=new ArrayList<>();
            for(UUID id:ids){Player p=Bukkit.getPlayer(id);if(p!=null&&isStandingOnBlock(p,selector))standing.add(id);}
            if(!standing.isEmpty()){UUID chosen=standing.get(new Random().nextInt(standing.size()));Player p=Bukkit.getPlayer(chosen);all("&c鬼指定ブロック &7の上にいる "+(p==null?"参加者":p.getName())+" が鬼になります。");return chosen;}
        }
        return ids.get(0);
    }
    private boolean isStandingOnBlock(Player player,Location block){
        if(player==null||block==null||block.getWorld()==null||!player.getWorld().equals(block.getWorld()))return false;
        Location feet=player.getLocation();
        Block below=feet.clone().subtract(0,0.2,0).getBlock();
        return below.getX()==block.getBlockX()&&below.getY()==block.getBlockY()&&below.getZ()==block.getBlockZ();
    }
    private void showGameStartTitle(){if(!getConfig().getBoolean("start-title.enabled",true))return;long fadeIn=Math.max(0,getConfig().getLong("start-title.fade-in-ticks",10))*50L,stay=Math.max(1,getConfig().getLong("start-title.stay-ticks",50))*50L,fadeOut=Math.max(0,getConfig().getLong("start-title.fade-out-ticks",20))*50L;Title title=Title.title(Component.text("\uE001").font(Key.key("onigame","title")),Component.empty(),Title.Times.times(Duration.ofMillis(fadeIn),Duration.ofMillis(stay),Duration.ofMillis(fadeOut)));for(UUID id:participants){Player player=Bukkit.getPlayer(id);if(player!=null)player.showTitle(title);}}
    private void startOniVsBots(OniType type,Player gm,int count){if(!participants.isEmpty()){msg(gm,"鬼対ぷれいやーBot戦は参加者0人の状態で開始してください。");return;}int oldTarget=getConfig().getInt("player-bot.auto-fill-target",5);getConfig().set("player-bot.auto-fill-target",Math.max(1,Math.min(8,count)));try{start(type,gm,false);}finally{getConfig().set("player-bot.auto-fill-target",oldTarget);}if(state!=GameState.RUNNING||oni==null||!oni.equals(gm.getUniqueId()))return;all("&bぷれいやーBot "+playerBots.size()+"体 &fが心臓の破壊を開始した……");}
    private void startFullAiBotMatch(OniType type,Player observer,int count){
        if(state!=GameState.WAITING){msg(observer,"すでに試合中です。");return;}
        if(!participants.isEmpty()){msg(observer,"AI Botマッチは参加者0人の状態で開始してください。");return;}
        int botCount=Math.max(1,Math.min(8,count));
        int oldTarget=getConfig().getInt("player-bot.auto-fill-target",5);
        String oldFriendly=getConfig().getString("friendly-npc.preset","");
        getConfig().set("player-bot.auto-fill-target",Math.min(9,botCount+1));
        getConfig().set("friendly-npc.preset","");
        try{start(type,observer,true);}
        finally{getConfig().set("player-bot.auto-fill-target",oldTarget);getConfig().set("friendly-npc.preset",oldFriendly);}
        if(state!=GameState.RUNNING||oniBot==null)return;
        eliteBotMatchIds.clear();eliteBotMatchIds.addAll(playerBots);
        UUID observerId=observer.getUniqueId();
        players.remove(observerId);dead.remove(observerId);escaped.remove(observerId);chased.remove(observerId);
        stopRepair(observer,null);stopHealing(observer,null);cooldowns.keySet().removeIf(k->k.startsWith(observerId+":"));
        observer.setGameMode(GameMode.SPECTATOR);
        observer.sendTitle(cc("&8&lAI BOT MATCH"),cc("&4鬼Bot &7vs &aエリート友軍AI "+playerBots.size()+"人"),5,50,15);
        observer.sendActionBar(cc("&7観戦モード ―― &f/og stop &7で即終了"));
        all("&8&l【AI BOT MATCH】 &4鬼Bot「"+oniType.display+"」 &fvs &aエリート友軍AI "+playerBots.size()+"人");
        all("&7AIだけで心臓破壊・追跡・戦闘・脱出まで進行します。");
    }
    private void resetRuntime(){clearFinalEffectTest();configuredFriendlyNpcId=null;eliteBotMatchIds.clear();oniBotTacticalTarget=null;safetyLastLocation.clear();safetyLastGoodLocation.clear();safetyStuckChecks.clear();safetyRescueCooldownUntil.clear();safetyAbuseWindowStartedAt.clear();safetyAbuseLockUntil.clear();safetyAbuseAttempts.clear();lastCombatAt.clear();lobbyUtilityGuiQueued.clear();playerHitGraceUntil.clear();downEscapeUntil.clear();extraLifeConsumed.clear();playerBotEscapeEquipment.clear();if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}if(crimsonTicker!=null){crimsonTicker.cancel();crimsonTicker=null;}resetFinalCrimsonVisuals();removeDakkoCloneBots();stopAllChaseBgm();stopGameStartBgm();stopFinalPhaseBgm();removeOniBot();removePlayerBots();restoreFakeHearts();restoreTemporaryTestHearts();restoreLootChests();restoreTestWorldBorder();players.clear();escaped.clear();dead.clear();chased.clear();heavenlyArrival.clear();bugeiActive.clear();jakutsukiPierceCharging.clear();removeBlackMirrors();removeJakutsukiSnakes();blackMirrorLocations.clear();exitSealedUntil=0L;predationTriggered.clear();activeOniPassives.clear();lastSeenAt.clear();heartbeatAt.clear();stamina.clear();dashJumpPenaltyAt.clear();passiveLeapReadyAt.clear();sneakStartedAt.clear();parrySneakAt.clear();repairStartedAt.clear();reversalAt.clear();clearFakeNoises();technicalPassiveReadyAt.clear();timedSkillUntil.clear();sealingCircles.clear();sealingCircleUntil.clear();lastMoveDirection.clear();wasSprinting.clear();aftermindPrimed.clear();repairingHeart.clear();repairProgressAt.clear();heartProgressCarry.clear();doubleStakesGreatStreak.clear();threePhaseMask.clear();footstepTokens.clear();deadlineTokens.clear();bloodMarkTokens.clear();footstepNextGainAt.clear();deadlineNearSince.clear();deadlineNextGainAt.clear();threePhaseReady.clear();huntRecordVictims.clear();grudgeResistReady.clear();grudgeTokens.clear();oniChaseLostAt.clear();oniLastHitAt.clear();oniScentTrailLocation.clear();oniScentTrailUntil.clear();oniSkillSealUntil.clear();oniBloodFrenzyHits.clear();oniDisruptionStacks.clear();oniLastAnyChaseAt=0;oniHuntersInstinctAt=0;oniHuntingGroundSince=0;oniHuntingGroundAnchor=null;nextSkillCheckAt.clear();skillChecks.clear();healingTasks.clear();playerBotWorkingHeart.clear();nextPlayerBotSkillCheck.clear();playerBotNextAttackAt.clear();playerBotSkillChecks.clear();playerBotSkills.clear();playerBotPassives.clear();playerBotPresetNames.clear();playerBotSkillReadyAt.clear();playerBotSprintUntil.clear();playerBotStamina.clear();playerBotTargetHeart.clear();playerBotLastLocation.clear();playerBotStuckTicks.clear();heartAlertAt.clear();cooldowns.clear();heartHp.clear();brokenHearts=0;finalPhase=false;playerSideTest=false;botStamina=20;botRestUntil=0;botNextMeleeAt=0;botDakkoSkillCycle=0;oniChaseStartedAt=0;nextBloodScentAt=0;if(ticker!=null)ticker.cancel();}
    private boolean isDownEscape(UUID id){return System.currentTimeMillis()<downEscapeUntil.getOrDefault(id,0L);}
    private boolean tryConsumeExtraLife(Player p,double lethalDamage){
        if(p==null||state!=GameState.RUNNING||!players.contains(p.getUniqueId())||dead.contains(p.getUniqueId())||escaped.contains(p.getUniqueId()))return false;
        UUID id=p.getUniqueId();
        if(!getConfig().getBoolean("player-lives.enabled",true)||extraLifeConsumed.contains(id)||lethalDamage<p.getHealth())return false;
        extraLifeConsumed.add(id);
        int ticks=Math.max(20,getConfig().getInt("player-lives.escape-duration-ticks",160));
        int speed=Math.max(0,getConfig().getInt("player-lives.escape-speed-amplifier",4));
        double max=p.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20.0:p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double ratio=Math.max(.05,Math.min(1.0,getConfig().getDouble("player-lives.revive-health-ratio",.35)));
        p.setHealth(Math.max(1.0,Math.min(max,max*ratio)));
        p.setFireTicks(0);p.setFallDistance(0);p.setCollidable(false);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,ticks,0,false,false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,speed,false,true));
        downEscapeUntil.put(id,System.currentTimeMillis()+ticks*50L);
        playerHitGraceUntil.remove(id);chased.remove(id);leaveChaseBgmNow(p);
        stopRepair(p,null);stopHealing(p,null);chestOpeningTasks.remove(id);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.hidePlayer(this,p);
        p.sendTitle(cc("&c&l残機消費"),cc("&7一度だけ死を免れた――今のうちに逃げろ！"),5,50,15);
        p.sendActionBar(cc("&8完全透明化 &7+ &b高速移動 &7―― &c次に倒されれば死亡"));
        p.playSound(p.getLocation(),Sound.ITEM_TOTEM_USE,1.0f,.75f);
        p.getWorld().spawnParticle(Particle.TOTEM,p.getLocation().add(0,1,0),45,.6,.9,.6,.08);
        Bukkit.getScheduler().runTaskLater(this,()->endDownEscape(id),ticks);
        return true;
    }
    private void endDownEscape(UUID id){
        downEscapeUntil.remove(id);
        Player p=Bukkit.getPlayer(id);if(p==null)return;
        p.setCollidable(true);
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.showPlayer(this,p);
        if(state==GameState.RUNNING&&players.contains(id)&&!dead.contains(id)&&!escaped.contains(id)){
            p.sendTitle("",cc("&c透明化終了 &7―― 次のダウンは即死"),0,35,10);
            p.playSound(p.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE,.7f,.8f);
        }
    }
    private void restoreDownEscapeVisual(Player p){
        if(p==null)return;p.setCollidable(true);p.removePotionEffect(PotionEffectType.INVISIBILITY);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(p.getUniqueId()))viewer.showPlayer(this,p);
    }

    private boolean tryConsumePlayerBotExtraLife(LivingEntity bot,double lethalDamage){
        if(bot==null||state!=GameState.RUNNING||!playerBots.contains(bot.getUniqueId())||bot.getUniqueId().equals(azakujiAllyId)||deadPlayerBots.contains(bot.getUniqueId())||escapedPlayerBots.contains(bot.getUniqueId()))return false;
        UUID id=bot.getUniqueId();
        if(!getConfig().getBoolean("player-lives.enabled",true)||extraLifeConsumed.contains(id)||lethalDamage<bot.getHealth())return false;
        extraLifeConsumed.add(id);
        int ticks=Math.max(20,getConfig().getInt("player-lives.escape-duration-ticks",160));
        int speed=Math.max(0,getConfig().getInt("player-lives.escape-speed-amplifier",4));
        double max=bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20.0:bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double ratio=Math.max(.05,Math.min(1.0,getConfig().getDouble("player-lives.revive-health-ratio",.35)));
        bot.setHealth(Math.max(1.0,Math.min(max,max*ratio)));
        bot.setFireTicks(0);bot.setFallDistance(0);bot.setInvulnerable(true);bot.setCollidable(false);
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,ticks,0,false,false));
        bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,speed,false,true));
        if(bot.getEquipment()!=null){
            ItemStack[] saved=new ItemStack[]{cloneOrNull(bot.getEquipment().getItemInMainHand()),cloneOrNull(bot.getEquipment().getItemInOffHand()),cloneOrNull(bot.getEquipment().getHelmet()),cloneOrNull(bot.getEquipment().getChestplate()),cloneOrNull(bot.getEquipment().getLeggings()),cloneOrNull(bot.getEquipment().getBoots())};
            playerBotEscapeEquipment.put(id,saved);
            bot.getEquipment().setItemInMainHand(null);bot.getEquipment().setItemInOffHand(null);bot.getEquipment().setHelmet(null);bot.getEquipment().setChestplate(null);bot.getEquipment().setLeggings(null);bot.getEquipment().setBoots(null);
        }
        downEscapeUntil.put(id,System.currentTimeMillis()+ticks*50L);
        clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
        bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_TOTEM_USE,.9f,.8f);
        bot.getWorld().spawnParticle(Particle.TOTEM,bot.getLocation().add(0,1,0),35,.55,.8,.55,.07);
        all("&bぷれいやーBot &7が残機を消費し、透明化して逃走した！");
        Bukkit.getScheduler().runTaskLater(this,()->endPlayerBotDownEscape(id),ticks);
        return true;
    }
    private ItemStack cloneOrNull(ItemStack stack){return stack==null?null:stack.clone();}
    private void endPlayerBotDownEscape(UUID id){
        downEscapeUntil.remove(id);
        if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){playerBotEscapeEquipment.remove(id);return;}
        bot.setInvulnerable(false);bot.setCollidable(true);bot.removePotionEffect(PotionEffectType.INVISIBILITY);
        ItemStack[] saved=playerBotEscapeEquipment.remove(id);
        if(saved!=null&&bot.getEquipment()!=null&&saved.length>=6){bot.getEquipment().setItemInMainHand(saved[0]);bot.getEquipment().setItemInOffHand(saved[1]);bot.getEquipment().setHelmet(saved[2]);bot.getEquipment().setChestplate(saved[3]);bot.getEquipment().setLeggings(saved[4]);bot.getEquipment().setBoots(saved[5]);}
        if(state==GameState.RUNNING&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)){bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE,.55f,.85f);bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),16,.4,.6,.4,.03);}
    }
    private void restorePlayerBotDownEscape(UUID id){
        downEscapeUntil.remove(id);
        if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){playerBotEscapeEquipment.remove(id);return;}
        bot.setInvulnerable(false);bot.setCollidable(true);bot.removePotionEffect(PotionEffectType.INVISIBILITY);
        ItemStack[] saved=playerBotEscapeEquipment.remove(id);
        if(saved!=null&&bot.getEquipment()!=null&&saved.length>=6){bot.getEquipment().setItemInMainHand(saved[0]);bot.getEquipment().setItemInOffHand(saved[1]);bot.getEquipment().setHelmet(saved[2]);bot.getEquipment().setChestplate(saved[3]);bot.getEquipment().setLeggings(saved[4]);bot.getEquipment().setBoots(saved[5]);}
    }

    private void common(Player p){restoreDownEscapeVisual(p);p.getInventory().clear();p.getActivePotionEffects().forEach(e->p.removePotionEffect(e.getType()));p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setWalkSpeed((float)getConfig().getDouble("player-walk-speed",0.20));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setFoodLevel(20);p.setFireTicks(0);}
    private void setupPlayer(Player p,Location at){common(p);if(hasPassive(p.getUniqueId(),PassiveSkill.DURABILITY_BOOST)){double maxHealth=Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);p.setHealth(maxHealth);}if(hasPassive(p.getUniqueId(),PassiveSkill.NINJA_BLOOD))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23));else if(hasPassive(p.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.divine-technique-walk-speed",0.21));p.teleport(at);PlayerSkill sk=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);p.getInventory().setItem(0,playerSkillIconItem(sk,"&b&l"+sk.display,"skill:"+sk.name()));p.getInventory().setItem(8,item(Material.COMPASS,"&d心臓探知機","tracker"));giveTechnicalPassiveItems(p);}
    private boolean hasPassive(UUID id,PassiveSkill passive){return selectedPassives.getOrDefault(id,new LinkedHashSet<>()).contains(passive);}
    private void giveTechnicalPassiveItems(Player player){if(hasPassive(player.getUniqueId(),PassiveSkill.DECOY))player.getInventory().setItem(7,menuItem(Material.SNOWBALL,"&b&l陽動石","passive:decoy","&e使用方法: &f右クリックで投げる","&7投擲地点に偽の走行音を発生","&8Qキーでは捨てられません"));}
    private void setupOni(Player p,Location at){common(p);migrateLegacyJakutsukiNames(p);p.teleport(at);p.setWalkSpeed((float)getConfig().getDouble("oni-walk-speed",0.24));if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(hasOniPassive(OniPassiveSkill.ANCHOR)?Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))):0);double oniMax=oniType==OniType.JAKUTSUKI?getConfig().getDouble("jakutsuki-stats.max-health",200.0):60.0;p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(oniMax);p.setHealth(oniMax);p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,Integer.MAX_VALUE,0,false,false));p.getInventory().setItem(0,item(Material.NETHERITE_SWORD,"&4&l鬼の爪","oni_weapon"));if(oniType==OniType.DAKKO){equipDakkoArmor(p);p.getInventory().setItem(1,oniSkillIconItem("dakko_tp","&d狐渡り","dakko_tp"));refreshOniUnlockedSkills(p,false);}else if(oniType==OniType.KISHIN){p.getInventory().setItem(1,oniSkillIconItem("kishin_charge","&c鬼突","kishin_charge"));refreshOniUnlockedSkills(p,false);}else{equipJakutsukiArmor(p);p.getInventory().setItem(1,oniSkillIconItem("jakutsuki_black_mirror","&5&l黒鏡 &7[SHIFT+右:設置 / 右:転移]","jakutsuki_black_mirror"));p.getInventory().setItem(2,oniSkillIconItem("jakutsuki_sweep","&5&l薙ぎ払い","jakutsuki_sweep"));p.getInventory().setItem(3,oniSkillIconItem("jakutsuki_snakefall","&8&l蛇崩","jakutsuki_snakefall"));p.getInventory().setItem(4,oniSkillIconItem("jakutsuki_release","&4&l解放","jakutsuki_release"));p.getInventory().setItem(5,oniSkillIconItem("jakutsuki_piercing_blast","&d&l黒の波動 &7[溜め攻撃]","jakutsuki_piercing_blast"));}}
    private LivingEntity spawnOniBot(Location at){Zombie bot=at.getWorld().spawn(at,Zombie.class);bot.setBaby(false);bot.setCustomName(cc("&4鬼Bot「"+oniType.display+"」"));bot.setCustomNameVisible(false);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.setCanPickupItems(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));double botMax=oniType==OniType.JAKUTSUKI?getConfig().getDouble("jakutsuki-stats.max-health",200.0):60.0;bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(botMax);bot.setHealth(botMax);bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",0.32));bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(getConfig().getDouble("bot.attack-damage",7.0));if(bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(hasOniPassive(OniPassiveSkill.ANCHOR)?Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))):0);bot.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));bot.getEquipment().setItemInMainHandDropChance(0);if(oniType==OniType.DAKKO){bot.getEquipment().setHelmet(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"));bot.getEquipment().setChestplate(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"));bot.getEquipment().setLeggings(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"));bot.getEquipment().setBoots(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"));}else if(oniType==OniType.JAKUTSUKI){bot.getEquipment().setHelmet(jakutsukiArmor(Material.IRON_HELMET,"&5蛇窟姫の髪飾り"));bot.getEquipment().setChestplate(jakutsukiArmor(Material.IRON_CHESTPLATE,"&5蛇窟姫の装束"));bot.getEquipment().setLeggings(jakutsukiArmor(Material.IRON_LEGGINGS,"&5蛇窟姫の袴"));bot.getEquipment().setBoots(jakutsukiArmor(Material.IRON_BOOTS,"&5蛇窟姫の足袋"));}bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);oniBot=bot.getUniqueId();botNextAbilityAt=System.currentTimeMillis()+5000;return bot;}
    private LivingEntity getOniEntity(){if(state==GameState.WAITING&&trainingDummy!=null&&Bukkit.getEntity(trainingDummy) instanceof LivingEntity entity)return entity;if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity entity)return entity;return oni==null?null:Bukkit.getPlayer(oni);}
    private void removeOniBot(){if(oniBot!=null){Entity entity=Bukkit.getEntity(oniBot);if(entity!=null)entity.remove();oniBot=null;}}
    private String parseFriendlyPreset(String raw){
        if(raw==null)return null;String key=raw.trim().toUpperCase(Locale.ROOT).replace("-","_");
        return switch(key){
            case "IGAMI","KYOYA","IGAMI_KYOYA","伊神京也"->"IGAMI_KYOYA";
            case "MISAKI","AZANAMI_MISAKI","字那美咲"->"AZANAMI_MISAKI";
            case "FUUKA","FUKA","ARIKAWA_FUUKA","有川風香"->"ARIKAWA_FUUKA";
            case "REN","AZANAMI_REN","字那美蓮"->"AZANAMI_REN";
            case "MEDIC","NATSUMI","SAKURA_NATSUMI","佐倉夏海"->"MEDIC";
            case "HIIRO","AKASAKA_HIIRO","赤坂陽彩"->"AKASAKA_HIIRO";
            case "RION","KAGAYA_RION","加賀谷凛音"->"KAGAYA_RION";
            case "IONA","AMANAI_IONA","天内伊御奈"->"AMANAI_IONA";
            case "HIRO","AZAKUJI_HIRO","字九字ひろ"->"AZAKUJI_HIRO";
            default->null;
        };
    }
    private String friendlyPresetDisplay(String id){return switch(id){
        case "IGAMI_KYOYA"->"伊神京也";case "AZANAMI_MISAKI"->"字那美咲";case "ARIKAWA_FUUKA"->"有川風香";case "AZANAMI_REN"->"字那美蓮";
        case "MEDIC"->"佐倉夏海";case "AKASAKA_HIIRO"->"赤坂陽彩";case "KAGAYA_RION"->"加賀谷凛音";case "AMANAI_IONA"->"天内伊御奈";case "AZAKUJI_HIRO"->"字九字ひろ";default->id;
    };}
    private int friendlyPresetHeadModel(String id){return switch(id){
        case "ARIKAWA_FUUKA"->2001;case "IGAMI_KYOYA"->2002;case "AZANAMI_MISAKI"->2003;case "AZANAMI_REN"->2004;case "MEDIC"->2005;
        case "AKASAKA_HIIRO"->2006;case "KAGAYA_RION"->2007;case "AMANAI_IONA"->2008;case "AZAKUJI_HIRO"->2010;default->0;
    };}
    private boolean applyFriendlyPreset(LivingEntity bot,String preset){
        if(bot==null||preset==null)return false;UUID id=bot.getUniqueId();PlayerSkill skill;LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();
        switch(preset){
            case "IGAMI_KYOYA"->{skill=PlayerSkill.ONI_STRIKE;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DURABILITY_BOOST);}
            case "AZANAMI_MISAKI"->{skill=PlayerSkill.SPRINT;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DEEP_BREATH);}
            case "ARIKAWA_FUUKA"->{skill=PlayerSkill.SMOKE;passives.add(PassiveSkill.COWARDICE);passives.add(PassiveSkill.EXORCISM);}
            case "AZANAMI_REN"->{skill=PlayerSkill.OBSESSION;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DURABILITY_BOOST);}
            case "MEDIC"->{skill=PlayerSkill.HEAL;passives.add(PassiveSkill.FOCUS);passives.add(PassiveSkill.BOND);}
            case "AKASAKA_HIIRO"->{skill=PlayerSkill.SAFE_LANDING;passives.add(PassiveSkill.NINJA_BLOOD);passives.add(PassiveSkill.LEAP);}
            case "KAGAYA_RION"->{skill=PlayerSkill.BLINK;passives.add(PassiveSkill.QUICK_TURN);passives.add(PassiveSkill.CORNERED_RAT);}
            case "AMANAI_IONA"->{skill=PlayerSkill.ECHO;passives.add(PassiveSkill.SILENT_BREATH);passives.add(PassiveSkill.DECOY);}
            case "AZAKUJI_HIRO"->{skill=PlayerSkill.BUGEI;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DIVINE_TECHNIQUE);}
            default->{return false;}
        }
        playerBotSkills.put(id,skill);playerBotPassives.put(id,passives);playerBotPresetNames.put(id,friendlyPresetDisplay(preset));
        double max=passives.contains(PassiveSkill.DURABILITY_BOOST)?Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30)):20;
        if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);bot.setHealth(max);
        bot.setCustomName(cc("&a&l"+friendlyPresetDisplay(preset)+" &7【友軍NPC】"));bot.setCustomNameVisible(true);
        if(bot.getEquipment()!=null){
            ItemStack head=new ItemStack(Material.PLAYER_HEAD);ItemMeta meta=head.getItemMeta();int model=friendlyPresetHeadModel(preset);if(model>0)meta.setCustomModelData(model);head.setItemMeta(meta);
            bot.getEquipment().setHelmet(head);bot.getEquipment().setChestplate(plainBotArmor(Material.LEATHER_CHESTPLATE));bot.getEquipment().setLeggings(plainBotArmor(Material.LEATHER_LEGGINGS));bot.getEquipment().setBoots(plainBotArmor(Material.LEATHER_BOOTS));
            bot.getEquipment().setItemInMainHand(skill==PlayerSkill.BUGEI?new ItemStack(Material.GOLDEN_SWORD):new ItemStack(Material.IRON_PICKAXE));
            bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);bot.getEquipment().setItemInMainHandDropChance(0);
        }
        return true;
    }
    private LivingEntity spawnConfiguredFriendlyNpc(Location origin){
        String preset=getConfig().getString("friendly-npc.preset","");
        if(origin==null||preset==null||preset.isBlank())return null;
        preset=parseFriendlyPreset(preset);if(preset==null)return null;
        if("AZAKUJI_HIRO".equals(preset)){
            if(isAzakujiLocked())return null;
            LivingEntity ally=spawnAzakujiAlly(origin,playerBots.size()+1);
            configuredFriendlyNpcId=ally==null?null:ally.getUniqueId();
            all("&6&l字九字ひろ &fが開始時から友軍NPCとして参戦した！");
            return ally;
        }
        LivingEntity ally=spawnPlayerBot(origin,playerBots.size()+1);
        if(!applyFriendlyPreset(ally,preset)){ally.remove();playerBots.remove(ally.getUniqueId());return null;}
        configuredFriendlyNpcId=ally.getUniqueId();
        all("&a&l"+friendlyPresetDisplay(preset)+" &fが友軍NPCとして参戦した！");
        return ally;
    }

    private LivingEntity spawnPlayerBot(Location origin,int number){Location at=origin.clone().add((number-1)%3-1,0,(number-1)/3);Zombie bot=at.getWorld().spawn(at,Zombie.class);bot.setBaby(false);bot.setAI(true);bot.setSilent(true);bot.setCanPickupItems(false);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));assignPlayerBotPreset(bot,number);double max=hasPlayerBotPassive(bot.getUniqueId(),PassiveSkill.DURABILITY_BOOST)?Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30)):20;bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("player-bot.pathfinder-base-movement-speed",.25));bot.setHealth(max);bot.setCustomName("ぷれいやーBot "+number+"【"+playerBotPresetNames.get(bot.getUniqueId())+"】");bot.setCustomNameVisible(false);bot.getEquipment().setHelmet(plainBotArmor(Material.LEATHER_HELMET));bot.getEquipment().setChestplate(plainBotArmor(Material.LEATHER_CHESTPLATE));bot.getEquipment().setLeggings(plainBotArmor(Material.LEATHER_LEGGINGS));bot.getEquipment().setBoots(plainBotArmor(Material.LEATHER_BOOTS));bot.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);bot.getEquipment().setItemInMainHandDropChance(0);UUID id=bot.getUniqueId();playerBots.add(id);playerBotStamina.put(id,20.0);return bot;}
    private LivingEntity spawnAzakujiAlly(Location origin,int number){
        LivingEntity bot=spawnPlayerBot(origin,number);
        UUID id=bot.getUniqueId();
        azakujiAllyId=id;
        azakujiFightUntil=0L;azakujiEscapeUntil=0L;azakujiTauntUntil=0L;azakujiNextLeapAt=0L;azakujiNextMeleeAt=0L;azakujiLastCombatAt=0L;
        LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();
        passives.add(PassiveSkill.ATTACK_BOOST);
        passives.add(PassiveSkill.DIVINE_TECHNIQUE);
        playerBotSkills.put(id,PlayerSkill.BUGEI);
        playerBotPassives.put(id,passives);
        playerBotPresetNames.put(id,"字九字ひろ");
        bot.setCustomName(cc("&6&l字九字ひろ &7【友軍NPC】"));
        bot.setCustomNameVisible(true);
        double speed=getConfig().getDouble("player-bot.pathfinder-base-movement-speed",.25)*1.05;
        if(bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        double allyMaxHealth=Math.max(1.0,getConfig().getDouble("azakuji-ally.max-health",60.0));
        if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(allyMaxHealth);
        bot.setHealth(allyMaxHealth);
        if(bot.getAttribute(Attribute.GENERIC_ARMOR)!=null)bot.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(Math.max(0.0,getConfig().getDouble("azakuji-ally.armor",12.0)));
        ItemStack head=new ItemStack(Material.PLAYER_HEAD);ItemMeta hm=head.getItemMeta();hm.setCustomModelData(2010);head.setItemMeta(hm);bot.getEquipment().setHelmet(head);
        bot.getEquipment().setChestplate(plainBotArmor(Material.LEATHER_CHESTPLATE));
        bot.getEquipment().setLeggings(plainBotArmor(Material.LEATHER_LEGGINGS));
        bot.getEquipment().setBoots(plainBotArmor(Material.LEATHER_BOOTS));
        bot.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        return bot;
    }
    private void assignPlayerBotPreset(LivingEntity bot,int number){UUID id=bot.getUniqueId();PlayerSkill skill=PlayerSkill.HEAL;LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();String name="佐倉夏海";switch(Math.floorMod(number-1,5)){case 0->{name="伊神京也";skill=PlayerSkill.ONI_STRIKE;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DURABILITY_BOOST);}case 1->{name="字那美咲";skill=PlayerSkill.SPRINT;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DEEP_BREATH);}case 2->{name="有川風香";skill=PlayerSkill.SMOKE;passives.add(PassiveSkill.COWARDICE);passives.add(PassiveSkill.EXORCISM);}case 3->{name="字那美蓮";skill=PlayerSkill.OBSESSION;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DURABILITY_BOOST);}default->{passives.add(PassiveSkill.FOCUS);passives.add(PassiveSkill.BOND);}}playerBotSkills.put(id,skill);playerBotPassives.put(id,passives);playerBotPresetNames.put(id,name);}
    private boolean hasPlayerBotPassive(UUID id,PassiveSkill passive){return playerBotPassives.getOrDefault(id,new LinkedHashSet<>()).contains(passive);}
    private boolean playerBotHasSnakeSlash(LivingEntity bot){return bot!=null&&bot.getEquipment()!=null&&isJakutsukiSnakeSlash(bot.getEquipment().getItemInMainHand());}
    private int getPlayerBotTargetCount(){return getConfig().getBoolean("player-bot.auto-fill-enabled",true)?Math.max(0,Math.min(8,getConfig().getInt("player-bot.auto-fill-target",5))):0;}
    private int fillPlayerBotsToTarget(Location origin,int target){if(origin==null||target<=0)return 0;int humanPlayers=(int)players.stream().filter(id->Bukkit.getPlayer(id)!=null).count();int needed=Math.max(0,target-humanPlayers-playerBots.size());int startNumber=playerBots.size()+1;for(int i=0;i<needed;i++)spawnPlayerBot(origin,startNumber+i);return needed;}

    private ItemStack plainBotArmor(Material material){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setUnbreakable(true);item.setItemMeta(meta);return item;}
    private void removePlayerBots(){configuredFriendlyNpcId=null;eliteBotMatchIds.clear();for(UUID id:new HashSet<>(playerBots)){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}azakujiAllyId=null;azakujiFightUntil=0L;azakujiEscapeUntil=0L;azakujiTauntUntil=0L;azakujiNextLeapAt=0L;azakujiNextMeleeAt=0L;azakujiLastCombatAt=0L;playerBots.clear();escapedPlayerBots.clear();deadPlayerBots.clear();playerBotSkills.clear();playerBotPassives.clear();playerBotPresetNames.clear();playerBotSkillReadyAt.clear();playerBotSprintUntil.clear();playerBotStamina.clear();playerBotTargetHeart.clear();playerBotLastLocation.clear();playerBotStuckTicks.clear();}
    private Material skillMaterial(PlayerSkill s){return switch(s){case SPRINT->Material.FEATHER;case INVISIBLE->Material.GLASS_BOTTLE;case SMOKE->Material.INK_SAC;case ONI_STRIKE->Material.BLAZE_ROD;case HEAL->Material.GOLDEN_APPLE;case OBSESSION->Material.SPECTRAL_ARROW;case SAFE_LANDING->Material.RABBIT_FOOT;case BLINK->Material.ENDER_PEARL;case ECHO->Material.ARMOR_STAND;case CLAIRVOYANCE->Material.ENDER_EYE;case UNYIELDING->Material.TOTEM_OF_UNDYING;case SEALING_CIRCLE->Material.ENCHANTING_TABLE;case RESONANCE->Material.AMETHYST_SHARD;case SUBSTITUTE->Material.CHORUS_FRUIT;case DESPERATE_RUN->Material.NETHER_STAR;case BUGEI->Material.GOLDEN_SWORD;};}
    private void migrateLegacyJakutsukiNames(Player p){
        for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack stack=p.getInventory().getItem(slot);if(stack==null||!stack.hasItemMeta())continue;String action=actionOf(stack);ItemMeta meta=stack.getItemMeta();String display=meta.hasDisplayName()?ChatColor.stripColor(meta.getDisplayName()):"";if("jakutsuki_piercing_blast".equals(action)||(display!=null&&display.contains("蛇穿"))){meta.setDisplayName(cc("&d&l黒の波動 &7[溜め攻撃]"));stack.setItemMeta(meta);}}
    }
    private ItemStack item(Material m,String name,String action){ItemStack i=new ItemStack(m);ItemMeta im=i.getItemMeta();im.setDisplayName(cc(name));im.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);i.setItemMeta(im);return i;}
    private ItemStack menuItem(Material material,String name,String action,String... lore){ItemStack i=item(material,name,action);ItemMeta m=i.getItemMeta();m.setLore(Arrays.stream(lore).map(this::cc).toList());i.setItemMeta(m);return i;}
    private ItemStack consumable(Material material,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();List<String> lines=new ArrayList<>(m.getLore()==null?List.of():m.getLore());lines.add(cc("&6使い切りアイテム"));m.setLore(lines);i.setItemMeta(m);return i;}
    private String actionOf(ItemStack stack){return stack==null||!stack.hasItemMeta()?null:stack.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);}
    private boolean isMainSkillItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("skill:");}
    private boolean isConsumableItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("item:");}
    private boolean isEquipmentItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("equipment:");}
    private int carriedConsumables(Player player){int count=0;for(ItemStack stack:player.getInventory().getContents())if(isConsumableItem(stack))count+=stack.getAmount();return count;}
    private ItemStack equipment(Material material,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();m.setUnbreakable(true);List<String> lines=new ArrayList<>(m.getLore()==null?List.of():m.getLore());lines.add(cc("&b装備品 &7――使い切りアイテム上限の対象外"));m.setLore(lines);i.setItemMeta(m);return i;}
    private ItemStack customModelItem(Material material,int model,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();m.setCustomModelData(model);i.setItemMeta(m);return i;}
    private int passiveIconModel(PassiveSkill passive){return 3101+passive.ordinal();}
    private int oniPassiveIconModel(OniPassiveSkill passive){return 3201+passive.ordinal();}
    private ItemStack passiveIconItem(PassiveSkill passive,String name,String action,String... lore){return customModelItem(Material.PAPER,passiveIconModel(passive),name,action,lore);}
    private ItemStack oniPassiveIconItem(OniPassiveSkill passive,String name,String action,String... lore){return customModelItem(Material.PAPER,oniPassiveIconModel(passive),name,action,lore);}
    private int playerSkillIconModel(PlayerSkill skill){return 3301+skill.ordinal();}
    private ItemStack playerSkillIconItem(PlayerSkill skill,String name,String action,String... lore){return customModelItem(Material.PAPER,playerSkillIconModel(skill),name,action,lore);}
    private int oniSkillIconModel(String action){return switch(action){
        case "dakko_tp"->3401;case "dakko_clone"->3402;case "dakko_fox_fire"->3403;case "dakko_heavenly_arrival"->3404;
        case "kishin_charge"->3411;case "kishin_slam"->3412;case "kishin_roar"->3413;case "kishin_iron_body"->3414;
        case "jakutsuki_black_mirror"->3421;case "jakutsuki_sweep"->3422;case "jakutsuki_snakefall"->3423;case "jakutsuki_release"->3424;case "jakutsuki_piercing_blast"->3425;
        default->0;};}
    private ItemStack oniSkillIconItem(String oniAction,String name,String action,String... lore){int model=oniSkillIconModel(oniAction);return model<=0?menuItem(Material.PAPER,name,action,lore):customModelItem(Material.PAPER,model,name,action,lore);}

    private String mainSkillUsage(PlayerSkill skill){return switch(skill){
        case SPRINT,INVISIBLE,SMOKE,ONI_STRIKE,OBSESSION,BLINK,ECHO,CLAIRVOYANCE,UNYIELDING,SEALING_CIRCLE,RESONANCE,DESPERATE_RUN,BUGEI->"&e使用方法: &fゲーム中、専用スキルアイテムを右クリック";
        case HEAL->"&e使用方法: &f専用スキルアイテムを右クリックし、移動せず回復完了まで待つ";
        case SAFE_LANDING->"&e使用方法: &f落下ダメージが発生する着地時に自動発動";
        case SUBSTITUTE->"&e使用方法: &f15m以内の仲間を視線中央に捉えて専用アイテムを右クリック";
    };}
    private String passiveUsage(PassiveSkill passive){return switch(passive){
        case LEAP->"&e使用方法: &f空中でスペースをもう一度押す";
        case PARRY->"&e使用方法: &f鬼の攻撃が当たる直前にSHIFT";
        case QUICK_TURN->"&e使用方法: &f走行方向に対して横を向きながらSHIFT";
        case SILENT_BREATH->"&e使用方法: &fその場で動かずSHIFTを長押し";
        case LAST_RESERVE->"&e使用方法: &fスタミナ切れ直前に走るのを止める";
        case DECOY->"&e使用方法: &f支給される陽動石を手に持って右クリック";
        case CORNERED_RAT->"&e使用方法: &f鬼の攻撃直前に進行方向を反転する";
        case DOUBLE_STAKES->"&e使用方法: &f心臓損傷中、金色のGREATに重なった瞬間にSHIFT";
        case THREE_PHASE->"&eトークン: &fチェイス離脱=走 / 心臓破壊参加=破 / 仲間の治療支援=援";
        case FOOTSTEP_THIEF->"&eトークン: &f鬼20m以内で未発見のまま10秒ごとに1、最大4";
        case DEADLINE_ACCOUNTING->"&eトークン: &f鬼3.5m以内を約2.5秒、追跡されず無傷で切り抜けると1";
        case AFTERMIND->"&e使用方法: &f心臓損傷のスキルチェックでGREATを出すと自動発動";
        case PRACTICED->"&e使用方法: &f同じ心臓の損傷を中断せず続けると自動発動";
        case COWARDICE->"&e使用方法: &fSHIFTでスニーク中に自動適用";
        case BOND->"&e使用方法: &f治療スキル完了時、周囲の仲間へ自動適用";
        default->"&e使用方法: &f常時または条件成立時に自動発動";
    };}
    private int menuPageFromType(String type){
        int colon=type.indexOf(':');
        if(colon<0)return 1;
        try{return Math.max(1,Integer.parseInt(type.substring(colon+1)));}catch(NumberFormatException ignored){return 1;}
    }
    private ItemStack playerSkillMenuItem(PlayerSkill skill,PlayerSkill current){
        String[] lore=switch(skill){
            case SPRINT->new String[]{"&7効果: 5秒間速度上昇III","&7クールダウン: 35秒"};
            case INVISIBLE->new String[]{"&7効果: 6秒間透明化","&7クールダウン: 40秒"};
            case SMOKE->new String[]{"&7鬼の視界を妨害する煙幕を展開","&7クールダウン: 35秒"};
            case ONI_STRIKE->new String[]{"&7鬼を怯ませて距離を作る","&7クールダウン: 45秒"};
            case HEAL->new String[]{"&7回復ゲージ完了後に体力8回復","&7絆なしでは自分だけを回復","&7攻撃・移動で中断 / CT: 45秒"};
            case OBSESSION->new String[]{"&7自分と鬼を8秒間発光","&7鬼の輪郭は全員から視認可能","&7クールダウン: 35秒"};
            case SAFE_LANDING->new String[]{"&7落下ダメージが発生する着地時に自動発動","&7落下ダメージ無効＋4秒間速度上昇III","&7クールダウン: 20秒"};
            case BLINK->new String[]{"&7移動方向へ4ブロック高速移動","&7壁抜け不可 / スタミナ4 / CT: 20秒"};
            case ECHO->new String[]{"&7スキン頭＋皮装備の残像と偽走行音を15秒間放つ","&7発動直後は音響感知を回避 / CT: 60秒"};
            case CLAIRVOYANCE->new String[]{"&7心臓・物資・負傷者・出口を色別表示","&7持続8秒 / CT: 45秒"};
            case UNYIELDING->new String[]{"&76秒間、致死ダメージを受けても体力1で耐える","&7CT: 60秒"};
            case SEALING_CIRCLE->new String[]{"&7足元へ10秒間の罠を設置","&7鬼が踏むと3秒鈍足 / CT: 35秒"};
            case RESONANCE->new String[]{"&7自分のスタミナ6を周囲の仲間へ分配","&7半径8m / CT: 30秒"};
            case SUBSTITUTE->new String[]{"&7視線先15m以内の仲間と位置交換","&7自分は発光・仲間は加速 / CT: 50秒"};
            case DESPERATE_RUN->new String[]{"&7体力30%以下限定。8秒間速度・損壊強化","&7終了後スタミナ0＋鈍足 / CT: 60秒"};
            case BUGEI->new String[]{"&7高く跳躍して鬼へ急接近し、強烈な飛び蹴りを叩き込む","&7射程18m / CT: 55秒"};
        };
        List<String> lines=new ArrayList<>(Arrays.asList(lore));
        lines.add(mainSkillUsage(skill));
        lines.add(current==skill?"&a✔ 選択中":"&eクリックして選択");
        return playerSkillIconItem(skill,"&b&l"+skill.display,"menu_skill:"+skill.name(),lines.toArray(new String[0]));
    }
    private void fillMenuBackground(Inventory menu,Material material){
        for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(material," ","menu_decor"));
    }
    private void setRoleHeader(Inventory menu,int slot,Material material,String title,String description){
        menu.setItem(slot,menuItem(material,title,"menu_decor",description));
    }
    private void setSkillGroup(Inventory menu,PlayerSkill current,int startSlot,PlayerSkill... skills){
        for(int i=0;i<skills.length;i++)menu.setItem(startSlot+i,playerSkillMenuItem(skills[i],current));
    }
    private void setPassiveGroup(Inventory menu,LinkedHashSet<PassiveSkill> selected,int startSlot,PassiveSkill... passives){
        for(int i=0;i<passives.length;i++){
            PassiveSkill passive=passives[i];
            menu.setItem(startSlot+i,passiveIconItem(passive,"&a&l"+passive.display,"menu_passive:"+passive.name(),
                "&7"+passive.description,passiveUsage(passive),selected.contains(passive)?"&a✔ 選択中":"&eクリックして選択"));
        }
    }
    private void addPlayerBuildFooter(Player p,Inventory menu,boolean passiveTab,int page,int maxPage){
        PlayerSkill current=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);
        LinkedHashSet<PassiveSkill> passives=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
        menu.setItem(45,menuItem(Material.NETHER_STAR,passiveTab?"&7スキル":"&b&lスキル","menu_page:skill:1","&7メインスキルを1つ選択"));
        menu.setItem(46,menuItem(Material.ARROW,page>1?"&e&l← 前のページ":"&8← 前のページ",page>1?(passiveTab?"menu_page:passive:"+(page-1):"menu_page:skill:"+(page-1)):"menu_decor"));
        menu.setItem(47,playerSkillIconItem(current,"&b選択中スキル","menu_decor","&f"+current.display));
        List<String> selectedLore=new ArrayList<>();selectedLore.add("&7最大2個");
        if(passives.isEmpty())selectedLore.add("&8未選択");
        else for(PassiveSkill passive:passives)selectedLore.add("&a✔ "+passive.display);
        menu.setItem(48,menuItem(Material.TOTEM_OF_UNDYING,"&aパッシブ &f"+passives.size()+"/2","menu_decor",selectedLore.toArray(new String[0])));
        menu.setItem(49,menuItem(Material.TOTEM_OF_UNDYING,passiveTab?"&a&lパッシブ":"&7パッシブ","menu_page:passive:1","&7パッシブを最大2個まで選択"));
        menu.setItem(50,menuItem(Material.BOOK,"&6&l役割について","menu_decor","&7能力を用途ごとに分類しています。","&7同じ役割の中から比較しやすくなっています。"));
        menu.setItem(51,menuItem(Material.PAPER,"&fページ &e"+page+"&7/&e"+maxPage,"menu_decor"));
        menu.setItem(52,menuItem(Material.ARROW,page<maxPage?"&e&l次のページ →":"&8次のページ →",page<maxPage?(passiveTab?"menu_page:passive:"+(page+1):"menu_page:skill:"+(page+1)):"menu_decor"));
        menu.setItem(53,menuItem(Material.COMPASS,"&e&lプリセット一覧","menu_page:presets","&7おすすめ構成・キャラクタープリセット"));
    }
    private void openSkillMenu(Player p){openSkillPage(p,1);}
    private void openSkillPage(Player p,int page){
        page=Math.max(1,Math.min(2,page));
        MenuHolder holder=new MenuHolder("skills:"+page);
        Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0スキル &7["+page+"/2]"));holder.inventory=menu;
        fillMenuBackground(menu,Material.BLUE_STAINED_GLASS_PANE);
        PlayerSkill current=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);
        menu.setItem(4,menuItem(Material.NETHER_STAR,"&b&lメインスキル &f1/1","menu_decor","&7選択中: &b"+current.display,"&7役割別に整理されています。"));
        if(page==1){
            setRoleHeader(menu,9,Material.FEATHER,"&b&l【機動・逃走】","&7距離を稼ぐ、移動する、危険地帯から離脱する");
            setSkillGroup(menu,current,10,PlayerSkill.SPRINT,PlayerSkill.SAFE_LANDING,PlayerSkill.BLINK,PlayerSkill.DESPERATE_RUN);
            setRoleHeader(menu,27,Material.ENDER_EYE,"&7&l【潜伏・撹乱】","&7鬼から姿や位置を隠し、追跡判断を狂わせる");
            setSkillGroup(menu,current,28,PlayerSkill.INVISIBLE,PlayerSkill.ECHO,PlayerSkill.SMOKE);
        }else{
            setRoleHeader(menu,9,Material.IRON_SWORD,"&c&l【対鬼・制圧】","&7鬼へ直接干渉し、追跡や攻撃を抑える");
            setSkillGroup(menu,current,10,PlayerSkill.ONI_STRIKE,PlayerSkill.BUGEI,PlayerSkill.SEALING_CIRCLE,PlayerSkill.OBSESSION);
            setRoleHeader(menu,27,Material.HEART_OF_THE_SEA,"&a&l【支援・探索】","&7仲間を助け、情報やスタミナを共有する");
            setSkillGroup(menu,current,28,PlayerSkill.HEAL,PlayerSkill.CLAIRVOYANCE,PlayerSkill.RESONANCE,PlayerSkill.SUBSTITUTE);
            setRoleHeader(menu,36,Material.SHIELD,"&6&l【生存】","&7致命的な状況を切り抜ける");
            setSkillGroup(menu,current,37,PlayerSkill.UNYIELDING);
        }
        addPlayerBuildFooter(p,menu,false,page,2);
        p.openInventory(menu);
    }
    private void openPassivePage(Player p,int page){
        page=Math.max(1,Math.min(4,page));
        MenuHolder holder=new MenuHolder("passives:"+page);
        Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0パッシブ &7["+page+"/4]"));holder.inventory=menu;
        fillMenuBackground(menu,Material.LIME_STAINED_GLASS_PANE);
        LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
        menu.setItem(4,menuItem(Material.TOTEM_OF_UNDYING,"&a&lパッシブスキル &f"+selected.size()+"/2","menu_decor","&7最大2個まで選択","&7選択済みを再クリックすると解除","&7役割別に整理されています。"));
        if(page==1){
            setRoleHeader(menu,9,Material.RABBIT_FOOT,"&b&l【機動・スタミナ】","&7走行・跳躍・方向転換など移動性能を強化");
            setPassiveGroup(menu,selected,10,PassiveSkill.LIGHT_FOOTED,PassiveSkill.DEEP_BREATH,PassiveSkill.NINJA_BLOOD,PassiveSkill.LEAP,PassiveSkill.QUICK_TURN,PassiveSkill.LAST_RESERVE);
            setRoleHeader(menu,27,Material.SCULK_SENSOR,"&7&l【隠密・陽動】","&7発見されにくくする、または偽情報で鬼を誘導する");
            setPassiveGroup(menu,selected,28,PassiveSkill.COWARDICE,PassiveSkill.SILENT_BREATH,PassiveSkill.DECOY);
        }else if(page==2){
            setRoleHeader(menu,9,Material.SHIELD,"&c&l【生存・対鬼】","&7耐久・回避・反撃など鬼との接触に強くなる");
            setPassiveGroup(menu,selected,10,PassiveSkill.DURABILITY_BOOST,PassiveSkill.ATTACK_BOOST,PassiveSkill.PARRY,PassiveSkill.CORNERED_RAT,PassiveSkill.DIVINE_TECHNIQUE);
            menu.setItem(27,menuItem(Material.IRON_CHESTPLATE,"&6役割メモ","menu_decor","&7チェイスや救援、鬼への牽制を担当したい人向け。","&7他ページの機動系と組み合わせるのも有効です。"));
        }else if(page==3){
            setRoleHeader(menu,9,Material.CRYING_OBSIDIAN,"&6&l【心臓損壊・作業】","&7心臓の発見・損壊・スキルチェックを強化");
            setPassiveGroup(menu,selected,10,PassiveSkill.FOCUS,PassiveSkill.EXORCISM,PassiveSkill.PRACTICED,PassiveSkill.AFTERMIND,PassiveSkill.DOUBLE_STAKES);
            setRoleHeader(menu,27,Material.HEART_OF_THE_SEA,"&a&l【支援】","&7仲間との連携や回復を強化");
            setPassiveGroup(menu,selected,28,PassiveSkill.BOND);
        }else{
            setRoleHeader(menu,9,Material.AMETHYST_SHARD,"&d&l【トークン系】","&7条件を満たして蓄積し、完成時に大きな効果へ変換");
            setPassiveGroup(menu,selected,10,PassiveSkill.THREE_PHASE,PassiveSkill.FOOTSTEP_THIEF,PassiveSkill.DEADLINE_ACCOUNTING);
            menu.setItem(27,menuItem(Material.BOOK,"&fトークンの特徴","menu_decor","&d三相: &f走・破・援を揃える","&7足跡泥棒: &f鬼の近くで未発見を維持","&c死線勘定: &f鬼の至近距離を無傷で切り抜ける"));
        }
        addPlayerBuildFooter(p,menu,true,page,4);
        p.openInventory(menu);
    }
    private void openPresetMenu(Player p){
        MenuHolder holder=new MenuHolder("presets");Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0プリセット一覧"));holder.inventory=menu;for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(Material.BLACK_STAINED_GLASS_PANE," ","menu_decor"));
        menu.setItem(4,menuItem(Material.BOOK,"&f&l通常プリセット","menu_decor","&7キャラクターを選ぶとメイン・パッシブを一括設定"));
        menu.setItem(11,customModelItem(Material.PLAYER_HEAD,2002,"&6&lプリセット: 伊神京也","menu_preset:IGAMI_KYOYA","&7読み: イガミキョウヤ","","&8メイン: &f破鬼撃","&8パッシブ: &f神喰 / 強靭","&8&m--------------------","&7&o「俺達は屈しない。」","","&eクリックで一括設定"));
        menu.setItem(12,customModelItem(Material.PLAYER_HEAD,2003,"&b&lプリセット: 字那美咲","menu_preset:AZANAMI_MISAKI","&7読み: アザナミサキ","","&8メイン: &f疾走","&8パッシブ: &f軽足 / 深呼吸","&8&m--------------------","&7&o「私に任せて。体力には自信があるんだ。」","","&eクリックで一括設定"));
        menu.setItem(13,customModelItem(Material.PLAYER_HEAD,2001,"&d&lプリセット: 有川風香","menu_preset:ARIKAWA_FUUKA","&7読み: アリカワフウカ","","&8メイン: &f煙幕","&8パッシブ: &f臆病 / 退魔","&8&m--------------------","&7&o「えぇ…あんなのと戦うんですか…？」","","&eクリックで一括設定"));
        menu.setItem(14,customModelItem(Material.PLAYER_HEAD,2004,"&3&lプリセット: 字那美蓮","menu_preset:AZANAMI_REN","&7読み: アザナミレン","","&8メイン: &f執念","&8パッシブ: &f軽足 / 強靭","&8&m--------------------","&7&o「鬼さんこちら…ってな！」","","&eクリックで一括設定"));
        menu.setItem(15,customModelItem(Material.PLAYER_HEAD,2005,"&a&lプリセット: 佐倉夏海","menu_preset:MEDIC","&7読み: サクラナツミ","","&8メイン: &f治療","&8パッシブ: &f集中 / 絆","&8&m--------------------","&7&o「治療は私に任せて！」","","&eクリックで一括設定"));
        menu.setItem(22,menuItem(Material.COMPASS,"&e&l戦術プリセット","menu_decor","&7相性の良いスキルをまとめたおすすめ構成"));
        menu.setItem(27,menuItem(Material.IRON_SWORD,"&c&l返し刃","menu_build:COUNTER_BLADE","&7破鬼撃＋受け流し＋神喰","&8攻撃をいなして反撃する対鬼構成","&eクリックで一括設定"));menu.setItem(28,menuItem(Material.CRYING_OBSIDIAN,"&4&l破壊工作員","menu_build:SABOTEUR","&7霊視＋手馴れ＋退魔","&8心臓発見と高速損壊に特化","&eクリックで一括設定"));menu.setItem(29,menuItem(Material.CHORUS_FRUIT,"&a&l守護走者","menu_build:GUARD_RUNNER","&7身代わり＋強靭＋軽足","&8仲間を救出して追跡を引き受ける構成","&eクリックで一括設定"));menu.setItem(30,menuItem(Material.NETHER_STAR,"&c&l背水","menu_build:LAST_STAND","&7決死行＋土壇場＋手馴れ","&8瀕死状態から心臓を押し切る構成","&eクリックで一括設定"));menu.setItem(31,menuItem(Material.AMETHYST_SHARD,"&5&l輪唱","menu_build:CHORUS","&7共鳴＋深呼吸＋軽足","&8味方のスタミナを支える集団行動構成","&eクリックで一括設定"));menu.setItem(32,menuItem(Material.ENCHANTING_TABLE,"&9&l罠師","menu_build:TRAPPER","&7封鬼陣＋息殺し＋臆病","&8罠設置後に気配を消して離脱する構成","&eクリックで一括設定"));
        menu.setItem(38,menuItem(Material.NETHER_STAR,"&c&lEXプリセット","menu_decor","&7特殊な能力構成を持つプリセット"));
        List<ItemStack> ex=new ArrayList<>();
        ex.add(customModelItem(Material.PLAYER_HEAD,2006,"&c&lEXプリセット: 赤坂陽彩","menu_preset:AKASAKA_HIIRO","&7読み: アカサカヒイロ","","&8メイン: &f安定着地","&8パッシブ: &f忍びの血統 / 跳躍","&8&m--------------------","&7&o「アタシを捕まえられるかな？」","","&eクリックで一括設定"));
        ex.add(customModelItem(Material.PLAYER_HEAD,2007,"&a&lEXプリセット: 加賀谷凛音","menu_preset:KAGAYA_RION","&7読み: カガヤリオン","","&8メイン: &f瞬歩","&8パッシブ: &f急転 / 窮鼠","&8&m--------------------","&7&o「魅せてあげるよ！」","","&eクリックで一括設定"));
        ex.add(customModelItem(Material.PLAYER_HEAD,2008,"&9&lEXプリセット: 天内伊御奈","menu_preset:AMANAI_IONA","&7読み: アマナイイオナ","","&8メイン: &f残響","&8パッシブ: &f息殺し / 陽動","&8&m--------------------","&7&o「残念ね…そっちは偽物よ。」","","&eクリックで一括設定"));
        if(!isAzakujiLocked())ex.add(customModelItem(Material.PLAYER_HEAD,2010,"&6&lEXプリセット: 字九字ひろ","menu_preset:AZAKUJI_HIRO","&7読み: アザクジヒロ","","&8メイン: &f武芸","&8パッシブ: &f神喰 / 神技","&8&m--------------------","&7&o「神だろうが鬼だろうが、喰らってみせる。」","","&eクリックで一括設定"));
        int[] exSlots=ex.size()==4?new int[]{39,41,43,45}:new int[]{40,42,44};for(int i=0;i<ex.size();i++)menu.setItem(exSlots[i],ex.get(i));
        menu.setItem(49,menuItem(Material.ARROW,"&e&l← スキル設定へ戻る","menu_page:skills"));p.openInventory(menu);
    }
    private Material passiveMaterial(PassiveSkill passive){return switch(passive){case LIGHT_FOOTED->Material.RABBIT_FOOT;case DEEP_BREATH->Material.GHAST_TEAR;case FOCUS->Material.AMETHYST_SHARD;case ATTACK_BOOST->Material.IRON_SWORD;case DURABILITY_BOOST->Material.IRON_CHESTPLATE;case EXORCISM->Material.BLAZE_POWDER;case COWARDICE->Material.FERMENTED_SPIDER_EYE;case BOND->Material.HEART_OF_THE_SEA;case NINJA_BLOOD->Material.NETHERITE_BOOTS;case LEAP->Material.FIREWORK_ROCKET;case AFTERMIND->Material.ECHO_SHARD;case PARRY->Material.SHIELD;case QUICK_TURN->Material.ENDER_PEARL;case SILENT_BREATH->Material.SCULK_SENSOR;case LAST_RESERVE->Material.HONEY_BOTTLE;case PRACTICED->Material.CLOCK;case DECOY->Material.SNOWBALL;case CORNERED_RAT->Material.GOLDEN_CARROT;case DIVINE_TECHNIQUE->Material.NETHER_STAR;case DOUBLE_STAKES->Material.GOLD_INGOT;case THREE_PHASE->Material.AMETHYST_SHARD;case FOOTSTEP_THIEF->Material.RABBIT_FOOT;case DEADLINE_ACCOUNTING->Material.CLOCK;};}
    private void applyTacticalPreset(Player player,String id){PlayerSkill skill;PassiveSkill first,second;String name;switch(id){case "COUNTER_BLADE"->{name="返し刃";skill=PlayerSkill.ONI_STRIKE;first=PassiveSkill.PARRY;second=PassiveSkill.ATTACK_BOOST;}case "SABOTEUR"->{name="破壊工作員";skill=PlayerSkill.CLAIRVOYANCE;first=PassiveSkill.PRACTICED;second=PassiveSkill.EXORCISM;}case "GUARD_RUNNER"->{name="守護走者";skill=PlayerSkill.SUBSTITUTE;first=PassiveSkill.DURABILITY_BOOST;second=PassiveSkill.LIGHT_FOOTED;}case "LAST_STAND"->{name="背水";skill=PlayerSkill.DESPERATE_RUN;first=PassiveSkill.LAST_RESERVE;second=PassiveSkill.PRACTICED;}case "CHORUS"->{name="輪唱";skill=PlayerSkill.RESONANCE;first=PassiveSkill.DEEP_BREATH;second=PassiveSkill.LIGHT_FOOTED;}case "TRAPPER"->{name="罠師";skill=PlayerSkill.SEALING_CIRCLE;first=PassiveSkill.SILENT_BREATH;second=PassiveSkill.COWARDICE;}default->{return;}}selectedSkill.put(player.getUniqueId(),skill);selectedPresetNames.remove(player.getUniqueId());LinkedHashSet<PassiveSkill> passives=selectedPassives.computeIfAbsent(player.getUniqueId(),key->new LinkedHashSet<>());passives.clear();passives.add(first);passives.add(second);msg(player,"戦術プリセット &e"+name+" &fを設定しました。");}
    private void openOniMenu(Player p){
        MenuHolder holder=new MenuHolder("oni");Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0鬼構成"));holder.inventory=menu;
        for(int slot=0;slot<18;slot++)menu.setItem(slot,menuItem(Material.RED_STAINED_GLASS_PANE,"&4鬼選択","menu_decor"));
        for(int slot=18;slot<36;slot++)menu.setItem(slot,menuItem(Material.GRAY_STAINED_GLASS_PANE,"&8固有スキル説明","menu_decor"));
        for(int slot=36;slot<54;slot++)menu.setItem(slot,menuItem(Material.PURPLE_STAINED_GLASS_PANE,"&5鬼パッシブ枠","menu_decor"));
        if(isJakutsukiLocked()&&selectedOniType==OniType.JAKUTSUKI)selectedOniType=OniType.DAKKO;OniType shown=selectedOniType==null?OniType.DAKKO:selectedOniType;LinkedHashSet<OniPassiveSkill> passives=selectedOniPassives;
        if(isJakutsukiLocked()){menu.setItem(12,customModelItem(Material.CHAINMAIL_HELMET,1001,"&5&l堕狐","menu_oni:DAKKO","&7奇襲・攪乱型","&d狐渡り &7/ &6分霊 &7/ &6狐火 &7/ &d天来",shown==OniType.DAKKO?"&a現在表示・選択中":"&eクリックして選択"));menu.setItem(14,menuItem(Material.NETHERITE_AXE,"&4&l鬼神","menu_oni:KISHIN","&7正面突破・追跡型","&c鬼突 &7/ &4地砕 &7/ &4鬼吼 &7/ &6剛身",shown==OniType.KISHIN?"&a現在表示・選択中":"&eクリックして選択"));}
        else{menu.setItem(11,customModelItem(Material.CHAINMAIL_HELMET,1001,"&5&l堕狐","menu_oni:DAKKO","&7奇襲・攪乱型","&d狐渡り &7/ &6分霊 &7/ &6狐火 &7/ &d天来",shown==OniType.DAKKO?"&a現在表示・選択中":"&eクリックして選択"));menu.setItem(13,customModelItem(Material.PLAYER_HEAD,2009,"&8&lEX鬼 蛇窟姫 &7(ジャクツキ)","menu_oni:JAKUTSUKI","&7領域制圧・終盤強化型","&5黒鏡 &7/ &5薙ぎ払い &7/ &8蛇崩 &7/ &4解放 &7/ &5黒の波動","&c基礎体力200 / 心臓破壊ごとに最大体力-10","&c固有ルール: 鬼の心臓が8個に増加","&4偽心臓4個 &7――接近すると爆発","&7爆発は地形を破壊せず、ダメージ＋発光を付与","&f全心臓破壊時: 全ぷれいやーへ『蛇斬』支給","&6救援: 残存4人以下 または 心臓残り3個でアザクジ参戦",shown==OniType.JAKUTSUKI?"&a現在表示・選択中":"&eクリックして選択"));menu.setItem(15,menuItem(Material.NETHERITE_AXE,"&4&l鬼神","menu_oni:KISHIN","&7正面突破・追跡型","&c鬼突 &7/ &4地砕 &7/ &4鬼吼 &7/ &6剛身",shown==OniType.KISHIN?"&a現在表示・選択中":"&eクリックして選択"));}
        if(shown==OniType.DAKKO){menu.setItem(20,menuItem(Material.ENDER_PEARL,"&d&l狐渡り","menu_decor","&7視線先へ瞬間移動","&7心臓破壊で射程低下・CT増加"));menu.setItem(24,menuItem(Material.FOX_SPAWN_EGG,"&6&l分霊","menu_decor","&7弱体化した堕狐鬼Botの分身体を2体召喚","&7約5秒だけ追跡・攻撃して消滅","&7心臓1個破壊で解放"));menu.setItem(29,menuItem(Material.BLAZE_POWDER,"&6&l狐火","menu_decor","&7周囲へ固定ダメージ＋炎上","&7対集団用 / 心臓2個破壊で解放"));menu.setItem(33,menuItem(Material.FIREWORK_STAR,"&d&l天来","menu_decor","&7視線先の上空から飛来して範囲攻撃","&7心臓3個破壊で解放"));}
        else if(shown==OniType.KISHIN){menu.setItem(20,menuItem(Material.FIREWORK_ROCKET,"&c&l鬼突","menu_decor","&7前方へ強力な突進","&7心臓破壊で速度低下・CT増加"));menu.setItem(22,menuItem(Material.IRON_AXE,"&4&l地砕","menu_decor","&7周囲を攻撃し、吹き飛ばして鈍足付与","&7心臓1個破壊で解放"));menu.setItem(24,menuItem(Material.GOAT_HORN,"&4&l鬼吼","menu_decor","&7咆哮で周囲を攻撃し、鈍足＋弱体化","&c威力4 / 半径8m / CT28秒","&7心臓2個破壊で解放"));menu.setItem(31,menuItem(Material.SHIELD,"&6&l剛身","menu_decor","&76秒間、耐性I＋移動速度上昇II＋攻撃力上昇II","&7追跡・接近戦を大きく強化 / CT35秒","&7心臓3個破壊で解放"));}else{menu.setItem(20,menuItem(Material.BLACK_STAINED_GLASS,"&5&l黒鏡","menu_decor","&7SHIFT+右クリックで鏡を設置","&7右クリックで設置地点へ転移 / 長CT"));menu.setItem(22,menuItem(Material.NETHERITE_HOE,"&5&l薙ぎ払い","menu_decor","&7前方の扇状範囲を一閃"));menu.setItem(24,menuItem(Material.SILVERFISH_SPAWN_EGG,"&8&l蛇崩","menu_decor","&7大量の蛇を一定時間召喚","&7蛇に噛まれると短時間発光"));menu.setItem(29,menuItem(Material.ECHO_SHARD,"&d&l黒の波動","menu_decor","&7溜めの後、前方へ強力な貫通範囲攻撃","&7複数のぷれいやーを一直線に貫く","&c威力16 / 射程28m / 溜め2.5秒"));menu.setItem(31,menuItem(Material.NETHER_STAR,"&4&l解放","menu_decor","&7心臓が残り1つの時のみ使用可能","&7即狂化＋脱出口を60秒封印"));menu.setItem(33,menuItem(Material.CRYING_OBSIDIAN,"&4&l蛇窟の偽心臓","menu_decor","&7本物8個＋偽物4個が配置される","&7偽物へ接近すると爆発","&cダメージ＋発光 &7/ 地形破壊なし","&7探知機・霊視でも本物と区別できない"));}
        menu.setItem(38,menuItem(Material.COMPASS,"&5&l鬼パッシブ設定","menu_oni_passive_page:1","&7役割別5ページから最大4個選択","&d索敵 / 心臓防衛 / 戦闘 / 妨害 / 機動"));
        int slot=40;for(OniPassiveSkill passive:passives){if(slot>48)break;menu.setItem(slot++,oniPassiveIconItem(passive,"&d"+passive.display,"menu_oni_passive_page:1","&7選択中"));}
        menu.setItem(53,menuItem(Material.NETHER_STAR,"&d&l鬼パッシブ &f"+passives.size()+"/4","menu_oni_passive_page:1","&eクリックして役割別ページを開く"));p.openInventory(menu);
    }
    private void openOniPassivePage(Player p,int page){
        page=Math.max(1,Math.min(5,page));MenuHolder holder=new MenuHolder("oni_passives:"+page);Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &5鬼パッシブ ["+page+"/5]"));holder.inventory=menu;for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(Material.BLACK_STAINED_GLASS_PANE," ","menu_decor"));
        OniPassiveSkill[] group;String title;Material icon;String desc;
        if(page==1){title="【索敵・追跡】";icon=Material.COMPASS;desc="獲物を見失わず、再発見を強化";group=new OniPassiveSkill[]{OniPassiveSkill.BLOOD_SCENT,OniPassiveSkill.HUNT_RECORD,OniPassiveSkill.EXECUTION,OniPassiveSkill.SCENT_TRAIL,OniPassiveSkill.HUNTERS_INSTINCT,OniPassiveSkill.FOOTSTEP_HUNTER,OniPassiveSkill.OBSESSION_HUNT};}
        else if(page==2){title="【心臓防衛】";icon=Material.CRYING_OBSIDIAN;desc="心臓への攻撃へ反応し、終盤を守る";group=new OniPassiveSkill[]{OniPassiveSkill.GUARDIAN,OniPassiveSkill.INTERFERENCE,OniPassiveSkill.PULSE,OniPassiveSkill.BLOOD_MARK,OniPassiveSkill.HEART_EYE,OniPassiveSkill.CURSED_VEIN,OniPassiveSkill.BACKFLOW,OniPassiveSkill.LAST_FORTRESS};}
        else if(page==3){title="【戦闘・制圧】";icon=Material.NETHERITE_AXE;desc="接近戦・連撃・圧力を強化";group=new OniPassiveSkill[]{OniPassiveSkill.PREDATION,OniPassiveSkill.MOMENTUM,OniPassiveSkill.ANCHOR,OniPassiveSkill.BLOOD_FRENZY,OniPassiveSkill.PRESSURE,OniPassiveSkill.HUNTING_GROUND,OniPassiveSkill.FINISHER_CHASE};}
        else if(page==4){title="【妨害・対スキル】";icon=Material.ENCHANTING_TABLE;desc="ぷれいやーのスキルと妨害へ対抗";group=new OniPassiveSkill[]{OniPassiveSkill.GRUDGE_RETURN,OniPassiveSkill.SEE_THROUGH,OniPassiveSkill.SPELL_BREAK,OniPassiveSkill.SKILL_SEAL,OniPassiveSkill.ADAPTATION};}
        else{title="【機動・特殊】";icon=Material.FEATHER;desc="追跡初動・帰還・スキル回転を強化";group=new OniPassiveSkill[]{OniPassiveSkill.CRAVING,OniPassiveSkill.MASTERY,OniPassiveSkill.BEAST_PATH,OniPassiveSkill.AMBUSH,OniPassiveSkill.TERRITORY,OniPassiveSkill.HOMING};}
        menu.setItem(4,menuItem(icon,"&d&l"+title,"menu_decor","&7"+desc));int[] slots={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};for(int i=0;i<group.length;i++){OniPassiveSkill passive=group[i];menu.setItem(slots[i],oniPassiveIconItem(passive,"&5&l"+passive.display,"menu_oni_passive:"+passive.name(),"&7"+passive.description,selectedOniPassives.contains(passive)?"&a✔ 選択中":"&eクリックして選択"));}
        menu.setItem(45,menuItem(Material.BARRIER,"&c&l← 鬼選択へ","menu_oni_back","&7鬼種・固有スキル画面へ戻る"));menu.setItem(47,menuItem(Material.ARROW,page>1?"&e&l← 前ページ":"&8← 前ページ",page>1?"menu_oni_passive_page:"+(page-1):"menu_decor"));menu.setItem(49,menuItem(Material.NETHER_STAR,"&d&l選択中 &f"+selectedOniPassives.size()+"/4","menu_decor","&7最大4個まで設定可能"));menu.setItem(51,menuItem(Material.ARROW,page<5?"&e&l次ページ →":"&8次ページ →",page<5?"menu_oni_passive_page:"+(page+1):"menu_decor"));menu.setItem(53,menuItem(Material.BOOK,"&f&lページ "+page+"/5","menu_decor","&7役割ごとに鬼パッシブを分類"));p.openInventory(menu);
    }
    private Material oniPassiveMaterial(OniPassiveSkill passive){return switch(passive){
        case CRAVING->Material.ROTTEN_FLESH;case BLOOD_SCENT->Material.REDSTONE;case GUARDIAN->Material.CRYING_OBSIDIAN;case PREDATION->Material.BONE;case INTERFERENCE->Material.TRIPWIRE_HOOK;case MASTERY->Material.CLOCK;case MOMENTUM->Material.SUGAR;case ANCHOR->Material.ANVIL;case PULSE->Material.ECHO_SHARD;case EXECUTION->Material.SPECTRAL_ARROW;case HUNT_RECORD->Material.WRITABLE_BOOK;case GRUDGE_RETURN->Material.AMETHYST_SHARD;case BLOOD_MARK->Material.REDSTONE_TORCH;
        case SCENT_TRAIL->Material.CRIMSON_FUNGUS;case HUNTERS_INSTINCT->Material.COMPASS;case FOOTSTEP_HUNTER->Material.LEATHER_BOOTS;case OBSESSION_HUNT->Material.RED_DYE;case HEART_EYE->Material.ENDER_EYE;case CURSED_VEIN->Material.FERMENTED_SPIDER_EYE;case BACKFLOW->Material.FIREWORK_STAR;case LAST_FORTRESS->Material.OBSIDIAN;case BLOOD_FRENZY->Material.NETHERITE_SWORD;case PRESSURE->Material.SOUL_SAND;case HUNTING_GROUND->Material.TARGET;case FINISHER_CHASE->Material.GOLDEN_AXE;case SEE_THROUGH->Material.SPYGLASS;case SPELL_BREAK->Material.AMETHYST_CLUSTER;case SKILL_SEAL->Material.BARRIER;case ADAPTATION->Material.SCULK_CATALYST;case BEAST_PATH->Material.RABBIT_FOOT;case AMBUSH->Material.FIREWORK_ROCKET;case TERRITORY->Material.RESPAWN_ANCHOR;case HOMING->Material.RECOVERY_COMPASS;};}
    private ItemStack createHeartMarker(){ItemStack i=item(Material.ARMOR_STAND,"&4&l鬼の心臓地点マーカー","heart_marker");ItemMeta m=i.getItemMeta();m.setLore(List.of(cc("&7設置地点に心臓を生成します。"),cc("&7設置後は透明・固定・無敵になります。")));i.setItemMeta(m);return i;}
    private void equipDakkoArmor(Player p){
        p.getInventory().setHelmet(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"));
        p.getInventory().setChestplate(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"));
        p.getInventory().setLeggings(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"));
        p.getInventory().setBoots(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"));
    }
    private ItemStack oniArmor(Material material,String name){ItemStack i=item(material,name,"dakko_armor");ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setCustomModelData(dakkoArmorModel(material));m.setLore(List.of(cc("&8堕狐専用装備")));i.setItemMeta(m);return i;}
    private int dakkoArmorModel(Material material){return switch(material){case CHAINMAIL_HELMET->1001;case CHAINMAIL_CHESTPLATE->1002;case CHAINMAIL_LEGGINGS->1003;case CHAINMAIL_BOOTS->1004;default->1001;};}
    private boolean isDakkoArmor(ItemStack i){if(i==null||!i.hasItemMeta())return false;return "dakko_armor".equals(i.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING));}
    private void equipJakutsukiArmor(Player p){
        p.getInventory().setHelmet(jakutsukiArmor(Material.IRON_HELMET,"&5蛇窟姫の髪飾り"));
        p.getInventory().setChestplate(jakutsukiArmor(Material.IRON_CHESTPLATE,"&5蛇窟姫の装束"));
        p.getInventory().setLeggings(jakutsukiArmor(Material.IRON_LEGGINGS,"&5蛇窟姫の袴"));
        p.getInventory().setBoots(jakutsukiArmor(Material.IRON_BOOTS,"&5蛇窟姫の足袋"));
    }
    private ItemStack jakutsukiArmor(Material material,String name){
        ItemStack i=item(material,name,"jakutsuki_armor");ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setCustomModelData(jakutsukiArmorModel(material));m.setLore(List.of(cc("&8蛇窟姫専用装備"),cc("&7リソースパック v18 対応")));i.setItemMeta(m);return i;
    }
    private int jakutsukiArmorModel(Material material){return switch(material){case IRON_HELMET->1101;case IRON_CHESTPLATE->1102;case IRON_LEGGINGS->1103;case IRON_BOOTS->1104;default->1101;};}
    private boolean isJakutsukiArmor(ItemStack i){if(i==null||!i.hasItemMeta())return false;return "jakutsuki_armor".equals(i.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING));}
    private boolean isTestGm(Player p){return state==GameState.RUNNING&&testParticipant!=null&&testParticipant.equals(p.getUniqueId());}
    private void giveTestPlayBook(Player p){p.getInventory().setItem(8,createTestPlayBook());msg(p,"&eテストプレイ本 &fをホットバーへ配布しました。");}
    private ItemStack createGmBook(){
        ItemStack book=new ItemStack(Material.WRITTEN_BOOK);BookMeta m=(BookMeta)book.getItemMeta();
        m.title(Component.text("鬼げぇむ GM操作本",NamedTextColor.DARK_RED));m.author(Component.text("鬼げぇむ"));
        Component page1=Component.text("【心臓の配置方式】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ ランダム配置\n",NamedTextColor.GREEN,"/og heartmode random"))
                .append(Component.text("登録候補から設定個数を抽選\n\n",NamedTextColor.GRAY))
                .append(button("▶ 手動配置\n",NamedTextColor.GOLD,"/og heartmode manual"))
                .append(Component.text("専用マーカーの全地点に生成",NamedTextColor.GRAY));
        Component page2=Component.text("【心臓マーカー】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ マーカーを受け取る\n",NamedTextColor.GOLD,"/og marker"))
                .append(button("▶ マーカー数を確認\n",NamedTextColor.AQUA,"/og markers"))
                .append(Component.text("\n設置したマーカーは自動で透明になります。",NamedTextColor.GRAY));
        Component page3=Component.text("【ゲーム操作】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ 鬼を選択\n",NamedTextColor.GOLD,"/og onimenu"))
                .append(button("▶ 選択した鬼で開始\n",NamedTextColor.RED,"/og start"))
                .append(button("▶ 状態を確認\n",NamedTextColor.AQUA,"/og status"))
                .append(button("▶ ゲームを停止",NamedTextColor.DARK_RED,"/og stop"));
        Component page4=Component.text("【ぷれいやーBotテスト】\n\n",NamedTextColor.DARK_RED)
                .append(Component.text("GMがぷれいやーになり、鬼Botと対戦します。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 堕狐Bot戦を開始\n",NamedTextColor.LIGHT_PURPLE,"/og botgame dakko"))
                .append(button("▶ 鬼神Bot戦を開始",NamedTextColor.RED,"/og botgame kishin"));
        Component page5=Component.text("【鬼対ぷれいやーBot】\n\n",NamedTextColor.DARK_RED)
                .append(Component.text("GMが鬼になり、革装備のぷれいやーBot 5体と対戦します。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 堕狐で開始\n",NamedTextColor.LIGHT_PURPLE,"/og onibotgame dakko 5"))
                .append(button("▶ 鬼神で開始",NamedTextColor.RED,"/og onibotgame kishin 5"));
        Component page6=Component.text("【ルートチェスト】\n\n",NamedTextColor.GOLD)
                .append(Component.text("現在地をランダム配置候補として登録します。候補なしなら心臓周辺へ自動配置。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 現在地を追加/削除\n",NamedTextColor.GOLD,"/og chest"))
                .append(button("▶ 候補数を確認",NamedTextColor.AQUA,"/og chests"));
        Component page7=Component.text("【スキル設定チェスト】\n\n",NamedTextColor.BLUE)
                .append(Component.text("ロビーのチェストへ視点を合わせて登録してください。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 視線先チェストを登録",NamedTextColor.AQUA,"/og skillchest"));
        Component page8=Component.text("【鬼設定チェスト】\n\n",NamedTextColor.DARK_PURPLE)
                .append(Component.text("鬼選択・固有スキル説明・鬼パッシブ4枠を扱います。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 視線先チェストを登録\n",NamedTextColor.LIGHT_PURPLE,"/og onichest"))
                .append(button("▶ 鬼構成UIを開く",NamedTextColor.GOLD,"/og onimenu"));
        Component page9=Component.text("【テスト区域の境界】\n\n",NamedTextColor.DARK_AQUA)
                .append(Component.text("200×200の外周にワールドボーダーを表示するか選べます。\n\n",NamedTextColor.GRAY))
                .append(button("▶ ボーダーを有効化\n",NamedTextColor.GREEN,"/og testborder on"))
                .append(button("▶ ボーダーを無効化",NamedTextColor.RED,"/og testborder off"));
        m.pages(List.of(page1,page2,page3,page4,page5,page6,page7,page8,page9));book.setItemMeta(m);return book;
    }
    private ItemStack createTestPlayBook(){ItemStack book=new ItemStack(Material.WRITTEN_BOOK);BookMeta m=(BookMeta)book.getItemMeta();m.title(Component.text("鬼げぇむ テストプレイ",NamedTextColor.GOLD));m.author(Component.text("鬼げぇむ"));Component page1=Component.text("【心臓テスト】\n\n",NamedTextColor.DARK_RED).append(Component.text("破壊したい心臓へ視点を合わせて押してください。\n\n",NamedTextColor.GRAY)).append(button("▶ 視線先の心臓を破壊\n",NamedTextColor.RED,"/og testdestroy")).append(button("▶ 現在状態を確認",NamedTextColor.AQUA,"/og status"));Component page2=Component.text("【BGMテスト】\n\n",NamedTextColor.DARK_PURPLE).append(button("▶ 最終局面BGMを再生\n",NamedTextColor.GREEN,"/og bgmtest")).append(button("▶ BGMを停止",NamedTextColor.RED,"/og bgmstop"));Component page3=Component.text(playerSideTest?"【鬼Botテスト】\n\n":"【鬼スキル確認】\n\n",NamedTextColor.DARK_RED).append(Component.text(playerSideTest?"あなたはぷれいやーです。\n堕狐Botは狐火・天来を含む固有技を使います。":"堕狐\n2: 狐渡り\n3: 分霊\n4: 狐火\n5: 天来\n\n鬼神\n2: 鬼突\n3: 地砕\n4: 鬼吼\n5: 剛身\n\n各アイテムを右クリック",NamedTextColor.GRAY));Component page4=Component.text("【テスト終了】\n\n",NamedTextColor.DARK_RED).append(Component.text("装備・速度・BGM・一時参加状態を解除してロビーへ戻ります。\n\n",NamedTextColor.GRAY)).append(button("▶ テストプレイを終了",NamedTextColor.RED,"/og stop"));m.pages(List.of(page1,page2,page3,page4));book.setItemMeta(m);return book;}
    private Component button(String text,NamedTextColor color,String command){return Component.text(text,color).clickEvent(ClickEvent.runCommand(command));}
    private List<Location> selectRandomHearts(){List<Location> locations=new ArrayList<>(getConfig().getStringList("locations.hearts").stream().map(LocationStore::decode).filter(Objects::nonNull).toList());Collections.shuffle(locations);int wanted=oniType==OniType.JAKUTSUKI?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8))+Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4)):Math.max(1,getConfig().getInt("random-heart-count",5));int count=Math.min(wanted,locations.size());return new ArrayList<>(locations.subList(0,count));}
    private List<Location> scanHeartMarkers(boolean hide){
        String markerName=getConfig().getString("heart-marker-name","鬼の心臓地点");Map<String,Location> unique=new LinkedHashMap<>();
        for(World world:Bukkit.getWorlds())for(ArmorStand stand:world.getEntitiesByClass(ArmorStand.class))if(markerName.equals(stand.getCustomName())){if(hide)configureMarker(stand);Location l=stand.getLocation().getBlock().getLocation();unique.put(LocationStore.encode(l),l);}
        return new ArrayList<>(unique.values());
    }
    private Vector horizontalDirection(Location base){Vector direction=base.getDirection().setY(0);return direction.lengthSquared()<0.01?new Vector(0,0,1):direction.normalize();}
    private void configureNormalArena(Player admin){
        Location center=admin.getLocation().clone();
        int size=Math.max(40,getConfig().getInt("arena-setup.size",200)),half=size/2-5;
        int heartCount=Math.max(12,getConfig().getInt("arena-setup.heart-candidate-count",16));
        Random random=new Random();List<Location> occupied=new ArrayList<>();
        Location playerSpawn=randomTestSurface(center,random,half,occupied,0);occupied.add(playerSpawn);
        Location oniSpawn=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(70,size*.35));occupied.add(oniSpawn);
        Location exit=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(38,size*.19));occupied.add(exit);
        List<String> hearts=new ArrayList<>();
        for(int i=0;i<heartCount;i++){Location heart=randomTestSurface(center,random,half,occupied,Math.min(14,size*.07));occupied.add(heart);hearts.add(LocationStore.encode(heart.getBlock().getLocation()));}
        LocationStore.set(getConfig(),"arena-setup.center",center);
        LocationStore.set(getConfig(),"locations.player-spawn",playerSpawn);
        LocationStore.set(getConfig(),"locations.oni-spawn",oniSpawn);
        LocationStore.set(getConfig(),"locations.exit",exit);
        getConfig().set("locations.hearts",hearts);
        getConfig().set("heart-placement-mode","random");
        getConfig().set("arena-setup.enabled",true);
        saveConfig();
        msg(admin,"&a通常試合用アリーナを一括設定しました。");
        msg(admin,"&7範囲: &e"+size+"x"+size+" &7/ 心臓候補: &e"+hearts.size()+"個");
        msg(admin,"&7ぷれいやー開始・鬼開始・脱出口・心臓候補・ワールドボーダー中心を自動設定しました。");
    }
    private void applyNormalArenaWorldBorder(){
        if(!getConfig().getBoolean("arena-setup.enabled",false)||!getConfig().getBoolean("arena-setup.world-border-enabled",true))return;
        Location center=LocationStore.get(getConfig(),"arena-setup.center");if(center==null||center.getWorld()==null)return;
        WorldBorder border=center.getWorld().getWorldBorder();
        if(savedTestWorldBorder==null)savedTestWorldBorder=new SavedWorldBorder(center.getWorld(),border.getCenter().clone(),border.getSize(),border.getWarningDistance(),border.getWarningTime(),border.getDamageAmount(),border.getDamageBuffer());
        border.setCenter(center.getX(),center.getZ());
        border.setSize(Math.max(40,getConfig().getDouble("arena-setup.size",200)));
        border.setWarningDistance(Math.max(0,getConfig().getInt("arena-setup.world-border-warning-distance",8)));
        border.setWarningTime(0);border.setDamageAmount(0);
    }
    private TestArenaLayout createRandomTestArena(Location center){Random random=new Random();int size=Math.max(40,getConfig().getInt("test-arena.size",200)),half=size/2-5,heartCount=oniType==OniType.JAKUTSUKI?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8))+Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4)):Math.max(1,getConfig().getInt("test-arena.heart-count",5)),lootCount=Math.max(1,getConfig().getInt("test-arena.loot-chest-count",5));List<Location> occupied=new ArrayList<>();Location player=randomTestSurface(center,random,half,occupied,0);occupied.add(player);Location oni=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(70,size*.35));occupied.add(oni);Location exit=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(38,size*.19));occupied.add(exit);List<Location> hearts=new ArrayList<>();for(int i=0;i<heartCount;i++){Location location=randomTestSurface(center,random,half,occupied,Math.min(14,size*.07));hearts.add(location);occupied.add(location);}List<Location> loot=new ArrayList<>();for(int i=0;i<lootCount;i++){Location location=randomTestSurface(center,random,half,occupied,Math.min(8,size*.04));loot.add(location);occupied.add(location);}return new TestArenaLayout(player,oni,exit,hearts,loot);}
    private void applyTestWorldBorder(Location center){WorldBorder border=center.getWorld().getWorldBorder();if(savedTestWorldBorder==null)savedTestWorldBorder=new SavedWorldBorder(center.getWorld(),border.getCenter().clone(),border.getSize(),border.getWarningDistance(),border.getWarningTime(),border.getDamageAmount(),border.getDamageBuffer());border.setCenter(center.getX(),center.getZ());border.setSize(Math.max(40,getConfig().getDouble("test-arena.size",200)));border.setWarningDistance(Math.max(0,getConfig().getInt("test-arena.world-border-warning-distance",8)));border.setWarningTime(0);border.setDamageAmount(0);}
    private void restoreTestWorldBorder(){if(savedTestWorldBorder==null)return;SavedWorldBorder saved=savedTestWorldBorder;WorldBorder border=saved.world().getWorldBorder();border.setCenter(saved.center().getX(),saved.center().getZ());border.setSize(saved.size());border.setWarningDistance(saved.warningDistance());border.setWarningTime(saved.warningTime());border.setDamageAmount(saved.damageAmount());border.setDamageBuffer(saved.damageBuffer());savedTestWorldBorder=null;}
    private Location randomTestSurfaceFarFrom(Location center,Random random,int half,List<Location> occupied,double minDistance){for(int attempt=0;attempt<100;attempt++){Location location=randomTestSurface(center,random,half,List.of(),0);boolean far=true;for(Location other:occupied)if(other.getWorld().equals(location.getWorld())&&other.distanceSquared(location)<minDistance*minDistance){far=false;break;}if(far)return location;}return randomTestSurface(center,random,half,occupied,8);}
    private Location randomTestSurface(Location center,Random random,int half,List<Location> occupied,double separation){World world=center.getWorld();for(int attempt=0;attempt<120;attempt++){int x=center.getBlockX()+random.nextInt(half*2+1)-half,z=center.getBlockZ()+random.nextInt(half*2+1)-half,y=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);Location location=new Location(world,x,y+1,z,center.getYaw(),center.getPitch());Block ground=world.getBlockAt(x,y,z);if(!ground.getType().isSolid()||ground.isLiquid()||!location.getBlock().isPassable()||!location.clone().add(0,1,0).getBlock().isPassable())continue;boolean clear=true;for(Location other:occupied)if(other.getWorld().equals(world)&&other.distanceSquared(location)<separation*separation){clear=false;break;}if(clear)return location;}Location fallback=center.getBlock().getLocation().add(0,1,0);while(fallback.getBlockY()<world.getMaxHeight()-2&&(!fallback.getBlock().isPassable()||!fallback.clone().add(0,1,0).getBlock().isPassable()))fallback.add(0,1,0);return fallback;}
    private List<Location> createAutomaticTestHearts(Location base){Vector forward=horizontalDirection(base),right=new Vector(-forward.getZ(),0,forward.getX());List<Location> result=new ArrayList<>();for(int side=-1;side<=1;side++){Location candidate=base.clone().add(forward.clone().multiply(5)).add(right.clone().multiply(side*3));candidate.setX(Math.floor(candidate.getX()));candidate.setZ(Math.floor(candidate.getZ()));candidate.setY(base.getBlockY()+1);while(candidate.getBlockY()<candidate.getWorld().getMaxHeight()-1&&!candidate.getBlock().isEmpty())candidate.add(0,1,0);if(candidate.getBlock().isEmpty())result.add(candidate.getBlock().getLocation());}return result;}
    private void restoreTemporaryTestHearts(){for(Map.Entry<String,Material> entry:new HashMap<>(temporaryTestHeartOriginals).entrySet()){Location location=LocationStore.decode(entry.getKey());if(location!=null)location.getBlock().setType(entry.getValue());}temporaryTestHeartOriginals.clear();}
    private List<Location> selectLootChestLocations(List<Location> hearts,Location playerSpawn){int count=Math.max(1,getConfig().getInt("random-loot-chest-count",5));List<Location> registered=new ArrayList<>(getConfig().getStringList("locations.loot-chests").stream().map(LocationStore::decode).filter(Objects::nonNull).toList());Collections.shuffle(registered);if(!registered.isEmpty())return new ArrayList<>(registered.subList(0,Math.min(count,registered.size())));List<Location> anchors=new ArrayList<>(hearts);Collections.shuffle(anchors);List<Location> result=new ArrayList<>();Random random=new Random();for(Location anchor:anchors){if(result.size()>=count)break;for(int attempt=0;attempt<8;attempt++){double angle=random.nextDouble()*Math.PI*2,distance=3+random.nextInt(4);Location candidate=anchor.clone().add(Math.cos(angle)*distance,0,Math.sin(angle)*distance).getBlock().getLocation();for(int up=0;up<4&&!candidate.getBlock().isEmpty();up++)candidate.add(0,1,0);if(candidate.getBlock().isEmpty()&&candidate.clone().add(0,1,0).getBlock().isEmpty()){result.add(candidate);break;}}}if(result.isEmpty()&&playerSpawn!=null){Location fallback=playerSpawn.clone().add(2,0,0).getBlock().getLocation();while(fallback.getBlockY()<fallback.getWorld().getMaxHeight()-2&&!fallback.getBlock().isEmpty())fallback.add(0,1,0);if(fallback.getBlock().isEmpty())result.add(fallback);}return result;}
    private void restoreLootChests(){for(Map.Entry<String,Material> entry:new HashMap<>(temporaryLootChestOriginals).entrySet()){Location location=LocationStore.decode(entry.getKey());if(location!=null)location.getBlock().setType(entry.getValue());}temporaryLootChestOriginals.clear();lootChestKeys.clear();openedLootChests.clear();personalLootInventories.clear();chestOpeningTasks.clear();}
    private void configureMarker(ArmorStand stand){stand.setCustomName(getConfig().getString("heart-marker-name","鬼の心臓地点"));stand.setCustomNameVisible(false);stand.setVisible(false);stand.setGravity(false);stand.setInvulnerable(true);stand.setMarker(true);stand.setPersistent(true);}

    private void tick(){if(state!=GameState.RUNNING)return;secondsLeft--;checkAzakujiReinforcement();updateOniHeartAwareness();for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null)continue;String warning=chased.contains(id)?"  &7|  &4&l追跡中":"";p.sendActionBar(cc("&c心臓 "+brokenHearts+"/&f"+totalHearts+"  &7|  &e残り "+(secondsLeft/60)+":"+String.format("%02d",secondsLeft%60)+warning));}renderNearbyHeartParticles();if(brokenHearts>=totalHearts)renderExitBeacon();checkExit();if(secondsLeft<=0)end(true,"時間切れ――鬼の勝利");}
    private void updateOniHeartAwareness(){
        if(!getConfig().getBoolean("oni-heart-awareness.enabled",true)||oni==null)return;
        Player p=Bukkit.getPlayer(oni);if(p==null||p.getGameMode()==GameMode.SPECTATOR)return;
        Location nearest=heartHp.keySet().stream().map(LocationStore::decode).filter(Objects::nonNull).filter(l->l.getWorld().equals(p.getWorld())).min(Comparator.comparingDouble(l->l.distanceSquared(p.getLocation()))).orElse(null);
        if(nearest==null){p.setCompassTarget(p.getWorld().getSpawnLocation());return;}
        p.setCompassTarget(nearest.clone().add(.5,.5,.5));
        int count=Math.max(1,getConfig().getInt("oni-heart-awareness.particle-count",18));
        float size=(float)Math.max(.5,getConfig().getDouble("oni-heart-awareness.particle-size",1.8));
        Particle.DustOptions dust=new Particle.DustOptions(Color.fromRGB(210,0,35),size);
        for(String key:heartHp.keySet()){
            Location h=LocationStore.decode(key);if(h==null||!h.getWorld().equals(p.getWorld()))continue;
            Location center=h.clone().add(.5,1.2,.5);
            p.spawnParticle(Particle.REDSTONE,center,count,.35,.55,.35,0,dust);
            p.spawnParticle(Particle.SOUL,center.clone().add(0,.35,0),5,.25,.3,.25,.01);
        }
    }
    private void renderNearbyHeartParticles(){if(!getConfig().getBoolean("heart-proximity-particles.enabled",true))return;double radius=Math.max(2,getConfig().getDouble("heart-proximity-particles.radius",18)),radiusSquared=radius*radius;Particle.DustOptions blood=new Particle.DustOptions(Color.fromRGB(150,0,20),1.35f);for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player viewer=Bukkit.getPlayer(id);if(viewer==null||viewer.getGameMode()==GameMode.SPECTATOR)continue;for(String key:heartHp.keySet()){Location heart=LocationStore.decode(key);if(heart==null||!heart.getWorld().equals(viewer.getWorld())||heart.distanceSquared(viewer.getLocation())>radiusSquared)continue;Location center=heart.clone().add(.5,1.15,.5);viewer.spawnParticle(Particle.REDSTONE,center,12,.32,.45,.32,0,blood);viewer.spawnParticle(Particle.SOUL,center.clone().add(0,.35,0),4,.22,.3,.22,.015);}}}
    private void renderExitBeacon(){if(activeExit==null||!getConfig().getBoolean("exit-beacon.enabled",true))return;World world=activeExit.getWorld();double height=Math.max(16,getConfig().getDouble("exit-beacon.height",80)),step=Math.max(.35,getConfig().getDouble("exit-beacon.particle-step",.5));Location base=activeExit.getBlock().getLocation().add(.5,1.1,.5);Particle.DustOptions cyan=new Particle.DustOptions(Color.fromRGB(40,245,255),Math.max(1f,(float)getConfig().getDouble("exit-beacon.dust-size",2.2)));for(double y=0;y<=height;y+=step){Location point=base.clone().add(0,y,0);world.spawnParticle(Particle.REDSTONE,point,3,.18,.08,.18,0,cyan);if(((int)(y*2))%6==0)world.spawnParticle(Particle.END_ROD,point,2,.12,.2,.12,.008);}for(int ring=0;ring<3;ring++){double radius=1.5+ring*1.2;for(int i=0;i<24;i++){double angle=Math.PI*2*i/24;world.spawnParticle(Particle.SOUL_FIRE_FLAME,base.clone().add(Math.cos(angle)*radius,.2,Math.sin(angle)*radius),1,0,0,0,0);}}world.spawnParticle(Particle.TOTEM,base,45,2.4,.8,2.4,.06);}
    private void updateStamina(){if(state!=GameState.RUNNING)return;long now=System.currentTimeMillis();double baseDrain=getConfig().getDouble("stamina.drain-per-second",2.0)/4.0,baseRecovery=getConfig().getDouble("stamina.recovery-per-second",1.5)/4.0,baseSneakRecovery=getConfig().getDouble("stamina.sneak-recovery-per-second",2.5)/4.0;for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null||p.getGameMode()==GameMode.SPECTATOR||p.getGameMode()==GameMode.CREATIVE)continue;boolean playerSide=players.contains(id);float walkSpeed=isDownEscape(id)?0.55f:(float)getConfig().getDouble("player-walk-speed",0.20);if(playerSide&&!isDownEscape(id)&&hasPassive(id,PassiveSkill.NINJA_BLOOD))walkSpeed=(float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23);else if(playerSide&&!isDownEscape(id)&&hasPassive(id,PassiveSkill.COWARDICE)&&p.isSneaking())walkSpeed=(float)getConfig().getDouble("passive-skills.cowardice-sneak-walk-speed",0.58);p.setWalkSpeed(walkSpeed);if(playerSide&&hasPassive(id,PassiveSkill.LEAP))p.setAllowFlight(now>=passiveLeapReadyAt.getOrDefault(id,0L));else if(p.getAllowFlight()){p.setFlying(false);p.setAllowFlight(false);}double drain=baseDrain,recovery=baseRecovery,sneakRecovery=baseSneakRecovery;if(playerSide&&hasPassive(id,PassiveSkill.LIGHT_FOOTED))drain*=.85;if(playerSide&&hasPassive(id,PassiveSkill.DEEP_BREATH)){recovery*=1.25;sneakRecovery*=1.25;}boolean desperate=playerSide&&updateDesperateState(p,now);double value=stamina.getOrDefault(id,20.0);boolean sprinting=p.isSprinting();if(playerSide&&hasPassive(id,PassiveSkill.LAST_RESERVE)&&wasSprinting.getOrDefault(id,false)&&!sprinting&&value<=getConfig().getDouble("technical-passives.last-reserve-threshold",3.0)&&technicalReady(id,"LAST_RESERVE",getConfig().getLong("technical-passives.last-reserve-cooldown-seconds",25))){value=Math.min(20,value+getConfig().getDouble("technical-passives.last-reserve-recovery",5.0));p.sendActionBar(cc("&e土壇場 &7――スタミナ回復"));}wasSprinting.put(id,sprinting);if(desperate){}else if(p.isSprinting()&&p.getVelocity().setY(0).lengthSquared()>0.005)value-=drain;else value+=p.isSneaking()?sneakRecovery:recovery;value=Math.max(0,Math.min(20,value));stamina.put(id,value);p.setFoodLevel((int)Math.ceil(value));p.setSaturation(0);if(value<=0&&p.isSprinting()){p.setSprinting(false);p.sendActionBar(cc("&cスタミナ切れ &7――歩くか、止まって回復"));}}LivingEntity hunter=getOniEntity();if(hunter!=null&&brokenHearts<totalHearts){double heal=getConfig().getDouble("oni-heart-protection.regeneration-per-second",1.0)/4.0;hunter.setHealth(Math.min(hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),hunter.getHealth()+heal));}}
    private String manualUnstuckDeniedReason(Player p){
        UUID id=p.getUniqueId();long now=System.currentTimeMillis();
        if(getConfig().getBoolean("safety-rescue.deny-while-chased",true)&&chased.contains(id))return "追跡中は使用できません";
        LivingEntity hunter=getOniEntity();
        double radius=Math.max(0,getConfig().getDouble("safety-rescue.deny-near-oni-radius",18.0));
        if(players.contains(id)&&hunter!=null&&hunter.isValid()&&hunter.getWorld()==p.getWorld()&&p.getLocation().distanceSquared(hunter.getLocation())<=radius*radius)return "鬼が近すぎるため使用できません";
        long combat=Math.max(0,getConfig().getLong("safety-rescue.deny-after-combat-seconds",12))*1000L;
        if(now-lastCombatAt.getOrDefault(id,0L)<combat)return "戦闘直後は使用できません";
        if(repairingHeart.containsKey(id)||healingTasks.containsKey(id)||chestOpeningTasks.containsKey(id))return "作業中は使用できません";
        boolean trapped=isEntityEmbedded(p)||p.getLocation().getY()<p.getWorld().getMinHeight()+1||!isSafeStandLocation(p.getLocation());
        if(getConfig().getBoolean("safety-rescue.require-no-escape-space",true))trapped=trapped||!hasEscapeSpace(p.getLocation(),2);
        return trapped?null:"現在地は脱出可能と判定されています";
    }
    private void registerUnstuckAbuseAttempt(Player p,String reason){
        UUID id=p.getUniqueId();long now=System.currentTimeMillis();
        long window=Math.max(10,getConfig().getLong("safety-rescue.abuse-window-seconds",120))*1000L;
        long start=safetyAbuseWindowStartedAt.getOrDefault(id,0L);
        if(start==0||now-start>window){safetyAbuseWindowStartedAt.put(id,now);safetyAbuseAttempts.put(id,0);}
        int attempts=safetyAbuseAttempts.getOrDefault(id,0)+1;safetyAbuseAttempts.put(id,attempts);
        int limit=Math.max(2,getConfig().getInt("safety-rescue.abuse-attempt-limit",3));
        msg(p,"&c詰み救済を拒否しました。 &7"+reason+" &8("+attempts+"/"+limit+")");
        if(attempts<limit)return;
        long lock=Math.max(30,getConfig().getLong("safety-rescue.abuse-lock-seconds",600))*1000L;
        safetyAbuseLockUntil.put(id,now+lock);safetyAbuseAttempts.put(id,0);safetyAbuseWindowStartedAt.put(id,now);
        msg(p,"&4悪用防止: &c詰み救済を一時ロックしました。");
        if(getConfig().getBoolean("safety-rescue.notify-ops-on-lock",true)){
            for(Player op:Bukkit.getOnlinePlayers())if(op.isOp())msg(op,"&e"+p.getName()+" &7の /og unstuck を悪用疑いで一時ロックしました。");
        }
    }

    private void updateSafetyRescue(){
        if(state!=GameState.RUNNING||!getConfig().getBoolean("safety-rescue.enabled",true))return;
        Set<UUID> checked=new HashSet<>();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)){checked.add(id);checkSafetyRescue(p,false);}}
        if(oni!=null){Player oniPlayer=Bukkit.getPlayer(oni);if(oniPlayer!=null){checked.add(oni);checkSafetyRescue(oniPlayer,false);}}
        if(getConfig().getBoolean("safety-rescue.auto-bots",true)){
            for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity e){checked.add(id);checkSafetyRescue(e,true);}}
            if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity e){checked.add(oniBot);checkSafetyRescue(e,true);}
            for(UUID id:new HashSet<>(dakkoCloneBots))if(Bukkit.getEntity(id) instanceof LivingEntity e){checked.add(id);checkSafetyRescue(e,true);}
        }
        safetyLastLocation.keySet().removeIf(id->!checked.contains(id));
        safetyStuckChecks.keySet().removeIf(id->!checked.contains(id));
    }
    private void checkSafetyRescue(LivingEntity entity,boolean bot){
        if(entity==null||!entity.isValid()||entity.isDead())return;
        UUID id=entity.getUniqueId();long now=System.currentTimeMillis();
        Location here=entity.getLocation();
        if(isSafeStandLocation(here)&&hasEscapeSpace(here,2)){
            safetyLastGoodLocation.put(id,here.clone());
        }
        boolean embedded=isEntityEmbedded(entity)||here.getY()<entity.getWorld().getMinHeight()+1;
        if(!bot){
            safetyLastLocation.put(id,here.clone());
            if(embedded&&getConfig().getBoolean("safety-rescue.auto-players-when-embedded",true)&&now>=safetyRescueCooldownUntil.getOrDefault(id,0L))rescueEntityToSafePlace(entity,false);
            return;
        }
        Location last=safetyLastLocation.put(id,here.clone());
        boolean barelyMoved=last!=null&&last.getWorld()==here.getWorld()&&last.distanceSquared(here)<.015;
        int checks=barelyMoved?safetyStuckChecks.getOrDefault(id,0)+1:0;
        safetyStuckChecks.put(id,checks);
        int threshold=Math.max(4,getConfig().getInt("safety-rescue.bot-stuck-checks",10));
        if((embedded||checks>=threshold)&&now>=safetyRescueCooldownUntil.getOrDefault(id,0L)){
            if(rescueEntityToSafePlace(entity,false))safetyStuckChecks.put(id,0);
        }
    }
    private boolean rescueEntityToSafePlace(LivingEntity entity,boolean manual){
        if(entity==null||entity.getWorld()==null)return false;
        UUID id=entity.getUniqueId();Location origin=entity.getLocation().clone();
        Location safe=findNearestSafeRescueLocation(entity,origin);
        if(safe==null){
            Location remembered=safetyLastGoodLocation.get(id);
            if(remembered!=null&&remembered.getWorld()==entity.getWorld()&&isSafeStandLocation(remembered))safe=remembered.clone();
        }
        if(safe==null){
            Location fallback=id.equals(oni)||id.equals(oniBot)?LocationStore.get(getConfig(),"locations.oni-spawn"):activePlayerSpawn;
            if(fallback!=null&&fallback.getWorld()==entity.getWorld())safe=findNearestSafeRescueLocation(entity,fallback);
        }
        if(safe==null)return false;
        safe.setYaw(origin.getYaw());safe.setPitch(origin.getPitch());
        if(entity instanceof Player p){if(players.contains(id))stopRepair(p,"&7詰み救済のため心臓への干渉を中断した。");chestOpeningTasks.remove(id);stopHealing(p,null);}
        if(playerBots.contains(id)){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);stopPlayerBotNavigation(entity);}
        entity.setVelocity(new Vector(0,0,0));entity.setFallDistance(0);entity.teleport(safe);
        entity.getWorld().spawnParticle(Particle.PORTAL,safe.clone().add(0,1,0),28,.45,.8,.45,.12);
        entity.getWorld().playSound(safe,Sound.ENTITY_ENDERMAN_TELEPORT,.55f,1.35f);
        long cd=Math.max(1,getConfig().getLong("safety-rescue.cooldown-seconds",180))*1000L;
        safetyRescueCooldownUntil.put(id,System.currentTimeMillis()+cd);
        safetyLastGoodLocation.put(id,safe.clone());safetyLastLocation.put(id,safe.clone());safetyStuckChecks.put(id,0);
        if(manual&&entity instanceof Player p)p.sendActionBar(cc("&b詰み救済 &7――安全地点へ復帰"));
        return true;
    }
    private Location findNearestSafeRescueLocation(LivingEntity entity,Location center){
        if(center==null||center.getWorld()==null)return null;World world=center.getWorld();
        int radius=Math.max(3,getConfig().getInt("safety-rescue.search-radius",12));
        int vertical=Math.max(2,getConfig().getInt("safety-rescue.vertical-search",6));
        Location best=null;double bestScore=Double.MAX_VALUE;
        int baseX=center.getBlockX(),baseY=center.getBlockY(),baseZ=center.getBlockZ();
        for(int r=1;r<=radius;r++){
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
                for(int dy=vertical;dy>=-vertical;dy--){
                    int y=baseY+dy;if(y<=world.getMinHeight()+1||y>=world.getMaxHeight()-2)continue;
                    Location candidate=new Location(world,baseX+dx+.5,y,baseZ+dz+.5);
                    if(!isSafeStandLocation(candidate)||!hasEscapeSpace(candidate,2))continue;
                    WorldBorder border=world.getWorldBorder();if(border!=null&&!border.isInside(candidate))continue;
                    double score=dx*dx+dz*dz+Math.abs(dy)*1.5;
                    if(score<bestScore){bestScore=score;best=candidate;}
                }
            }
            if(best!=null&&r>=3)break;
        }
        return best;
    }
    private boolean isSafeStandLocation(Location feet){
        if(feet==null||feet.getWorld()==null)return false;
        Block foot=feet.getBlock(),head=feet.clone().add(0,1,0).getBlock(),below=feet.clone().add(0,-1,0).getBlock();
        if(!foot.isPassable()||!head.isPassable()||below.isPassable()||below.isLiquid())return false;
        Material m=below.getType();
        return m!=Material.MAGMA_BLOCK&&m!=Material.CACTUS&&m!=Material.CAMPFIRE&&m!=Material.SOUL_CAMPFIRE&&m!=Material.FIRE&&m!=Material.SOUL_FIRE;
    }
    private boolean hasEscapeSpace(Location feet,int radius){
        if(feet==null)return false;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(dx==0&&dz==0)continue;
            Location next=feet.clone().add(dx,0,dz);
            if(isSafeStandLocation(next))return true;
            if(isSafeStandLocation(next.clone().add(0,1,0)))return true;
            if(isSafeStandLocation(next.clone().add(0,-1,0)))return true;
        }
        return false;
    }
    private boolean isEntityEmbedded(LivingEntity entity){
        Location feet=entity.getLocation(),body=feet.clone().add(0,.9,0),head=feet.clone().add(0,1.7,0);
        return !feet.getBlock().isPassable()||!body.getBlock().isPassable()||!head.getBlock().isPassable();
    }

    private void updateBots(){updateSafetyRescue();updateOniBot();updateDakkoCloneBots();updatePlayerBots();}
    private void updateOniBot(){
        if(state!=GameState.RUNNING||oniBot==null||heavenlyArrival.contains(oniBot))return;if(!(Bukkit.getEntity(oniBot) instanceof Mob bot))return;long now=System.currentTimeMillis();
        LivingEntity target=getConfig().getBoolean("bot.hunter-ai.enabled",true)?findTacticalOniBotTarget(bot):findNearestPlayerSideTarget(bot);
        if(target==null){oniBotTacticalTarget=null;bot.setTarget(null);return;}oniBotTacticalTarget=target.getUniqueId();if(now<botRestUntil){bot.setTarget(null);botStamina=Math.min(20,botStamina+getConfig().getDouble("stamina.sneak-recovery-per-second",2.5)/2.0);return;}
        bot.setTarget(target);double distance=bot.getLocation().distance(target.getLocation());Location predictedTarget=predictOniBotTarget(target);if(distance>2.2)botStamina-=getConfig().getDouble("stamina.drain-per-second",2.0)/2.0;else botStamina=Math.min(20,botStamina+getConfig().getDouble("stamina.recovery-per-second",1.5)/2.0);
        if(playerBots.contains(target.getUniqueId())){guideOniBotToPlayerBot(bot,target,distance);if(distance<=getConfig().getDouble("bot.player-bot-attack-range",2.5)&&now>=botNextMeleeAt){botNextMeleeAt=now+Math.max(1,getConfig().getLong("bot.player-bot-attack-cooldown-ticks",20))*50L;bot.swingMainHand();double melee=getConfig().getDouble("bot.attack-damage",7.0);if(isKishinIronBodyActive(bot))melee*=Math.max(1.0,getConfig().getDouble("kishin-skills.iron-body.attack-damage-multiplier",1.35));target.damage(melee,bot);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,.7f,.8f);}}
        if(botStamina<=0){botStamina=0;botRestUntil=now+(long)(getConfig().getDouble("bot.rest-seconds",3.0)*1000);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_STUNNED,0.6f,1.2f);return;}if(now<botNextAbilityAt)return;
        if(oniType==OniType.DAKKO){
            double foxRadius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0);
            if(oniSkillUnlocked("dakko_clone")&&botDakkoSkillCycle%3==0){spawnDakkoCloneBots(bot,bot.getLocation());botNextAbilityAt=now+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.clone.bot-cooldown-seconds",35));recoverOniSkillMomentum();botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("dakko_fox_fire")&&distance<=foxRadius){useBotFoxFire(bot);botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("dakko_heavenly_arrival")&&distance>=getConfig().getDouble("bot.dakko-heavenly-min-range",8.0)&&distance<=getConfig().getDouble("bot.dakko-heavenly-max-range",24.0)&&botDakkoSkillCycle%2==0){useBotHeavenlyArrival(bot,target);botDakkoSkillCycle++;return;}
            if(brokenHearts<4&&distance>8&&distance<24){Location behind=predictedTarget.clone().subtract(horizontalDirection(target.getLocation()).multiply(3));behind.setY(predictedTarget.getY());bot.teleport(behind);bot.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,bot.getLocation(),30,0.5,1,0.5,0.04);botNextAbilityAt=now+oniBotAbilityCooldownMillis(20+brokenHearts*3);recoverOniSkillMomentum();botDakkoSkillCycle++;}
        }else if(oniType==OniType.KISHIN){
            double roarRadius=getConfig().getDouble("kishin-skills.roar.radius",8.0);
            if(oniSkillUnlocked("kishin_roar")&&distance<=roarRadius&&botDakkoSkillCycle%3==0){useBotKishinRoar(bot);botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("kishin_iron_body")&&botDakkoSkillCycle%4==1&&(distance<=10||bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=getConfig().getDouble("bot.hunter-ai.skill-save-health-ratio",.45))){useBotKishinIronBody(bot);botDakkoSkillCycle++;return;}
            if(distance>5&&distance<16){Vector charge=predictedTarget.toVector().subtract(bot.getLocation().toVector()).normalize().multiply(Math.max(1.2,2.1-brokenHearts*.15)).setY(.15);bot.setVelocity(charge);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);botNextAbilityAt=now+oniBotAbilityCooldownMillis(18+brokenHearts*3);recoverOniSkillMomentum();botDakkoSkillCycle++;}
        }
        else if(oniType==OniType.JAKUTSUKI&&distance>=getConfig().getDouble("jakutsuki-skills.piercing-blast.bot-min-range",7.0)&&distance<=getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)){useBotJakutsukiPiercingBlast(bot,target);return;}
    }

    private LivingEntity findTacticalOniBotTarget(LivingEntity hunter){
        if(azakujiAllyId!=null&&System.currentTimeMillis()<azakujiTauntUntil&&!deadPlayerBots.contains(azakujiAllyId)&&!escapedPlayerBots.contains(azakujiAllyId)&&Bukkit.getEntity(azakujiAllyId) instanceof LivingEntity azakuji&&azakuji.isValid()&&!azakuji.isDead()&&azakuji.getWorld().equals(hunter.getWorld()))return azakuji;
        List<LivingEntity> candidates=new ArrayList<>();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)&&!isDownEscape(id)&&p.getWorld()==hunter.getWorld())candidates.add(p);}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity e&&e.isValid()&&!e.isDead()&&e.getWorld()==hunter.getWorld())candidates.add(e);}
        if(candidates.isEmpty())return null;
        LivingEntity best=null;double bestScore=-Double.MAX_VALUE;
        for(LivingEntity target:candidates){
            UUID id=target.getUniqueId();double distance=hunter.getLocation().distance(target.getLocation());
            double score=-distance*Math.max(.1,getConfig().getDouble("bot.hunter-ai.target-distance-weight",1.0));
            if(target instanceof Player)score+=getConfig().getDouble("bot.hunter-ai.target-human-bonus",2.5);
            if(id.equals(configuredFriendlyNpcId))score+=getConfig().getDouble("bot.hunter-ai.target-friendly-npc-bonus",5.0);
            double max=target.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20:target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double ratio=target.getHealth()/Math.max(1,max);score+=(1-ratio)*getConfig().getDouble("bot.hunter-ai.target-injured-bonus",7.0);
            if(repairingHeart.containsKey(id)||playerBotWorkingHeart.containsKey(id))score+=getConfig().getDouble("bot.hunter-ai.target-heart-worker-bonus",8.0);
            if(isNearRemainingHeart(target.getLocation(),getConfig().getDouble("bot.hunter-ai.heart-defense-radius",16.0)))score+=getConfig().getDouble("bot.hunter-ai.heart-defense-bonus",6.0);
            if(brokenHearts>=totalHearts)score+=getConfig().getDouble("bot.hunter-ai.final-target-bonus",5.0)*(1-ratio);
            if(id.equals(oniBotTacticalTarget))score+=getConfig().getDouble("bot.hunter-ai.retarget-hysteresis",3.0);
            if(score>bestScore){bestScore=score;best=target;}
        }
        return best;
    }
    private boolean isNearRemainingHeart(Location at,double radius){
        if(at==null)return false;double r2=radius*radius;
        for(String key:heartHp.keySet()){Location h=LocationStore.decode(key);if(h!=null&&h.getWorld()==at.getWorld()&&h.distanceSquared(at)<=r2)return true;}
        return false;
    }
    private Location predictOniBotTarget(LivingEntity target){
        Location base=target.getLocation().clone();Vector v=target.getVelocity().clone().setY(0);
        double seconds=Math.max(0,getConfig().getDouble("bot.hunter-ai.prediction-seconds",.65));
        Vector lead=v.multiply(20.0*seconds);double max=Math.max(0,getConfig().getDouble("bot.hunter-ai.prediction-max-distance",3.5));
        if(lead.length()>max&&lead.lengthSquared()>0)lead.normalize().multiply(max);
        return base.add(lead);
    }
    private LivingEntity findNearestPlayerSideTarget(LivingEntity hunter){if(azakujiAllyId!=null&&System.currentTimeMillis()<azakujiTauntUntil&&!deadPlayerBots.contains(azakujiAllyId)&&!escapedPlayerBots.contains(azakujiAllyId)&&Bukkit.getEntity(azakujiAllyId) instanceof LivingEntity azakuji&&azakuji.isValid()&&!azakuji.isDead()&&azakuji.getWorld().equals(hunter.getWorld()))return azakuji;List<LivingEntity> targets=new ArrayList<>();for(UUID id:players){Player player=Bukkit.getPlayer(id);if(player!=null&&!dead.contains(id)&&!escaped.contains(id)&&!isDownEscape(id)&&player.getWorld().equals(hunter.getWorld()))targets.add(player);}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity entity&&entity.isValid()&&!entity.isDead()&&entity.getWorld().equals(hunter.getWorld()))targets.add(entity);}return targets.stream().min(Comparator.comparingDouble(entity->entity.getLocation().distanceSquared(hunter.getLocation()))).orElse(null);}
    private void guideOniBotToPlayerBot(Mob bot,LivingEntity target,double distance){if(distance<=getConfig().getDouble("bot.player-bot-attack-range",2.5))return;Vector direction=target.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);if(direction.lengthSquared()<.01)return;direction.normalize();double speed=getConfig().getDouble("bot.player-bot-chase-velocity",.28);double y=bot.getVelocity().getY();if(bot.isOnGround()&&isPlayerBotBlocked(bot,direction,1.0))y=getConfig().getDouble("player-bot.movement.jump-velocity",.42);bot.setVelocity(new Vector(direction.getX()*speed,y,direction.getZ()*speed));float yaw=(float)Math.toDegrees(Math.atan2(-direction.getX(),direction.getZ()));bot.setRotation(yaw,bot.getLocation().getPitch());}

    private void spawnDakkoCloneBots(LivingEntity owner,Location origin){
        if(owner==null||origin==null||origin.getWorld()==null)return;
        int max=Math.max(1,getConfig().getInt("dakko-skills.clone.max-active",2));
        removeExpiredDakkoCloneBots();
        int room=Math.max(0,max-dakkoCloneBots.size());
        int count=Math.min(room,Math.max(1,getConfig().getInt("dakko-skills.clone.spawn-count",2)));
        if(count<=0)return;
        double baseHealth=owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?60.0:owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double hpRatio=Math.max(.05,Math.min(.49,getConfig().getDouble("dakko-skills.clone.health-ratio",.40)));
        double attackRatio=Math.max(.05,Math.min(.49,getConfig().getDouble("dakko-skills.clone.attack-ratio",.40)));
        double moveRatio=Math.max(.10,Math.min(1.0,getConfig().getDouble("dakko-skills.clone.movement-ratio",.85)));
        double baseAttack=getConfig().getDouble("bot.attack-damage",7.0);
        if(owner.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!=null)baseAttack=Math.max(1.0,owner.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue());
        double baseMove=getConfig().getDouble("bot.movement-speed",.32);
        if(owner.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)baseMove=Math.max(.05,owner.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue());
        long expires=System.currentTimeMillis()+Math.max(1,getConfig().getInt("dakko-skills.clone.duration-seconds",5))*1000L;
        Vector right=new Vector(-horizontalDirection(owner.getLocation()).getZ(),0,horizontalDirection(owner.getLocation()).getX());
        for(int n=0;n<count;n++){
            Location at=origin.clone().add(right.clone().multiply(n==0?-1.2:1.2));
            Zombie clone=at.getWorld().spawn(at,Zombie.class);
            clone.setBaby(false);clone.setPersistent(true);clone.setRemoveWhenFarAway(false);clone.setCanPickupItems(false);clone.setSilent(true);
            clone.setCustomName(cc("&5&l堕狐・分身体"));clone.setCustomNameVisible(false);
            clone.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
            double hp=Math.max(4.0,baseHealth*hpRatio);
            clone.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);clone.setHealth(hp);
            clone.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(Math.max(1.0,baseAttack*attackRatio));
            clone.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(Math.max(.05,baseMove*moveRatio));
            clone.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));clone.getEquipment().setItemInMainHandDropChance(0);
            clone.getEquipment().setHelmet(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"));
            clone.getEquipment().setChestplate(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"));
            clone.getEquipment().setLeggings(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"));
            clone.getEquipment().setBoots(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"));
            clone.getEquipment().setHelmetDropChance(0);clone.getEquipment().setChestplateDropChance(0);clone.getEquipment().setLeggingsDropChance(0);clone.getEquipment().setBootsDropChance(0);
            clone.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"dakko_clone_bot");
            dakkoCloneBots.add(clone.getUniqueId());dakkoCloneExpiresAt.put(clone.getUniqueId(),expires);dakkoCloneNextAttackAt.put(clone.getUniqueId(),0L);
            clone.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,clone.getLocation().add(0,1,0),40,.55,.9,.55,.05);
            clone.getWorld().spawnParticle(Particle.SMOKE_LARGE,clone.getLocation().add(0,1,0),24,.45,.75,.45,.03);
        }
        origin.getWorld().playSound(origin,Sound.ENTITY_FOX_SCREECH,.8f,.72f);
    }
    private void updateDakkoCloneBots(){
        if(dakkoCloneBots.isEmpty())return;
        long now=System.currentTimeMillis();
        for(UUID id:new HashSet<>(dakkoCloneBots)){
            Entity raw=Bukkit.getEntity(id);
            if(!(raw instanceof Mob clone)||!clone.isValid()||clone.isDead()||now>=dakkoCloneExpiresAt.getOrDefault(id,0L)){
                dismissDakkoCloneBot(id,true);continue;
            }
            LivingEntity target=findNearestPlayerSideTarget(clone);
            if(target==null){clone.setTarget(null);continue;}
            clone.setTarget(target);
            double dist=clone.getLocation().distance(target.getLocation());
            if(dist>2.5){
                Vector d=target.getLocation().toVector().subtract(clone.getLocation().toVector()).setY(0);
                if(d.lengthSquared()>.01){
                    d.normalize();double v=getConfig().getDouble("bot.player-bot-chase-velocity",.28)*getConfig().getDouble("dakko-skills.clone.movement-ratio",.85);
                    double y=clone.getVelocity().getY();if(clone.isOnGround()&&isPlayerBotBlocked(clone,d,1.0))y=getConfig().getDouble("player-bot.movement.jump-velocity",.42);
                    clone.setVelocity(new Vector(d.getX()*v,y,d.getZ()*v));
                }
            }else if(now>=dakkoCloneNextAttackAt.getOrDefault(id,0L)){
                dakkoCloneNextAttackAt.put(id,now+Math.max(8,getConfig().getLong("dakko-skills.clone.attack-cooldown-ticks",22))*50L);
                clone.swingMainHand();
                double damage=Math.max(1.0,getConfig().getDouble("bot.attack-damage",7.0)*Math.min(.49,getConfig().getDouble("dakko-skills.clone.attack-ratio",.40)));
                target.damage(damage,clone);
                clone.getWorld().playSound(clone.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.55f,1.15f);
            }
        }
    }
    private void removeExpiredDakkoCloneBots(){
        long now=System.currentTimeMillis();
        for(UUID id:new HashSet<>(dakkoCloneBots))if(now>=dakkoCloneExpiresAt.getOrDefault(id,0L)||Bukkit.getEntity(id)==null)dismissDakkoCloneBot(id,false);
    }
    private void dismissDakkoCloneBot(UUID id,boolean effect){
        Entity e=Bukkit.getEntity(id);
        if(e!=null&&effect&&e.getWorld()!=null){e.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,e.getLocation().add(0,1,0),30,.45,.8,.45,.04);e.getWorld().spawnParticle(Particle.SMOKE_LARGE,e.getLocation().add(0,1,0),22,.4,.65,.4,.03);e.getWorld().playSound(e.getLocation(),Sound.ENTITY_FOX_TELEPORT,.55f,.8f);}
        if(e!=null)e.remove();
        dakkoCloneBots.remove(id);dakkoCloneExpiresAt.remove(id);dakkoCloneNextAttackAt.remove(id);
    }
    private void removeDakkoCloneBots(){for(UUID id:new HashSet<>(dakkoCloneBots))dismissDakkoCloneBot(id,false);}
    private void useBotKishinRoar(Mob bot){
        double radius=getConfig().getDouble("kishin-skills.roar.radius",8.0),damage=getConfig().getDouble("kishin-skills.roar.fixed-damage",4.0),knock=getConfig().getDouble("kishin-skills.roar.knockback",.65);int slowTicks=getConfig().getInt("kishin-skills.roar.slowness-ticks",60),weakTicks=getConfig().getInt("kishin-skills.roar.weakness-ticks",80);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1.35f,.55f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,bot.getLocation().add(0,1,0),8,1.4,.8,1.4,.05);bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation().add(0,1,0),90,radius*.55,.8,radius*.55,.08);
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;dealFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(bot,q,knock,.32);q.sendActionBar(cc("&4鬼吼 &7――身体が竦んだ！"));}
        for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity q=(LivingEntity)Bukkit.getEntity(id);dealFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(bot,q,knock,.32);}
        botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("kishin-skills.roar.cooldown-seconds",28));recoverOniSkillMomentum();
    }
    private void useBotKishinIronBody(Mob bot){int ticks=Math.max(20,getConfig().getInt("kishin-skills.iron-body.duration-ticks",120));bot.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,ticks,0,false,true));bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,true));bot.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,ticks,1,false,true));bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1,0),60,.65,1,.65,.08);bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_SHIELD_BLOCK,1.1f,.65f);botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("kishin-skills.iron-body.cooldown-seconds",35));recoverOniSkillMomentum();}

    private boolean isKishinIronBodyActive(LivingEntity entity){
        if(oniType!=OniType.KISHIN||entity==null)return false;
        PotionEffect effect=entity.getPotionEffect(PotionEffectType.INCREASE_DAMAGE);
        return effect!=null&&effect.getAmplifier()>=1;
    }
    private void useBotFoxFire(Mob bot){double radius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0),damage=getConfig().getDouble("dakko-skills.fox-fire.fixed-damage",5.0);int fireTicks=getConfig().getInt("dakko-skills.fox-fire.fire-ticks",100);bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_FIRECHARGE_USE,1.2f,.65f);bot.getWorld().spawnParticle(Particle.FLAME,bot.getLocation().add(0,1,0),100,radius*.55,1.2,radius*.55,.08);bot.getWorld().spawnParticle(Particle.LAVA,bot.getLocation(),25,radius*.5,.7,radius*.5,.03);for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;q.setFireTicks(Math.max(q.getFireTicks(),fireTicks));dealFixedDamage(q,damage);q.sendActionBar(cc("&6狐火に焼かれている！"));}for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity target=(LivingEntity)Bukkit.getEntity(id);target.setFireTicks(Math.max(target.getFireTicks(),fireTicks));dealFixedDamage(target,damage);}botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.fox-fire.cooldown-seconds",30));recoverOniSkillMomentum();}

    private void useBotHeavenlyArrival(Mob bot,LivingEntity target){Location landing=target.getLocation().clone();double height=getConfig().getDouble("dakko-skills.heavenly-arrival.launch-height",10.0);Location sky=landing.clone().add(0,height,0);sky.setY(Math.min(sky.getY(),bot.getWorld().getMaxHeight()-2));heavenlyArrival.add(bot.getUniqueId());bot.setTarget(null);bot.teleport(sky);bot.setFallDistance(0);bot.getWorld().playSound(sky,Sound.ENTITY_PHANTOM_FLAP,1.2f,.55f);int windup=Math.max(4,getConfig().getInt("bot.dakko-heavenly-windup-ticks",12));Bukkit.getScheduler().runTaskLater(this,()->{if(state!=GameState.RUNNING||!bot.isValid()){heavenlyArrival.remove(bot.getUniqueId());return;}Location arrival=isActivePlayerSideTarget(target)?target.getLocation().clone():landing;bot.teleport(arrival);bot.setFallDistance(0);heavenlyArrival.remove(bot.getUniqueId());impactBotHeavenlyArrival(bot);},windup);botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.heavenly-arrival.cooldown-seconds",35));recoverOniSkillMomentum();}

    private void impactBotHeavenlyArrival(Mob bot){double radius=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-radius",5.0),damage=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-damage",7.0);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.2f,.65f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,bot.getLocation(),5,.8,.4,.8,.05);bot.getWorld().spawnParticle(Particle.FLAME,bot.getLocation(),70,radius*.45,.8,radius*.45,.06);for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;dealFixedDamage(q,damage);pushFrom(bot,q);}for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity target=(LivingEntity)Bukkit.getEntity(id);dealFixedDamage(target,damage);pushFrom(bot,target);}}
    private boolean isActivePlayerSideTarget(LivingEntity target){if(target==null||!target.isValid()||target.isDead())return false;UUID id=target.getUniqueId();return players.contains(id)&&!dead.contains(id)&&!escaped.contains(id)||playerBots.contains(id)&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&!isDownEscape(id);}
    private boolean validPlayerBotSkillTarget(LivingEntity hunter,UUID id,double radius){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id)||!(Bukkit.getEntity(id) instanceof LivingEntity target))return false;return target.isValid()&&!target.isDead()&&target.getWorld().equals(hunter.getWorld())&&target.getLocation().distanceSquared(hunter.getLocation())<=radius*radius;}
    private void pushFrom(LivingEntity source,LivingEntity target){Vector push=target.getLocation().toVector().subtract(source.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);target.setVelocity(push.normalize().multiply(.85).setY(.45));}
    private void updatePlayerBots(){
        if(state!=GameState.RUNNING||playerBots.isEmpty())return;LivingEntity hunter=getOniEntity();
        for(UUID id:new HashSet<>(playerBots)){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){deadPlayerBots.add(id);continue;}
            if(bot instanceof Mob mob)mob.setTarget(null);
            if(id.equals(azakujiAllyId)){updateAzakujiAlly(bot,hunter);continue;}
            if(isDownEscape(id)){
                clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                if(hunter!=null&&hunter.getWorld().equals(bot.getWorld())){
                    Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                    if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                    movePlayerBot(bot,bot.getLocation().clone().add(away.normalize().multiply(12)),1.15);
                }
                continue;
            }
            if((id.equals(configuredFriendlyNpcId)||eliteBotMatchIds.contains(id))&&getConfig().getBoolean("friendly-npc.elite-ai.enabled",true)){updateEliteFriendlyNpc(bot,hunter);continue;}
            Location destination=null;boolean fleeing=false,hunting=false;
            if(brokenHearts>=totalHearts){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);hunting=shouldPlayerBotHuntOni(bot,hunter);if(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot)&&hunter!=null)hunting=true;destination=hunting&&hunter!=null?hunter.getLocation():activeExit;}
            else if(hunter!=null&&hunter.getWorld().equals(bot.getWorld())&&hunter.getLocation().distanceSquared(bot.getLocation())<Math.pow(getConfig().getDouble("player-bot.flee-radius",10.0),2)){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<0.01)away=new Vector(1,0,0);double side=((id.hashCode()&1)==0?1:-1)*getConfig().getDouble("player-bot.movement.flee-strafe",3.0);Vector lateral=new Vector(-away.getZ(),0,away.getX()).normalize().multiply(side);destination=bot.getLocation().clone().add(away.normalize().multiply(8)).add(lateral);fleeing=true;}
            else destination=selectPlayerBotHeart(id,bot);
            if(destination==null)continue;double distance=bot.getLocation().distance(destination);
            if(hunting&&oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot)){LivingEntity snake=findNearestJakutsukiSnake(bot,getConfig().getDouble("jakutsuki-final.bot-snake-clear-radius",7.0));if(snake!=null&&(hunter==null||bot.getLocation().distanceSquared(hunter.getLocation())>9.0)){double snakeDistance=bot.getLocation().distance(snake.getLocation());if(snakeDistance<=2.6){stopPlayerBotNavigation(bot);bot.swingMainHand();jakutsukiSnakes.remove(snake.getUniqueId());snake.getWorld().spawnParticle(Particle.SWEEP_ATTACK,snake.getLocation().add(0,.5,0),3,.2,.2,.2,0);snake.getWorld().playSound(snake.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.8f,1.25f);snake.remove();continue;}movePlayerBot(bot,snake.getLocation(),getConfig().getDouble("jakutsuki-final.bot-hunt-step",0.92));continue;}}
            usePlayerBotSkill(id,bot,hunter,fleeing,hunting);
            if(!fleeing&&brokenHearts<totalHearts&&distance<=2.6){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);advanceHeartBot(id,bot,destination.getBlock());continue;}if(!fleeing)clearPlayerBotWork(id);
            if(hunting&&hunter!=null&&distance<=getConfig().getDouble("player-bot.hunt-attack-range",2.4)){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);attackOniWithPlayerBot(id,bot,hunter);continue;}
            if(!hunting&&brokenHearts>=totalHearts&&System.currentTimeMillis()>=exitSealedUntil&&distance<=getConfig().getDouble("exit-radius",3.0)){escapedPlayerBots.add(id);bot.remove();all("&bぷれいやーBot &fが脱出しました！");checkPlayerOutcome();continue;}
            double step=fleeing?getConfig().getDouble("player-bot.flee-step",0.85):hunting?getConfig().getDouble("player-bot.hunt-step",0.78):getConfig().getDouble("player-bot.move-step",0.65);if(hunting&&oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))step=Math.max(step,getConfig().getDouble("jakutsuki-final.bot-hunt-step",0.92));boolean skillSprint=System.currentTimeMillis()<playerBotSprintUntil.getOrDefault(id,0L);if(skillSprint)step*=getConfig().getDouble("player-bot.skills.sprint-step-multiplier",1.45);if(fleeing&&hasPlayerBotPassive(id,PassiveSkill.COWARDICE))step*=getConfig().getDouble("player-bot.passives.cowardice-flee-step-multiplier",1.08);if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))step*=1.05;step*=updatePlayerBotStamina(id,fleeing||hunting||skillSprint);movePlayerBot(bot,destination,step);
        }
    }
    private void updateEliteFriendlyNpc(LivingEntity bot,LivingEntity hunter){
        UUID id=bot.getUniqueId();if(isDownEscape(id))return;
        String preset=parseFriendlyPreset(playerBotPresetNames.getOrDefault(id,""));
        if(preset==null)preset=parseFriendlyPreset(getConfig().getString("friendly-npc.preset",""));
        if(preset==null)return;
        long now=System.currentTimeMillis();
        double baseStep=getConfig().getDouble("player-bot.move-step",.65)*getConfig().getDouble("friendly-npc.elite-ai.movement-multiplier",1.12);
        double chaseStep=getConfig().getDouble("player-bot.flee-step",.85)*getConfig().getDouble("friendly-npc.elite-ai.chase-movement-multiplier",1.18);
        double danger=getConfig().getDouble("friendly-npc.elite-ai.danger-radius",12.0);
        double oniDistance=hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()?bot.getLocation().distance(hunter.getLocation()):Double.MAX_VALUE;
        Player threatened=findMostThreatenedHuman(hunter);
        LivingEntity injured=findMostInjuredAlly(bot);
        boolean finalPhaseNow=brokenHearts>=totalHearts;

        if(finalPhaseNow){
            clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
            boolean canFight=hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()
                    &&("IGAMI_KYOYA".equals(preset)||"AZANAMI_REN".equals(preset)||"KAGAYA_RION".equals(preset));
            if(canFight&&bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())>.55){
                usePlayerBotSkill(id,bot,hunter,false,true);
                double d=bot.getLocation().distance(hunter.getLocation());
                if(d<=getConfig().getDouble("player-bot.hunt-attack-range",2.4))attackOniWithPlayerBot(id,bot,hunter);
                else movePlayerBot(bot,hunter.getLocation(),baseStep*1.08);
            }else if(activeExit!=null)movePlayerBot(bot,activeExit,baseStep*1.10);
            return;
        }

        switch(preset){
            case "IGAMI_KYOYA" -> {
                if(hunter!=null&&threatened!=null&&hunter.getWorld()==bot.getWorld()
                        &&hunter.getLocation().distanceSquared(threatened.getLocation())<=Math.pow(getConfig().getDouble("friendly-npc.elite-ai.intercept-radius",10.0),2)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    usePlayerBotSkill(id,bot,hunter,false,true);
                    Vector away=threatened.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                    if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                    Location intercept=threatened.getLocation().clone().subtract(away.normalize().multiply(2.2));
                    movePlayerBot(bot,intercept,chaseStep);
                    return;
                }
                if(oniDistance<danger){usePlayerBotSkill(id,bot,hunter,true,false);Location flee=eliteFleeDestination(bot,hunter,9);movePlayerBot(bot,flee,chaseStep);return;}
            }
            case "AZANAMI_MISAKI" -> {
                if(oniDistance<Math.max(danger,15.0)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);usePlayerBotSkill(id,bot,hunter,true,false);
                    movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,14),chaseStep*1.08);return;
                }
            }
            case "ARIKAWA_FUUKA" -> {
                if(oniDistance<danger+2){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);usePlayerBotSkill(id,bot,hunter,true,false);movePlayerBot(bot,eliteFleeDestination(bot,hunter,12),chaseStep);return;}
            }
            case "AZANAMI_REN" -> {
                if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&(oniDistance<18||threatened!=null)){
                    usePlayerBotSkill(id,bot,hunter,oniDistance<10,true);
                    if(oniDistance<8){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,11),chaseStep);return;}
                }
            }
            case "MEDIC" -> {
                double supportRatio=getConfig().getDouble("friendly-npc.elite-ai.support-health-ratio",.70);
                if(injured!=null&&injured.getHealth()/Math.max(1,injured.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=supportRatio){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&injured.getLocation().distanceSquared(hunter.getLocation())<danger*danger){
                        movePlayerBot(bot,eliteFleeDestination(bot,hunter,10),chaseStep);return;
                    }
                    double d=bot.getLocation().distance(injured.getLocation());
                    if(d>getConfig().getDouble("friendly-npc.elite-ai.medic-follow-distance",4.5))movePlayerBot(bot,injured.getLocation(),baseStep*1.08);
                    else{stopPlayerBotNavigation(bot);usePlayerBotSkill(id,bot,hunter,false,false);eliteHealTarget(bot,injured);}
                    return;
                }
                if(oniDistance<danger){movePlayerBot(bot,eliteFleeDestination(bot,hunter,10),chaseStep);return;}
            }
            case "AKASAKA_HIIRO" -> {
                if(oniDistance<danger+3){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,30,2,false,true));if(bot.isOnGround())bot.setVelocity(bot.getVelocity().setY(.55));movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,14),chaseStep*1.12);return;}
            }
            case "KAGAYA_RION" -> {
                if(oniDistance<getConfig().getDouble("friendly-npc.elite-ai.rion-blink-trigger-radius",10.0)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(now>=playerBotSkillReadyAt.getOrDefault(id,0L)&&eliteRionBlink(id,bot,hunter)){return;}
                    movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,12),chaseStep*1.10);return;
                }
            }
            case "AMANAI_IONA" -> {
                if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&(oniDistance<16||(threatened!=null&&hunter.getLocation().distanceSquared(threatened.getLocation())<144))){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(now>=playerBotSkillReadyAt.getOrDefault(id,0L))eliteIonaDecoy(id,bot,hunter,threatened);
                    if(oniDistance<danger)movePlayerBot(bot,eliteFleeDestination(bot,hunter,11),chaseStep);
                    else if(threatened!=null)movePlayerBot(bot,threatened.getLocation(),baseStep);
                    return;
                }
            }
        }

        Location heart=selectEliteFriendlyHeart(id,bot,hunter);
        if(heart==null)return;
        double d=bot.getLocation().distance(heart);
        if(d<=2.6){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);advanceHeartBot(id,bot,heart.getBlock());}
        else{clearPlayerBotWork(id);double step=baseStep*updatePlayerBotStamina(id,true);movePlayerBot(bot,heart,step);}
    }

    private Player findMostThreatenedHuman(LivingEntity hunter){
        if(hunter==null)return null;Player best=null;double bestScore=Double.MAX_VALUE;
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=hunter.getWorld())continue;double d=p.getLocation().distanceSquared(hunter.getLocation());if(d<bestScore){bestScore=d;best=p;}}
        return best;
    }
    private LivingEntity findMostInjuredAlly(LivingEntity from){
        LivingEntity best=null;double bestRatio=1.01;
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=from.getWorld())continue;double ratio=p.getHealth()/Math.max(1,p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(ratio<bestRatio){bestRatio=ratio;best=p;}}
        for(UUID id:playerBots){if(id.equals(from.getUniqueId())||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity e)||e.getWorld()!=from.getWorld())continue;double ratio=e.getHealth()/Math.max(1,e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(ratio<bestRatio){bestRatio=ratio;best=e;}}
        return best;
    }
    private Location selectEliteFriendlyHeart(UUID id,LivingEntity bot,LivingEntity hunter){
        Set<String> choices=new HashSet<>(heartHp.keySet());choices.addAll(fakeHeartKeys);String best=null;double bestScore=Double.MAX_VALUE;
        for(String key:choices){
            Location l=LocationStore.decode(key);if(l==null||l.getWorld()!=bot.getWorld())continue;
            double score=l.distanceSquared(bot.getLocation());
            long bots=playerBotTargetHeart.entrySet().stream().filter(e->!e.getKey().equals(id)&&key.equals(e.getValue())).count();
            score+=bots*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-bot-penalty",75.0);
            int humans=0;for(UUID pid:players){Player p=Bukkit.getPlayer(pid);if(p!=null&&!dead.contains(pid)&&!escaped.contains(pid)&&p.getWorld()==l.getWorld()&&p.getLocation().distanceSquared(l)<36)humans++;}
            score+=humans*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-player-penalty",110.0);
            if(hunter!=null&&hunter.getWorld()==l.getWorld()){double oni=Math.min(1600,l.distanceSquared(hunter.getLocation()));score-=oni*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-oni-distance-weight",1.8);}
            if(score<bestScore){bestScore=score;best=key;}
        }
        if(best==null)return null;playerBotTargetHeart.put(id,best);return LocationStore.decode(best);
    }
    private Location eliteFleeDestination(LivingEntity bot,LivingEntity hunter,double distance){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return bot.getLocation();
        Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);
        return bot.getLocation().clone().add(away.normalize().multiply(distance));
    }
    private Location eliteFleeDestinationAwayFromTeam(LivingEntity bot,LivingEntity hunter,double distance){
        Location base=eliteFleeDestination(bot,hunter,distance);Vector push=new Vector();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p==null||dead.contains(id)||escaped.contains(id)||p.getWorld()!=bot.getWorld())continue;Vector away=bot.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(away.lengthSquared()>0&&away.lengthSquared()<100)push.add(away.normalize());}
        if(push.lengthSquared()>0)base.add(push.normalize().multiply(4));return base;
    }
    private boolean eliteRionBlink(UUID id,LivingEntity bot,LivingEntity hunter){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return false;
        Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);away.normalize();
        double distance=getConfig().getDouble("friendly-npc.elite-ai.rion-blink-distance",4.0);
        Location dest=bot.getLocation().clone().add(away.multiply(distance));
        for(int y=1;y>=-1;y--){Location test=dest.clone().add(0,y,0);if(test.getBlock().isPassable()&&test.clone().add(0,1,0).getBlock().isPassable()&&!test.clone().add(0,-1,0).getBlock().isPassable()){bot.teleport(test);bot.getWorld().spawnParticle(Particle.PORTAL,test,32,.4,.8,.4,.15);bot.getWorld().playSound(test,Sound.ENTITY_ENDERMAN_TELEPORT,.75f,1.25f);playerBotSkillReadyAt.put(id,System.currentTimeMillis()+PlayerSkill.BLINK.cooldown*1000L);return true;}}
        return false;
    }
    private void eliteIonaDecoy(UUID id,LivingEntity bot,LivingEntity hunter,Player threatened){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return;
        Vector fromHunter=(threatened!=null?threatened.getLocation():bot.getLocation()).toVector().subtract(hunter.getLocation().toVector()).setY(0);if(fromHunter.lengthSquared()<.01)fromHunter=new Vector(1,0,0);
        Vector side=new Vector(-fromHunter.getZ(),0,fromHunter.getX()).normalize().multiply(((id.hashCode()&1)==0?1:-1)*getConfig().getDouble("friendly-npc.elite-ai.iona-decoy-distance",10.0));
        Location at=hunter.getLocation().clone().add(side);Item decoy=bot.getWorld().dropItem(at,new ItemStack(Material.SNOWBALL));decoy.setPickupDelay(Integer.MAX_VALUE);decoy.setVelocity(new Vector(0,.18,0));
        fakeNoiseUntil.put(decoy.getUniqueId(),System.currentTimeMillis()+Math.max(1,getConfig().getLong("friendly-npc.elite-ai.iona-decoy-duration-seconds",8))*1000L);
        bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),28,.5,.8,.5,.03);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_SNOWBALL_THROW,.75f,.7f);
        playerBotSkillReadyAt.put(id,System.currentTimeMillis()+Math.max(1,getConfig().getLong("friendly-npc.elite-ai.iona-decoy-cooldown-seconds",18))*1000L);
    }
    private void eliteHealTarget(LivingEntity healer,LivingEntity target){
        if(target==null||target.isDead()||!target.isValid()||target.getWorld()!=healer.getWorld())return;
        double max=target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(target.getHealth()>=max)return;
        double amount=getConfig().getDouble("player-bot.skills.heal-amount",8.0);target.setHealth(Math.min(max,target.getHealth()+amount));
        target.getWorld().spawnParticle(Particle.HEART,target.getLocation().add(0,1,0),8,.4,.5,.4,.02);target.getWorld().playSound(target.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.5f,1.45f);
    }

    private void updateAzakujiAlly(LivingEntity bot,LivingEntity hunter){
        UUID id=bot.getUniqueId();long now=System.currentTimeMillis();
        if(hunter==null||!hunter.isValid()||hunter.isDead()||hunter.getWorld()!=bot.getWorld()){
            Location ally=nearestActiveHumanLocation(bot);
            if(ally!=null&&bot.getLocation().distanceSquared(ally)>Math.pow(getConfig().getDouble("azakuji-ally.guard-ai.follow-distance",4.5),2))movePlayerBot(bot,ally,getConfig().getDouble("player-bot.move-step",.65)*1.08);
            return;
        }

        double max=Math.max(1.0,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        double hpRatio=bot.getHealth()/max;
        Player threatened=findThreatenedHumanForAzakuji(hunter);
        double nearestHumanThreat=threatened==null?Double.MAX_VALUE:threatened.getLocation().distance(hunter.getLocation());
        double combatDetect=getConfig().getDouble("azakuji-ally.guard-ai.protect-detection-radius",16.0);
        boolean combatNearby=nearestHumanThreat<=combatDetect||bot.getLocation().distance(hunter.getLocation())<=combatDetect||now<azakujiFightUntil||now<azakujiEscapeUntil;
        if(combatNearby)azakujiLastCombatAt=now;
        long regenDelay=(long)(getConfig().getDouble("azakuji-ally.guard-ai.out-of-combat-regen-delay-seconds",3.0)*1000L);
        if(!combatNearby&&now-azakujiLastCombatAt>=regenDelay&&bot.getHealth()<max){
            double regen=Math.max(0.0,getConfig().getDouble("azakuji-ally.guard-ai.out-of-combat-regen-per-second",8.0));
            bot.setHealth(Math.min(max,bot.getHealth()+regen*0.5));
            bot.getWorld().spawnParticle(Particle.HEART,bot.getLocation().add(0,1.25,0),2,.22,.28,.22,.01);
        }
        double oniDistance=bot.getLocation().distance(hunter.getLocation());
        double detection=getConfig().getDouble("azakuji-ally.guard-ai.protect-detection-radius",16.0);

        // Only at critical health: make a short evasive leap, then immediately return to combat.
        if(hpRatio<=getConfig().getDouble("azakuji-ally.guard-ai.low-health-ratio",0.15)&&azakujiEscapeUntil<now){
            azakujiEscapeUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.escape-seconds",2.0)*1000L);
            azakujiTauntUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L);
        }
        if(now<azakujiEscapeUntil){
            clearPlayerBotWork(id);
            playerBotTargetHeart.remove(id);
            makeAzakujiTaunt(bot,now);
            Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
            if(away.lengthSquared()<.01)away=new Vector(1,0,0);
            Vector side=new Vector(-away.getZ(),0,away.getX()).normalize().multiply(((now/1000L)&1L)==0L?3.5:-3.5);
            Location escape=bot.getLocation().clone().add(away.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.escape-distance",10.0))).add(side);
            azakujiLeapEscape(bot,hunter,escape,now);
            movePlayerBot(bot,escape,getConfig().getDouble("player-bot.flee-step",.85)*1.18);
            return;
        }

        boolean protecting=threatened!=null&&threatened.getWorld()==bot.getWorld()
                &&hunter.getLocation().distanceSquared(threatened.getLocation())<=detection*detection;
        boolean directEngage=oniDistance<=detection;

        if(protecting||directEngage||now<azakujiFightUntil){
            clearPlayerBotWork(id);
            playerBotTargetHeart.remove(id);
            if((protecting||directEngage)&&now>=azakujiFightUntil)azakujiFightUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.fight-seconds",14.0)*1000L);

            // Interpose himself between the Oni and the endangered player.
            Location guardPoint=threatened!=null?threatened.getLocation().clone():bot.getLocation().clone();
            if(threatened!=null){
                Vector fromOni=threatened.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                if(fromOni.lengthSquared()<.01)fromOni=new Vector(1,0,0);
                guardPoint=threatened.getLocation().clone().subtract(fromOni.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.intercept-distance",3.0)));
            }

            // 武芸 is his first choice when he can safely dive in.
            if(now>=playerBotSkillReadyAt.getOrDefault(id,0L)
                    &&oniDistance<=getConfig().getDouble("skills.bugei.target-range",18.0)
                    &&oniDistance>=3.5){
                azakujiLastCombatAt=now;
                usePlayerBotBugei(id,bot,hunter);
                playerBotSkillReadyAt.put(id,now+Math.max(1,PlayerSkill.BUGEI.cooldown)*1000L);
                azakujiTauntUntil=Math.max(azakujiTauntUntil,now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L));
                return;
            }

            if(oniDistance<=getConfig().getDouble("azakuji-ally.guard-ai.melee-range",2.8)){
                stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);
                if(now>=azakujiNextMeleeAt){
                    azakujiNextMeleeAt=now+Math.max(1,getConfig().getLong("azakuji-ally.guard-ai.melee-cooldown-ticks",18))*50L;
                    bot.swingMainHand();
                    azakujiLastCombatAt=now;
                    double damage=getConfig().getDouble("azakuji-ally.guard-ai.melee-damage",7.0);
                    damage*=1.20; // 神喰
                    damage*=getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10); // 神技
                    hunter.damage(damage,bot);
                    bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_ATTACK_STRONG,.75f,.9f);
                    azakujiTauntUntil=Math.max(azakujiTauntUntil,now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L));
                }
            }else{
                Location destination=oniDistance<=7.0?hunter.getLocation():guardPoint;
                movePlayerBot(bot,destination,getConfig().getDouble("player-bot.hunt-step",.78)*1.10);
            }

            makeAzakujiTaunt(bot,now);
            // v0.31.2: fixed-time disengage removed; Azakuji stays in combat unless critically wounded.
            return;
        }

        // No immediate danger: stay close enough to the real players to intercept a chase.
        Location ally=nearestActiveHumanLocation(bot);
        if(ally==null)ally=nearestActivePlayerSideLocation(bot);
        if(ally!=null){
            double follow=getConfig().getDouble("azakuji-ally.guard-ai.follow-distance",4.5);
            if(bot.getLocation().distanceSquared(ally)>follow*follow)movePlayerBot(bot,ally,getConfig().getDouble("player-bot.move-step",.65)*1.05);
            else stopPlayerBotNavigation(bot);
        }
    }

    private Player findThreatenedHumanForAzakuji(LivingEntity hunter){
        Player best=null;double bestDist=Double.MAX_VALUE;
        for(UUID pid:players){
            if(dead.contains(pid)||escaped.contains(pid))continue;
            Player p=Bukkit.getPlayer(pid);
            if(p==null||!p.isOnline()||p.getWorld()!=hunter.getWorld())continue;
            double d=p.getLocation().distanceSquared(hunter.getLocation());
            if(d<bestDist){bestDist=d;best=p;}
        }
        return best;
    }

    private Location nearestActiveHumanLocation(LivingEntity from){
        Location best=null;double bestDist=Double.MAX_VALUE;
        for(UUID pid:players){
            if(dead.contains(pid)||escaped.contains(pid))continue;
            Player p=Bukkit.getPlayer(pid);
            if(p==null||!p.isOnline()||p.getWorld()!=from.getWorld())continue;
            double d=p.getLocation().distanceSquared(from.getLocation());
            if(d<bestDist){bestDist=d;best=p.getLocation();}
        }
        return best;
    }

    private Location nearestActivePlayerSideLocation(LivingEntity from){
        Location best=nearestActiveHumanLocation(from);double bestDist=best==null?Double.MAX_VALUE:best.distanceSquared(from.getLocation());
        for(UUID other:playerBots){
            if(other.equals(from.getUniqueId())||deadPlayerBots.contains(other)||escapedPlayerBots.contains(other))continue;
            if(!(Bukkit.getEntity(other) instanceof LivingEntity entity)||!entity.isValid()||entity.isDead()||entity.getWorld()!=from.getWorld())continue;
            double d=entity.getLocation().distanceSquared(from.getLocation());
            if(d<bestDist){bestDist=d;best=entity.getLocation();}
        }
        return best;
    }

    private void makeAzakujiTaunt(LivingEntity bot,long now){
        if(now>=azakujiTauntUntil)return;
        bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,Math.max(10,getConfig().getInt("azakuji-ally.guard-ai.aggro-glow-ticks",30)),0,false,false));
        if(now%1000L<550L)bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1.1,0),5,.25,.35,.25,.02);
    }

    private void azakujiLeapEscape(LivingEntity bot,LivingEntity hunter,Location escape,long now){
        if(now<azakujiNextLeapAt||!bot.isOnGround())return;
        azakujiNextLeapAt=now+Math.max(4,getConfig().getLong("azakuji-ally.guard-ai.leap-cooldown-ticks",24))*50L;
        stopPlayerBotNavigation(bot);
        Vector away=escape.toVector().subtract(bot.getLocation().toVector()).setY(0);
        if(away.lengthSquared()<.01)away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
        if(away.lengthSquared()<.01)away=new Vector(1,0,0);
        away.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.leap-horizontal-speed",.95));
        away.setY(getConfig().getDouble("azakuji-ally.guard-ai.leap-upward-velocity",.72));
        bot.setVelocity(away);bot.setFallDistance(0);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_HORSE_JUMP,.6f,1.35f);
        bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),16,.3,.12,.3,.05);
    }

    private double updatePlayerBotStamina(UUID id,boolean running){double value=playerBotStamina.getOrDefault(id,20.0),drain=getConfig().getDouble("stamina.drain-per-second",2.0)/2.0,recovery=getConfig().getDouble("stamina.recovery-per-second",1.5)/2.0;if(hasPlayerBotPassive(id,PassiveSkill.LIGHT_FOOTED))drain*=.85;if(hasPlayerBotPassive(id,PassiveSkill.DEEP_BREATH))recovery*=1.25;if(running&&value>0)value-=drain;else value+=recovery;value=Math.max(0,Math.min(20,value));playerBotStamina.put(id,value);return value<=0?getConfig().getDouble("player-bot.stamina-empty-step-multiplier",.55):1.0;}
    private Location selectPlayerBotHeart(UUID id,LivingEntity bot){String current=playerBotTargetHeart.get(id);if(current!=null&&(heartHp.containsKey(current)||fakeHeartKeys.contains(current))){Location location=LocationStore.decode(current);if(location!=null&&location.getWorld().equals(bot.getWorld()))return location;}Set<String> botHeartChoices=new HashSet<>(heartHp.keySet());botHeartChoices.addAll(fakeHeartKeys);String chosen=botHeartChoices.stream().filter(key->{Location l=LocationStore.decode(key);return l!=null&&l.getWorld().equals(bot.getWorld());}).min(Comparator.comparingDouble(key->{Location l=LocationStore.decode(key);long assigned=playerBotTargetHeart.entrySet().stream().filter(e->!e.getKey().equals(id)&&e.getValue().equals(key)).count();return l.distanceSquared(bot.getLocation())+assigned*getConfig().getDouble("player-bot.movement.shared-heart-penalty",64.0);})).orElse(null);if(chosen==null)return null;playerBotTargetHeart.put(id,chosen);return LocationStore.decode(chosen);}
    private void usePlayerBotSkill(UUID id,LivingEntity bot,LivingEntity hunter,boolean fleeing,boolean hunting){
        PlayerSkill skill=playerBotSkills.getOrDefault(id,PlayerSkill.SPRINT);long now=System.currentTimeMillis();if(now<playerBotSkillReadyAt.getOrDefault(id,0L))return;double distance=hunter!=null&&hunter.isValid()&&hunter.getWorld().equals(bot.getWorld())?hunter.getLocation().distance(bot.getLocation()):Double.MAX_VALUE;boolean used=false;
        switch(skill){
            case SPRINT -> {if(fleeing||(hunting&&distance>3)){playerBotSprintUntil.put(id,now+(long)(getConfig().getDouble("player-bot.skills.sprint-seconds",5.0)*1000));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_HORSE_GALLOP,.55f,1.25f);bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),12,.3,.1,.3,.04);used=true;}}
            case SMOKE -> {if(fleeing&&distance<=getConfig().getDouble("player-bot.skills.smoke-range",8.0)){bot.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,bot.getLocation().add(0,1,0),70,2.2,1.0,2.2,.03);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,.65f,1.5f);if(hunter!=null)hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,Math.max(20,getConfig().getInt("player-bot.skills.smoke-blindness-ticks",60)),0,false,false));used=true;}}
            case ONI_STRIKE -> {if(hunter!=null&&distance<=getConfig().getDouble("player-bot.skills.strike-range",4.0)){bot.swingMainHand();double damage=brokenHearts>=totalHearts?getConfig().getDouble("player-bot.skills.strike-damage-final",12.0):getConfig().getDouble("player-bot.skills.strike-damage-sealed",2.0);if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;hunter.damage(damage,bot);Vector push=hunter.getLocation().toVector().subtract(bot.getLocation().toVector());if(push.lengthSquared()>.01)hunter.setVelocity(push.normalize().multiply(.8).setY(.3));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,1f,.75f);used=true;}}
            case OBSESSION -> {if(hunter!=null&&(fleeing||hunting)&&distance<=getConfig().getDouble("player-bot.skills.obsession-range",18.0)){int ticks=Math.max(20,getConfig().getInt("player-bot.skills.obsession-ticks",160));bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));hunter.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.65f,1.35f);used=true;}}
            case HEAL -> {double max=bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();boolean needsHealing=bot.getHealth()<=max*getConfig().getDouble("player-bot.skills.heal-threshold",.65),allyNeedsHealing=hasPlayerBotPassive(id,PassiveSkill.BOND)&&hasInjuredBondAlly(bot);if((needsHealing||allyNeedsHealing)&&distance>=getConfig().getDouble("player-bot.skills.heal-safe-distance",9.0)){double amount=getConfig().getDouble("player-bot.skills.heal-amount",8.0);bot.setHealth(Math.min(max,bot.getHealth()+amount));if(hasPlayerBotPassive(id,PassiveSkill.BOND))healBondAllies(bot,amount);bot.getWorld().spawnParticle(Particle.HEART,bot.getLocation().add(0,1,0),8,.4,.5,.4,.02);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.6f,1.4f);used=true;}}
            case INVISIBLE -> {if(fleeing){bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),25,.5,.8,.5,.03);playerBotSprintUntil.put(id,now+(long)(getConfig().getDouble("player-bot.skills.invisible-evasion-seconds",4.0)*1000));used=true;}}
            case BUGEI -> {if(hunter!=null&&distance<=getConfig().getDouble("skills.bugei.target-range",18.0)){usePlayerBotBugei(id,bot,hunter);used=true;}}
            case SAFE_LANDING,BLINK,ECHO,CLAIRVOYANCE,UNYIELDING,SEALING_CIRCLE,RESONANCE,SUBSTITUTE,DESPERATE_RUN -> {}
        }
        if(used)playerBotSkillReadyAt.put(id,now+Math.max(1,skill.cooldown)*1000L);
    }
    private void usePlayerBotBugei(UUID id,LivingEntity bot,LivingEntity hunter){
        if(bot==null||hunter==null||!bot.isValid()||!hunter.isValid())return;
        stopPlayerBotNavigation(bot);
        bot.setVelocity(new Vector(0,getConfig().getDouble("skills.bugei.launch-upward-velocity",1.05),0));
        bot.setFallDistance(0);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.55f,1.5f);
        bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),18,.3,.15,.3,.06);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(state!=GameState.RUNNING||!bot.isValid()||!hunter.isValid())return;
            final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
            task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
                ticks[0]++;
                if(state!=GameState.RUNNING||!bot.isValid()||!hunter.isValid()||bot.getWorld()!=hunter.getWorld()){task[0].cancel();return;}
                Vector to=hunter.getEyeLocation().toVector().subtract(bot.getLocation().toVector());double distance=to.length();
                double speed=getConfig().getDouble("skills.bugei.dive-speed",1.75);
                if(distance<=getConfig().getDouble("skills.bugei.final-boost-distance",5.0))speed*=getConfig().getDouble("skills.bugei.final-boost-multiplier",1.35);
                if(distance>.01)bot.setVelocity(to.normalize().multiply(speed));bot.setFallDistance(0);
                Location trail=bot.getLocation().add(0,.85,0);
                bot.getWorld().spawnParticle(Particle.REDSTONE,trail,2,.1,.1,.1,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,45,20),1.2f));
                bot.getWorld().spawnParticle(Particle.REDSTONE,trail,1,.08,.08,.08,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,190,35),1.0f));
                if(distance<=getConfig().getDouble("skills.bugei.impact-distance",2.2)){
                    double damage=brokenHearts>=totalHearts?getConfig().getDouble("skills.bugei.final-damage",18.0):getConfig().getDouble("skills.bugei.sealed-damage",10.0);
                    if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;
                    if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10);
                    hunter.damage(damage,bot);Location impact=hunter.getLocation().add(0,1,0);
                    bot.getWorld().playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,.5f,1.55f);
                    bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,impact,2,.15,.15,.15,.01);
                    bot.getWorld().spawnParticle(Particle.CRIT_MAGIC,impact,28,.5,.6,.5,.1);
                    all("&6字九字ひろ &f「ライダーキック！」");
                    task[0].cancel();return;
                }
                if(ticks[0]>=Math.max(8,getConfig().getInt("skills.bugei.max-dive-ticks",32)))task[0].cancel();
            },0L,1L);
        },8L);
    }
    private LivingEntity findNearestJakutsukiSnake(LivingEntity bot,double radius){LivingEntity best=null;double bestDist=radius*radius;for(UUID sid:new HashSet<>(jakutsukiSnakes)){Entity e=Bukkit.getEntity(sid);if(!(e instanceof LivingEntity snake)||!snake.isValid()||snake.isDead()||snake.getWorld()!=bot.getWorld())continue;double d=snake.getLocation().distanceSquared(bot.getLocation());if(d<=bestDist){bestDist=d;best=snake;}}return best;}
    private boolean shouldPlayerBotHuntOni(LivingEntity bot,LivingEntity hunter){if(hunter==null||!hunter.isValid()||hunter.isDead()||!hunter.getWorld().equals(bot.getWorld()))return false;if(oniType==OniType.JAKUTSUKI&&brokenHearts>=totalHearts&&playerBotHasSnakeSlash(bot))return true;double botRatio=bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(botRatio<getConfig().getDouble("player-bot.hunt-min-health-ratio",0.60))return false;long allies=playerBots.stream().filter(id->!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id)!=null).count();double oniRatio=hunter.getHealth()/Math.max(1,hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());return allies>=getConfig().getInt("player-bot.hunt-min-allies",2)||oniRatio<=getConfig().getDouble("player-bot.hunt-oni-max-health-ratio",0.50);}
    private void attackOniWithPlayerBot(UUID id,LivingEntity bot,LivingEntity hunter){long now=System.currentTimeMillis();long attackTicks=(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))?getConfig().getLong("jakutsuki-final.bot-attack-cooldown-ticks",14):getConfig().getLong("player-bot.hunt-attack-cooldown-ticks",20);if(now<playerBotNextAttackAt.getOrDefault(id,0L))return;playerBotNextAttackAt.put(id,now+Math.max(1,attackTicks)*50L);bot.swingMainHand();double damage=(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))?Math.max(0,getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0)):Math.max(0,getConfig().getDouble("player-bot.hunt-attack-damage",4.0));if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10);hunter.damage(damage,bot);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_ATTACK_STRONG,0.7f,1.0f);}
    private void movePlayerBot(LivingEntity bot,Location destination,double step){if(!(bot instanceof Mob mob))return;Vector separation=new Vector();double radius=getConfig().getDouble("player-bot.movement.separation-radius",2.2);for(UUID otherId:playerBots){if(otherId.equals(bot.getUniqueId())||!(Bukkit.getEntity(otherId) instanceof LivingEntity other)||!other.getWorld().equals(bot.getWorld()))continue;Vector away=bot.getLocation().toVector().subtract(other.getLocation().toVector()).setY(0);double length=away.length();if(length>0&&length<radius)separation.add(away.normalize().multiply((radius-length)/radius));}Location pathTarget=destination.clone();if(separation.lengthSquared()>0)pathTarget.add(separation.normalize().multiply(Math.min(1.5,radius)));double base=Math.max(.01,getConfig().getDouble("player-bot.move-step",.65)),speed=Math.max(.55,Math.min(1.65,step/base));boolean pathStarted=mob.getPathfinder().moveTo(pathTarget,speed);Location last=playerBotLastLocation.put(bot.getUniqueId(),bot.getLocation().clone());boolean stuck=last!=null&&last.getWorld().equals(bot.getWorld())&&last.distanceSquared(bot.getLocation())<.02;int stuckTicks=stuck?playerBotStuckTicks.getOrDefault(bot.getUniqueId(),0)+1:0;playerBotStuckTicks.put(bot.getUniqueId(),stuckTicks);if((!pathStarted||stuckTicks>=getConfig().getInt("player-bot.movement.stuck-updates-before-jump",3))&&bot.isOnGround())bot.setVelocity(bot.getVelocity().setY(getConfig().getDouble("player-bot.movement.jump-velocity",.42)));if(stuckTicks>=getConfig().getInt("player-bot.movement.stuck-updates-before-recovery",12)){recoverStuckPlayerBot(bot,destination);playerBotStuckTicks.put(bot.getUniqueId(),0);}}
    private void stopPlayerBotNavigation(LivingEntity bot){if(bot instanceof Mob mob)mob.getPathfinder().stopPathfinding();bot.setVelocity(new Vector(0,bot.getVelocity().getY(),0));}
    private void recoverStuckPlayerBot(LivingEntity bot,Location destination){Vector direction=destination.toVector().subtract(bot.getLocation().toVector()).setY(0);if(direction.lengthSquared()>.01){direction.normalize();for(double distance:new double[]{1.5,1.0,.5}){Location candidate=bot.getLocation().clone().add(direction.clone().multiply(distance));for(int y=1;y>=-1;y--){Location adjusted=candidate.clone().add(0,y,0);if(isSafeStandLocation(adjusted)){bot.teleport(adjusted);return;}}}}rescueEntityToSafePlace(bot,false);}
    private Vector findPlayerBotClearDirection(LivingEntity bot,Vector wanted){if(!isPlayerBotBlocked(bot,wanted,1.15))return wanted;double[] angles={38,-38,72,-72,110,-110,180};for(double angle:angles){Vector candidate=rotateHorizontal(wanted,angle);if(!isPlayerBotBlocked(bot,candidate,1.15))return candidate;}return wanted.clone().multiply(-1);}
    private Vector rotateHorizontal(Vector vector,double degrees){double r=Math.toRadians(degrees),cos=Math.cos(r),sin=Math.sin(r);return new Vector(vector.getX()*cos-vector.getZ()*sin,0,vector.getX()*sin+vector.getZ()*cos).normalize();}
    private boolean isPlayerBotBlocked(LivingEntity bot,Vector direction,double distance){Location feet=bot.getLocation().clone().add(direction.clone().multiply(distance));Location head=feet.clone().add(0,1,0);return !feet.getBlock().isPassable()||!head.getBlock().isPassable();}
    private void advanceHeartBot(UUID botId,LivingEntity bot,Block block){
        String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;long now=System.currentTimeMillis();bot.swingMainHand();
        if(!key.equals(playerBotWorkingHeart.get(botId))){playerBotWorkingHeart.put(botId,key);heartProgressCarry.remove(botId);doubleStakesGreatStreak.remove(botId);scheduleNextPlayerBotSkillCheck(botId,now);playerBotSkillChecks.remove(botId);}
        BotSkillCheck check=playerBotSkillChecks.get(botId);
        if(check!=null&&now>=check.resolveAt()){
            playerBotSkillChecks.remove(botId);
            if(check.success()){
                if(check.great()){int bonus=Math.max(0,getConfig().getInt("skill-check-great-bonus",5));if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES)){int streak=doubleStakesGreatStreak.getOrDefault(botId,0)+1;doubleStakesGreatStreak.put(botId,streak);bonus+=Math.min(Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-max-extra",10)),streak*Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-step",2)));}applyScaledHeartDamage(botId,key,bonus);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.7f,1.65f);bot.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,bot.getLocation().add(0,1,0),12,0.3,0.4,0.3,0.02);}
                else if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES)){doubleStakesGreatStreak.remove(botId);int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+Math.max(0,getConfig().getInt("skill-check-failure-penalty",8))));forceHeartAlert(block.getLocation());bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.75f);}
                else if(hasOniPassive(OniPassiveSkill.INTERFERENCE)){regressHeart(block,Math.max(0,getConfig().getInt("oni-passive-skills.interference-normal-regression",2)));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,0.7f,0.75f);}
                else{applyScaledHeartDamage(botId,key,Math.max(0,getConfig().getInt("skill-check-success-bonus",3)));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.7f,1.5f);bot.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,bot.getLocation().add(0,1,0),5,0.3,0.4,0.3,0.02);}
            }else{doubleStakesGreatStreak.remove(botId);int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+Math.max(0,getConfig().getInt("skill-check-failure-penalty",8))));forceHeartAlert(block.getLocation());bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.75f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,block.getLocation().add(.5,.5,.5),1);}
            scheduleNextPlayerBotSkillCheck(botId,now);
        }else if(check==null&&now>=nextPlayerBotSkillCheck.getOrDefault(botId,Long.MAX_VALUE)){
            double focus=hasPlayerBotPassive(botId,PassiveSkill.FOCUS)?getConfig().getDouble("player-bot.passives.focus-skill-check-bonus",.08):0,chance=Math.min(1,getConfig().getDouble("player-bot.skill-check-success-chance",0.78)+focus);if(hasOniPassive(OniPassiveSkill.INTERFERENCE))chance*=Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.interference-bot-success-multiplier",0.80)));boolean success=Math.random()<chance,great=success&&Math.random()<Math.max(0,Math.min(1,getConfig().getDouble("player-bot.skill-check-great-chance",0.15)+(hasPlayerBotPassive(botId,PassiveSkill.FOCUS)?getConfig().getDouble("player-bot.passives.focus-great-bonus",.05):0)));if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES))success=great;long reaction=(long)(getConfig().getDouble("player-bot.skill-check-reaction-seconds",0.65)*1000);playerBotSkillChecks.put(botId,new BotSkillCheck(key,now+reaction,success,great));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,0.7f,1.4f);bot.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,bot.getLocation().add(0,1.4,0),6,0.2,0.2,0.2,0.02);
        }
        double amount=Math.max(1,getConfig().getInt("heart-progress-per-interval",1))*Math.max(.25,getConfig().getDouble("player-bot.heart-progress-multiplier",.85));if(hasPlayerBotPassive(botId,PassiveSkill.EXORCISM))amount*=Math.max(1,getConfig().getDouble("passive-skills.exorcism-heart-progress-multiplier",1.10));applyScaledHeartDamage(botId,key,amount);int hp=heartHp.getOrDefault(key,0);updateHeartCracks(block,hp);notifyHeartAttack(block.getLocation(),key,now);if(hp<=0)destroyHeart(block);
    }
    private void scheduleNextPlayerBotSkillCheck(UUID id,long now){long min=(long)(getConfig().getDouble("skill-check-min-seconds",4.0)*1000),max=(long)(getConfig().getDouble("skill-check-max-seconds",9.0)*1000);nextPlayerBotSkillCheck.put(id,now+min+(long)(Math.random()*Math.max(1,max-min)));}
    private void clearPlayerBotWork(UUID id){playerBotWorkingHeart.remove(id);nextPlayerBotSkillCheck.remove(id);playerBotSkillChecks.remove(id);heartProgressCarry.remove(id);doubleStakesGreatStreak.remove(id);}
    private void updateHorrorState(){
        if(state!=GameState.RUNNING)return;LivingEntity hunter=getOniEntity();if(hunter==null)return;long now=System.currentTimeMillis();updateOniSoundIndicator(hunter);
        double terror=getConfig().getDouble("oni-terror-radius",28.0), detect=getConfig().getDouble("oni-detection-range",24.0), fov=getConfig().getDouble("oni-field-of-view",110.0);
        long lostMillis=(long)(getConfig().getDouble("chase-lost-seconds",8.0)*1000L);
        for(UUID id:players){
            if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player target=Bukkit.getPlayer(id);if(target==null)continue;
            double distance=target.getWorld().equals(hunter.getWorld())?target.getLocation().distance(hunter.getLocation()):Double.MAX_VALUE;
            if(distance<=terror)playHeartbeat(target,distance,terror,now);
            double detectRange=detect+(hasOniPassive(OniPassiveSkill.FOOTSTEP_HUNTER)&&target.isSprinting()?Math.max(0,getConfig().getDouble("oni-passive-skills.footstep-hunter-extra-range",6.0)):0);
            boolean visible=now>=timedSkillUntil.getOrDefault(id+":FOOTSTEP_HIDE",0L)&&distance<=detectRange&&hunter.hasLineOfSight(target)&&isInOniView(hunter,target,fov);
            if(visible){lastSeenAt.put(id,now);if(chased.add(id)){onExpandedOniChaseStart(hunter,target,now);beginChase(hunter,target);enterChaseBgm(target);}}
            else if(chased.contains(id)&&now-lastSeenAt.getOrDefault(id,now)>lostMillis){chased.remove(id);onExpandedOniChaseLost(target,now);scheduleLeaveChaseBgm(target);target.sendTitle(cc("&7追跡から逃れた"),cc("&8息を潜めろ……"),5,30,10);target.playSound(target.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,0.5f,0.6f);}
        }
        chased.removeIf(id->dead.contains(id)||escaped.contains(id)||Bukkit.getPlayer(id)==null);updateOniPassiveEffects(hunter,now);
    }
    private void updateOniSoundIndicator(LivingEntity hunter){if(!(hunter instanceof Player oniPlayer)||!getConfig().getBoolean("oni-sound-visualizer.enabled",true))return;double radius=Math.max(8,getConfig().getDouble("oni-sound-visualizer.radius",52)),bestScore=0,bestDistance=0;Location best=null;String soundType="物音";for(UUID id:players){if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player target=Bukkit.getPlayer(id);if(target==null||target.getGameMode()==GameMode.SPECTATOR||!target.getWorld().equals(hunter.getWorld()))continue;if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(id+":ECHO_SILENT",0L))continue;if(hasPassive(id,PassiveSkill.SILENT_BREATH)&&target.isSneaking()&&System.currentTimeMillis()-sneakStartedAt.getOrDefault(id,System.currentTimeMillis())>=getConfig().getLong("technical-passives.silent-breath-charge-millis",2000)&&target.getVelocity().setY(0).lengthSquared()<.003)continue;double distance=target.getLocation().distance(hunter.getLocation());if(distance>radius)continue;boolean moving=target.getVelocity().setY(0).lengthSquared()>0.006;double intensity=System.currentTimeMillis()<timedSkillUntil.getOrDefault(id+":DESPERATE",0L)?3.6:repairingHeart.containsKey(id)?3.4:chestOpeningTasks.containsKey(id)?2.8:target.isSprinting()?2.4:moving?(target.isSneaking()?.35:1.1):target.getHealth()<target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*.5?.45:0;if(intensity<=0||distance>radius*Math.min(1,intensity/1.1))continue;double score=intensity/(distance+4);if(score>bestScore){bestScore=score;bestDistance=distance;best=target.getLocation();soundType=repairingHeart.containsKey(id)?"心臓損壊":chestOpeningTasks.containsKey(id)?"開錠音":target.isSprinting()?"走る音":"足音";}}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;Entity entity=Bukkit.getEntity(id);if(!(entity instanceof LivingEntity bot)||!bot.getWorld().equals(hunter.getWorld()))continue;double distance=bot.getLocation().distance(hunter.getLocation());if(distance>radius)continue;double intensity=playerBotWorkingHeart.containsKey(id)?3.4:bot.getVelocity().setY(0).lengthSquared()>0.006?1.25:0;if(intensity<=0||distance>radius*Math.min(1,intensity/1.1))continue;double score=intensity/(distance+4);if(score>bestScore){bestScore=score;bestDistance=distance;best=bot.getLocation();soundType=playerBotWorkingHeart.containsKey(id)?"心臓損壊":"足音";}}for(Map.Entry<UUID,Long> noise:new HashMap<>(fakeNoiseUntil).entrySet()){Entity entity=Bukkit.getEntity(noise.getKey());if(entity==null||System.currentTimeMillis()>noise.getValue()){fakeNoiseUntil.remove(noise.getKey());if(entity!=null)entity.remove();continue;}if(!entity.getWorld().equals(hunter.getWorld()))continue;double distance=entity.getLocation().distance(hunter.getLocation());if(distance<=radius&&3.0/(distance+4)>bestScore){bestScore=3.0/(distance+4);bestDistance=distance;best=entity.getLocation();soundType="走る音";}}if(best==null)return;String arrow=soundDirectionArrow(oniPlayer,best),range=bestDistance<12?"&c近":bestDistance<28?"&e中":"&7遠";oniPlayer.sendActionBar(cc("&4▣ 音響感知  &f"+arrow+"  "+range+" &8| &7"+soundType));Vector direction=best.toVector().subtract(oniPlayer.getEyeLocation().toVector()).setY(0);if(direction.lengthSquared()>.01){direction.normalize();Location origin=oniPlayer.getEyeLocation().add(0,-.35,0);for(int i=1;i<=7;i++)oniPlayer.spawnParticle(Particle.SOUL_FIRE_FLAME,origin.clone().add(direction.clone().multiply(i*.55)),1,0,0,0,0);}}
    private String soundDirectionArrow(Player viewer,Location source){double dx=source.getX()-viewer.getLocation().getX(),dz=source.getZ()-viewer.getLocation().getZ(),targetYaw=Math.toDegrees(Math.atan2(-dx,dz)),relative=targetYaw-viewer.getLocation().getYaw();while(relative<=-180)relative+=360;while(relative>180)relative-=360;String[] arrows={"↑","↗","→","↘","↓","↙","←","↖"};int index=Math.floorMod((int)Math.round(relative/45.0),8);return "&b&l"+arrows[index];}
    private void registerOniGrudge(UUID attacker){
        if(attacker==null)return;if(hasOniPassive(OniPassiveSkill.SPELL_BREAK)||hasOniPassive(OniPassiveSkill.ADAPTATION))oniDisruptionStacks.put(attacker,Math.min(4,oniDisruptionStacks.getOrDefault(attacker,0)+1));if(!hasOniPassive(OniPassiveSkill.GRUDGE_RETURN))return;
        int next=grudgeTokens.getOrDefault(attacker,0)+1;
        if(next>=3){
            grudgeTokens.put(attacker,0);
            grudgeResistReady.add(attacker);
            LivingEntity hunter=getOniEntity();
            if(hunter instanceof Player op){
                Player source=Bukkit.getPlayer(attacker);
                op.sendActionBar(cc("&5怨返し &7――"+(source!=null?source.getName():"獲物")+"への耐性準備"));
            }
        }else grudgeTokens.put(attacker,next);
    }

    private boolean consumeGrudgeResistance(UUID attacker){
        return attacker!=null&&hasOniPassive(OniPassiveSkill.GRUDGE_RETURN)&&grudgeResistReady.remove(attacker);
    }

    private boolean hasOniPassive(OniPassiveSkill passive){
        if(activeOniPassives.contains(passive))return true;
        return state==GameState.WAITING&&!trainingPlayers.isEmpty()&&selectedOniPassives.contains(passive);
    }
    private boolean isOniSkillAction(String id){
        return id!=null&&(id.startsWith("dakko_")||id.startsWith("kishin_")||id.startsWith("jakutsuki_"));
    }
    private long oniBotAbilityCooldownMillis(double seconds){
        double multiplier=hasOniPassive(OniPassiveSkill.MASTERY)?Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.mastery-cooldown-multiplier",.85))):1.0;LivingEntity hunter=getOniEntity();if(hasOniPassive(OniPassiveSkill.TERRITORY)&&hunter!=null&&oniNearAliveHeart(hunter,getConfig().getDouble("oni-passive-skills.territory-radius",8.0)))multiplier*=Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.territory-cooldown-multiplier",.80)));return Math.max(1000L,(long)(seconds*1000.0*multiplier));
    }
    private void recoverOniSkillMomentum(){
        if(!hasOniPassive(OniPassiveSkill.MOMENTUM))return;
        double amount=Math.max(0,getConfig().getDouble("oni-passive-skills.momentum-stamina-recovery",2.0));
        if(oniBot!=null){
            botStamina=Math.min(20,botStamina+amount);
        }else if(oni!=null){
            double value=Math.min(20,stamina.getOrDefault(oni,20.0)+amount);stamina.put(oni,value);
            Player p=Bukkit.getPlayer(oni);if(p!=null){p.setFoodLevel((int)Math.ceil(value));p.sendActionBar(cc("&6余勢 &7――スキルの勢いでスタミナ回復"));}
        }
    }
    private void onExpandedOniChaseStart(LivingEntity hunter,Player target,long now){
        long idle=oniLastAnyChaseAt==0?Long.MAX_VALUE:now-oniLastAnyChaseAt;oniLastAnyChaseAt=now;
        if(hasOniPassive(OniPassiveSkill.BEAST_PATH))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.beast-path-speed-ticks",50)),0,false,true));
        if(hasOniPassive(OniPassiveSkill.AMBUSH)&&idle>=Math.max(1000,getConfig().getLong("oni-passive-skills.ambush-idle-seconds",10)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.ambush-speed-ticks",70)),1,false,true));
        if(hasOniPassive(OniPassiveSkill.OBSESSION_HUNT)&&now-oniChaseLostAt.getOrDefault(target.getUniqueId(),0L)<=Math.max(1000,getConfig().getLong("oni-passive-skills.obsession-reacquire-seconds",20)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.obsession-speed-ticks",50)),0,false,true));
    }
    private void onExpandedOniChaseLost(Player target,long now){
        oniChaseLostAt.put(target.getUniqueId(),now);
        if(hasOniPassive(OniPassiveSkill.SCENT_TRAIL)){oniScentTrailLocation.put(target.getUniqueId(),target.getLocation().clone());oniScentTrailUntil.put(target.getUniqueId(),now+Math.max(1000,getConfig().getLong("oni-passive-skills.scent-trail-seconds",8)*1000L));}
    }
    private boolean oniNearAliveHeart(LivingEntity hunter,double radius){
        double r2=radius*radius;for(String key:heartHp.keySet()){Location loc=LocationStore.decode(key);if(loc!=null&&loc.getWorld()!=null&&loc.getWorld().equals(hunter.getWorld())&&loc.distanceSquared(hunter.getLocation())<=r2)return true;}return false;
    }
    private void updateExpandedOniPassives(LivingEntity hunter,long now){
        if(hasOniPassive(OniPassiveSkill.SCENT_TRAIL)&&hunter instanceof Player op){for(UUID id:new HashSet<>(oniScentTrailUntil.keySet())){long until=oniScentTrailUntil.getOrDefault(id,0L);Location loc=oniScentTrailLocation.get(id);if(now>until||loc==null){oniScentTrailUntil.remove(id);oniScentTrailLocation.remove(id);continue;}if(loc.getWorld()!=null&&loc.getWorld().equals(op.getWorld()))op.spawnParticle(Particle.CRIMSON_SPORE,loc.clone().add(0,1,0),5,.35,.45,.35,.01);}}
        if(hasOniPassive(OniPassiveSkill.HUNTERS_INSTINCT)&&chased.isEmpty()&&now-oniLastAnyChaseAt>=Math.max(1000,getConfig().getLong("oni-passive-skills.hunters-instinct-idle-seconds",8)*1000L)&&now>=oniHuntersInstinctAt){oniHuntersInstinctAt=now+Math.max(1000,getConfig().getLong("oni-passive-skills.hunters-instinct-interval-seconds",12)*1000L);Player nearest=null;double best=Double.MAX_VALUE;for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(hunter.getWorld()))continue;double d=q.getLocation().distanceSquared(hunter.getLocation());if(d<best){best=d;nearest=q;}}if(nearest!=null&&hunter instanceof Player op)op.sendActionBar(cc("&6狩人の勘 &7――獲物は "+soundDirectionArrow(op,nearest.getLocation())+" &7方向"));}
        if(hasOniPassive(OniPassiveSkill.SEE_THROUGH)&&hunter instanceof Player op){for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(op.getWorld()))continue;boolean hidden=q.hasPotionEffect(PotionEffectType.INVISIBILITY)||now<timedSkillUntil.getOrDefault(id+":ECHO_SILENT",0L)||now<timedSkillUntil.getOrDefault(id+":FOOTSTEP_HIDE",0L);if(hidden&&q.getLocation().distanceSquared(op.getLocation())<=900){op.sendActionBar(cc("&5看破 &7――消えた気配 "+soundDirectionArrow(op,q.getLocation())));break;}}}
        if(hasOniPassive(OniPassiveSkill.PRESSURE)){for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(hunter.getWorld()))continue;if(q.getLocation().distanceSquared(hunter.getLocation())<=Math.pow(getConfig().getDouble("oni-passive-skills.pressure-radius",3.5),2))q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,12,0,false,true));}}
        if(hasOniPassive(OniPassiveSkill.LAST_FORTRESS)&&Math.max(0,totalHearts-brokenHearts)<=Math.max(1,getConfig().getInt("oni-passive-skills.last-fortress-heart-threshold",2))){hunter.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,15,0,false,true));hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,15,0,false,true));}
        if(hasOniPassive(OniPassiveSkill.HUNTING_GROUND)){if(oniHuntingGroundAnchor==null||!oniHuntingGroundAnchor.getWorld().equals(hunter.getWorld())||oniHuntingGroundAnchor.distanceSquared(hunter.getLocation())>36){oniHuntingGroundAnchor=hunter.getLocation().clone();oniHuntingGroundSince=now;}else if(now-oniHuntingGroundSince>=Math.max(1000,getConfig().getLong("oni-passive-skills.hunting-ground-seconds",6)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,15,0,false,true));}else{oniHuntingGroundAnchor=null;oniHuntingGroundSince=0;}
        if(hasOniPassive(OniPassiveSkill.HEART_EYE)&&hunter instanceof Player op){for(String key:repairingHeart.values()){Location loc=LocationStore.decode(key);if(loc!=null&&loc.getWorld()!=null&&loc.getWorld().equals(op.getWorld()))op.spawnParticle(Particle.SOUL_FIRE_FLAME,loc.clone().add(.5,1,.5),8,.3,.45,.3,.01);}}
    }
    private void updateOniPassiveEffects(LivingEntity hunter,long now){
        double botChaseRange=Math.pow(getConfig().getDouble("player-bot.flee-radius",10.0),2);boolean chasing=!chased.isEmpty()||playerBots.stream().filter(id->!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)).map(Bukkit::getEntity).filter(Objects::nonNull).anyMatch(entity->entity.getWorld().equals(hunter.getWorld())&&entity.getLocation().distanceSquared(hunter.getLocation())<=botChaseRange);
        updateExpandedOniPassives(hunter,now);
        if(hasOniPassive(OniPassiveSkill.CRAVING)&&chasing){if(oniChaseStartedAt==0)oniChaseStartedAt=now;long step=Math.max(1,getConfig().getLong("oni-passive-skills.craving-step-seconds",5))*1000L;int level=(int)((now-oniChaseStartedAt)/step),max=Math.max(0,getConfig().getInt("oni-passive-skills.craving-max-amplifier",1));if(level>0)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,15,Math.min(max,level-1),false,true));}else{oniChaseStartedAt=0;}
        if(!hasOniPassive(OniPassiveSkill.BLOOD_SCENT)||now<nextBloodScentAt)return;nextBloodScentAt=now+Math.max(1,getConfig().getLong("oni-passive-skills.blood-scent-interval-seconds",10))*1000L;double threshold=Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.blood-scent-health-ratio",0.70)));int ticks=Math.max(1,getConfig().getInt("oni-passive-skills.blood-scent-glow-ticks",40));boolean found=false;
        for(UUID id:players){Player target=Bukkit.getPlayer(id);if(target==null||dead.contains(id)||escaped.contains(id)||target.getHealth()>target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*threshold)continue;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));found=true;}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity target)||target.getHealth()>target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*threshold)continue;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));found=true;}
        if(found){hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_SNIFF,0.8f,0.8f);if(hunter instanceof Player p)p.sendActionBar(cc("&4血嗅 &7――傷ついた獲物の気配を捉えた"));}
    }
    private boolean isInOniView(LivingEntity hunter,Player target,double fov){if(hunter.getLocation().distanceSquared(target.getLocation())<=9)return true;Vector direction=hunter.getEyeLocation().getDirection().normalize();Vector toward=target.getEyeLocation().toVector().subtract(hunter.getEyeLocation().toVector()).normalize();return direction.dot(toward)>=Math.cos(Math.toRadians(fov/2.0));}
    private void beginChase(LivingEntity hunter,Player target){target.sendTitle(cc("&4&l見つかった"),cc("&c逃げろ"),5,35,10);target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,35,0,false,false));target.playSound(target.getLocation(),Sound.BLOCK_SCULK_SHRIEKER_SHRIEK,1.0f,1.0f);if(hunter instanceof Player p)p.sendActionBar(cc("&4獲物を発見: &f"+target.getName()));hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_ANGRY,0.5f,1.2f);}
    private void playHeartbeat(Player target,double distance,double terror,long now){double ratio=Math.max(0,Math.min(1,distance/terror));long interval=(long)(350+ratio*1200);if(now-heartbeatAt.getOrDefault(target.getUniqueId(),0L)<interval)return;heartbeatAt.put(target.getUniqueId(),now);float volume=(float)(0.35+(1-ratio)*0.9);float pitch=(float)(0.65+(1-ratio)*0.45);target.playSound(target.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,volume,pitch);}
    private void updateSidebars(){if(state==GameState.RUNNING||state==GameState.ENDING)updateGameSidebar();else updateLobbySidebar();}
    private void updateLobbySidebar(){
        for(Player viewer:Bukkit.getOnlinePlayers()){
            Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
            Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);
            boolean training=isInTrainingArea(viewer.getLocation());
            o.getScore(cc("&7──────────")).setScore(training?5:3);
            if(training){o.getScore(cc("&b&lTRAINING MODE")).setScore(4);o.getScore(cc("&fCT &e"+Math.max(1,getConfig().getInt("training.cooldown-seconds",1))+"秒")).setScore(3);}
            o.getScore(cc("&fオンライン数")).setScore(2);
            o.getScore(cc("&a"+Bukkit.getOnlinePlayers().size()+" &7人")).setScore(1);
            viewer.setScoreboard(board);
        }
    }
    private void updateGameSidebar(){
        Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score=15;
        o.getScore(cc("&e残り時間 &f"+(Math.max(0,secondsLeft)/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60))).setScore(score--);
        o.getScore(cc("&c心臓 &f"+brokenHearts+"&7/&f"+totalHearts)).setScore(score--);
        o.getScore(cc("&8──────────")).setScore(score--);
        List<UUID> shown=new ArrayList<>(players);shown.sort(Comparator.comparing(id->{Player p=Bukkit.getPlayer(id);return p==null?"":p.getName();}));
        for(UUID id:shown){
            if(score<=0)break;
            Player target=Bukkit.getPlayer(id);String name=target!=null?target.getName():"退出者";String icon;
            if(escaped.contains(id))icon="&f◆";
            else if(dead.contains(id)||target==null)icon="&c✖";
            else {double max=Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();icon=target.getHealth()<=max*.30?"&e▲":"&a●";}
            o.getScore(cc(icon+" &f"+trimName(name))).setScore(score--);
        }
        int botNumber=1;for(UUID id:playerBots){
            if(score<=0)break;
            Entity botEntity=Bukkit.getEntity(id);
            String icon=escapedPlayerBots.contains(id)?"&f◆":deadPlayerBots.contains(id)||botEntity==null?"&c✖":"&a●";
            boolean friendly=id.equals(configuredFriendlyNpcId)||id.equals(azakujiAllyId);
            String shownName;
            if(friendly){
                shownName=playerBotPresetNames.getOrDefault(id,"友軍NPC");
                o.getScore(cc(icon+" &a"+trimName(shownName))).setScore(score--);
            }else{
                o.getScore(cc(icon+" &fBot "+botNumber)).setScore(score--);
            }
            botNumber++;
        }
        if(score>0)o.getScore(cc("&8──────────&0")).setScore(score--);
        if(score>0)o.getScore(cc("&a● 通常  &e▲ 瀕死")).setScore(score--);
        if(score>0)o.getScore(cc("&c✖ 脱落  &f◆ 脱出")).setScore(score);
        for(Player viewer:Bukkit.getOnlinePlayers())viewer.setScoreboard(board);
    }
    private String trimName(String name){return name.length()>12?name.substring(0,12):name;}
    private void checkExit(){if(brokenHearts<totalHearts)return;if(System.currentTimeMillis()<exitSealedUntil)return;Location exit=activeExit!=null?activeExit:LocationStore.get(getConfig(),"locations.exit");if(exit==null)return;for(UUID id:new HashSet<>(players)){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld().equals(exit.getWorld())&&p.getLocation().distanceSquared(exit)<=Math.pow(getConfig().getDouble("exit-radius",3),2)){escaped.add(id);p.setGameMode(GameMode.SPECTATOR);all("&b"+p.getName()+" &fが脱出しました！");}}checkPlayerOutcome();}
    private void checkPlayerOutcome(){if(state!=GameState.RUNNING)return;long active=players.stream().filter(id->!dead.contains(id)&&!escaped.contains(id)).count()+playerBots.stream().filter(id->!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)).count();if(active==0){if(!escaped.isEmpty()||!escapedPlayerBots.isEmpty())end(false,"&b生 還――ぷれいやーの勝利");else end(true,"&4全 滅――鬼の勝利");}}
    private void end(boolean oniWin,String reason){if(state==GameState.WAITING)return;state=GameState.ENDING;if(ticker!=null)ticker.cancel();stopGameStartBgm();stopFinalPhaseBgm();all((oniWin?"&4&l":"&b&l")+reason);Bukkit.getScheduler().runTaskLater(this,()->{Location lobby=activeLobby!=null?activeLobby:LocationStore.get(getConfig(),"locations.lobby");restoreTemporaryTestHearts();resetFinalCrimsonVisuals();for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null)continue;p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);common(p);if(lobby!=null)p.teleport(lobby);}if(testParticipant!=null){participants.remove(testParticipant);selectedSkill.remove(testParticipant);selectedPresetNames.remove(testParticipant);testParticipant=null;}activeLobby=null;activeExit=null;activePlayerSpawn=null;restoreLobbyUtilities();state=GameState.WAITING;resetRuntime();refreshLobbyGuideDisplays();},100);}

    @EventHandler public void onBreak(BlockBreakEvent e){String key=LocationStore.encode(e.getBlock().getLocation());if((state==GameState.RUNNING&&(heartHp.containsKey(key)||lootChestKeys.contains(key)))||(state==GameState.WAITING&&(isPracticeHeartKey(key)||isPracticeChest(e.getBlock()))))e.setCancelled(true);}
    private void toggleHeartRepair(Player player,Block block){String key=LocationStore.encode(block.getLocation());boolean practice=state==GameState.WAITING&&isPracticeHeartKey(key);if(!practice&&(!heartHp.containsKey(key)||!players.contains(player.getUniqueId())||dead.contains(player.getUniqueId())||escaped.contains(player.getUniqueId())))return;if(practice&&!repairingHeart.containsValue(key)){heartHp.put(key,getConfig().getInt("heart-max-health",100));updateHeartCracks(block,getConfig().getInt("heart-max-health",100));}if(key.equals(repairingHeart.get(player.getUniqueId()))){stopRepair(player,"&7心臓への干渉を中断した。 ");return;}stopHealing(player,"&7回復を中断した。");chestOpeningTasks.remove(player.getUniqueId());repairingHeart.put(player.getUniqueId(),key);doubleStakesGreatStreak.remove(player.getUniqueId());heartProgressCarry.remove(player.getUniqueId());long now=System.currentTimeMillis();repairStartedAt.put(player.getUniqueId(),now);repairProgressAt.put(player.getUniqueId(),now);scheduleNextSkillCheck(player.getUniqueId(),now);skillChecks.remove(player.getUniqueId());player.sendActionBar(cc(practice?"&d心臓破壊の練習開始 &7――本番と同じくSHIFT判定あり":"&4心臓への干渉を開始 &7――離れると中断"));player.playSound(player.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,0.7f,0.65f);}
    private boolean isPracticeHeartKey(String key){Location l=LocationStore.get(getConfig(),"locations.practice-heart");return l!=null&&LocationStore.encode(l).equals(key);}
    private boolean isPracticeChest(Block block){return matchesConfiguredChest(block,"locations.practice-loot-chest");}
    private void stopRepair(Player player,String message){UUID id=player.getUniqueId();repairingHeart.remove(id);repairStartedAt.remove(id);repairProgressAt.remove(id);nextSkillCheckAt.remove(id);skillChecks.remove(id);heartProgressCarry.remove(id);doubleStakesGreatStreak.remove(id);if(message!=null)player.sendActionBar(cc(message));}
    private void scheduleNextSkillCheck(UUID id,long now){long min=(long)(getConfig().getDouble("skill-check-min-seconds",4.0)*1000),max=(long)(getConfig().getDouble("skill-check-max-seconds",9.0)*1000);nextSkillCheckAt.put(id,now+min+(long)(Math.random()*Math.max(1,max-min)));}
    private int heartWorkerCount(String heartKey){
        if(heartKey==null||state!=GameState.RUNNING)return 1;
        int count=0;
        for(Map.Entry<UUID,String> e:repairingHeart.entrySet()){
            if(!heartKey.equals(e.getValue())||dead.contains(e.getKey())||escaped.contains(e.getKey()))continue;
            Player p=Bukkit.getPlayer(e.getKey());if(p!=null&&p.isOnline())count++;
        }
        for(Map.Entry<UUID,String> e:playerBotWorkingHeart.entrySet()){
            if(!heartKey.equals(e.getValue())||deadPlayerBots.contains(e.getKey())||escapedPlayerBots.contains(e.getKey()))continue;
            if(Bukkit.getEntity(e.getKey()) instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead())count++;
        }
        return Math.max(1,count);
    }
    private double heartGroupMultiplier(String heartKey){
        int workers=heartWorkerCount(heartKey);
        if(workers<=1)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-1",1.00));
        if(workers==2)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-2",.85));
        if(workers==3)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-3",.70));
        if(workers==4)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-4",.60));
        return Math.max(0,getConfig().getDouble("heart-group-scaling.player-5-plus",.50));
    }
    private int applyScaledHeartDamage(UUID source,String key,double amount){
        if(amount<=0||!heartHp.containsKey(key))return 0;
        double total=amount*heartGroupMultiplier(key)+heartProgressCarry.getOrDefault(source,0.0);
        int whole=(int)Math.floor(total);
        heartProgressCarry.put(source,total-whole);
        if(whole>0)heartHp.computeIfPresent(key,(k,hp)->Math.max(0,hp-whole));
        return whole;
    }
    private int doubleStakesGreatBonus(Player player){
        UUID id=player.getUniqueId();
        if(!hasPassive(id,PassiveSkill.DOUBLE_STAKES))return Math.max(0,getConfig().getInt("skill-check-great-bonus",5));
        int streak=doubleStakesGreatStreak.getOrDefault(id,0)+1;
        doubleStakesGreatStreak.put(id,streak);
        int step=Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-step",2));
        int cap=Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-max-extra",10));
        int extra=Math.min(cap,streak*step);
        player.sendActionBar(cc("&6賭け金二倍 &7――連続GREAT &e"+streak+" &7/ 追加損傷 &c+"+extra));
        return Math.max(0,getConfig().getInt("skill-check-great-bonus",5))+extra;
    }
    private void updatePlayerActions(){updateHeartRepairs();updateHealing();updateChestOpening();updateSealingCircles();updateFakeHearts();}
    private void updateFakeHearts(){
        if(state!=GameState.RUNNING||oniType!=OniType.JAKUTSUKI||fakeHeartKeys.isEmpty())return;
        double radius=Math.max(.5,getConfig().getDouble("jakutsuki-hearts.fake-trigger-radius",3.0)),radiusSq=radius*radius;
        for(String key:new HashSet<>(fakeHeartKeys)){
            Location heart=LocationStore.decode(key);if(heart==null)continue;Location center=heart.clone().add(.5,.5,.5);boolean trigger=false;
            for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld().equals(center.getWorld())&&p.getLocation().distanceSquared(center)<=radiusSq){trigger=true;break;}}
            if(!trigger)for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead()&&bot.getWorld().equals(center.getWorld())&&bot.getLocation().distanceSquared(center)<=radiusSq){trigger=true;break;}}
            if(trigger)detonateFakeHeart(key,center);
        }
    }
    private void detonateFakeHeart(String key,Location center){
        if(!fakeHeartKeys.remove(key))return;
        double radius=Math.max(1,getConfig().getDouble("jakutsuki-hearts.fake-explosion-radius",4.5)),radiusSq=radius*radius,damage=Math.max(0,getConfig().getDouble("jakutsuki-hearts.fake-damage",7.0));
        int glowTicks=Math.max(1,getConfig().getInt("jakutsuki-hearts.fake-glow-ticks",160));
        World world=center.getWorld();world.playSound(center,Sound.ENTITY_GENERIC_EXPLODE,1.25f,.72f);world.spawnParticle(Particle.EXPLOSION_HUGE,center,1);world.spawnParticle(Particle.EXPLOSION_LARGE,center,5,.7,.7,.7,.08);world.spawnParticle(Particle.SMOKE_LARGE,center,45,1.2,1.0,1.2,.06);world.spawnParticle(Particle.SOUL,center,30,1.1,1.0,1.1,.05);
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world)||p.getLocation().distanceSquared(center)>radiusSq)continue;dealFixedDamage(p,damage);p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,glowTicks,0,false,true));p.sendActionBar(cc("&4偽心臓が爆発した！ &e発光状態になった")); }
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.isValid()||bot.isDead()||!bot.getWorld().equals(world)||bot.getLocation().distanceSquared(center)>radiusSq)continue;dealFixedDamage(bot,damage);bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,glowTicks,0,false,true));}
        restoreFakeHeartBlock(key);
    }
    private void restoreFakeHeartBlock(String key){Material original=fakeHeartOriginals.remove(key);Location location=LocationStore.decode(key);if(original!=null&&location!=null)location.getBlock().setType(original);}
    private void restoreFakeHearts(){for(String key:new HashSet<>(fakeHeartOriginals.keySet()))restoreFakeHeartBlock(key);fakeHeartKeys.clear();fakeHeartOriginals.clear();}
    private void updateSealingCircles(){long now=System.currentTimeMillis();LivingEntity hunter=getOniEntity();for(UUID id:new HashSet<>(sealingCircles.keySet())){Location location=sealingCircles.get(id);if(location==null||now>sealingCircleUntil.getOrDefault(id,0L)){sealingCircles.remove(id);sealingCircleUntil.remove(id);continue;}location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,location.clone().add(0,.15,0),6,1.5,.1,1.5,.05);if(hunter!=null&&hunter.getWorld().equals(location.getWorld())&&hunter.getLocation().distanceSquared(location)<=2.25){hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,2,false,true));hunter.getWorld().playSound(location,Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,1,.7f);sealingCircles.remove(id);sealingCircleUntil.remove(id);}}}
    private void updateHeartRepairs(){
        if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();
        for(Map.Entry<UUID,String> entry:new HashMap<>(repairingHeart).entrySet()){
            Player player=Bukkit.getPlayer(entry.getKey());Location location=LocationStore.decode(entry.getValue());
            if(player==null||location==null||(!heartHp.containsKey(entry.getValue())&&!isPracticeHeartKey(entry.getValue()))||(state==GameState.RUNNING&&(dead.contains(entry.getKey())||escaped.contains(entry.getKey())))||!player.getWorld().equals(location.getWorld())||player.getLocation().distanceSquared(location.clone().add(.5,.5,.5))>12.25){if(player!=null)stopRepair(player,"&7心臓から離れたため作業を中断した。");continue;}
            SkillCheck check=skillChecks.get(entry.getKey());
            if(check!=null){if(now>check.endsAt()){failSkillCheck(player,location,entry.getValue(),"時間切れ");continue;}showSkillCheck(player,check,now);}
            else if(now>=nextSkillCheckAt.getOrDefault(entry.getKey(),Long.MAX_VALUE)){startSkillCheck(player,now);continue;}
            long interval=getConfig().getLong("heart-progress-interval-ticks",10)*50L;if(now-repairProgressAt.getOrDefault(entry.getKey(),0L)>=interval){repairProgressAt.put(entry.getKey(),now);double amount=Math.max(1,getConfig().getInt("heart-progress-per-interval",1));if(hasPassive(player.getUniqueId(),PassiveSkill.EXORCISM))amount*=Math.max(1.0,getConfig().getDouble("passive-skills.exorcism-heart-progress-multiplier",1.10));if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(player.getUniqueId()+":DESPERATE",0L))amount*=Math.max(1.0,getConfig().getDouble("passive-skills.desperate-heart-progress-multiplier",1.20));if(hasPassive(player.getUniqueId(),PassiveSkill.PRACTICED)){long seconds=(now-repairStartedAt.getOrDefault(entry.getKey(),now))/1000;double step=Math.max(0,getConfig().getDouble("passive-skills.practiced-heart-progress-step",.025)),cap=Math.max(0,getConfig().getDouble("passive-skills.practiced-heart-progress-max",.10));amount*=1+Math.min(cap,Math.floor(seconds/5)*step);}advanceHeart(player,location.getBlock(),amount);}
        }
    }
    private boolean canStartHealing(Player player){double max=player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(player.getHealth()>=max-.01){msg(player,"体力はすでに満タンです。");return false;}return !dead.contains(player.getUniqueId())&&!escaped.contains(player.getUniqueId());}
    private void startHealing(Player player,boolean skill){if(!canStartHealing(player))return;if(healingTasks.containsKey(player.getUniqueId())){stopHealing(player,"&7回復を中断した。");return;}if(skill&&!ready(player,"HEAL",PlayerSkill.HEAL.cooldown))return;if(repairingHeart.containsKey(player.getUniqueId()))stopRepair(player,"&7心臓への干渉を中断した。");chestOpeningTasks.remove(player.getUniqueId());double seconds=getConfig().getDouble(skill?"healing.skill-duration-seconds":"healing.item-duration-seconds",skill?5.0:8.0),amount=getConfig().getDouble(skill?"healing.skill-amount":"healing.item-amount",skill?8.0:5.0);long now=System.currentTimeMillis();healingTasks.put(player.getUniqueId(),new HealingTask(player.getLocation().clone(),now,now+(long)(seconds*1000),amount,skill?null:"item:heal"));player.sendActionBar(cc(skill?"&a治癒を開始 &7――動くか攻撃を受けると中断":"&a包帯を巻き始めた &7――動くか攻撃を受けると中断"));player.playSound(player.getLocation(),Sound.ITEM_ARMOR_EQUIP_LEATHER,0.8f,1.1f);}
    private void updateHealing(){if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();for(Map.Entry<UUID,HealingTask> entry:new HashMap<>(healingTasks).entrySet()){Player player=Bukkit.getPlayer(entry.getKey());HealingTask task=entry.getValue();double tolerance=getConfig().getDouble("healing.move-tolerance",0.45)+(hasPassive(entry.getKey(),PassiveSkill.FOCUS)?0.20:0.0);if(player==null||(state==GameState.WAITING&&!isInTrainingArea(player.getLocation()))||dead.contains(entry.getKey())||escaped.contains(entry.getKey())||!player.getWorld().equals(task.start().getWorld())||player.getLocation().distanceSquared(task.start())>tolerance*tolerance){if(player!=null)stopHealing(player,"&c動いたため回復を中断した。");else healingTasks.remove(entry.getKey());continue;}if(now>=task.endsAt()){double max=player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();player.setHealth(Math.min(max,player.getHealth()+task.amount()));healingTasks.remove(entry.getKey());if(task.consumableAction()!=null)consumeInventoryAction(player,task.consumableAction());else if(hasPassive(entry.getKey(),PassiveSkill.BOND)){int healed=healBondAllies(player,task.amount());if(healed>0)player.sendActionBar(cc("&d絆 &7――周囲の仲間 "+healed+"人も回復した"));}player.sendTitle("",cc("&a&l回復完了 &f+"+(int)task.amount()),0,20,8);player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.7f,1.4f);continue;}double ratio=(now-task.startedAt())/(double)Math.max(1,task.endsAt()-task.startedAt());int filled=Math.max(0,Math.min(16,(int)Math.floor(ratio*16)));StringBuilder bar=new StringBuilder("&a回復 &8[");for(int i=0;i<16;i++)bar.append(i<filled?"&a▰":"&7▱");bar.append("&8] &f").append((int)(ratio*100)).append("%");player.sendActionBar(cc(bar.toString()));}}
    private int healBondAllies(LivingEntity healer,double amount){double radius=Math.max(0,getConfig().getDouble("healing.bond-radius",6.0)),shared=Math.max(0,amount*getConfig().getDouble("healing.bond-shared-heal-multiplier",1.0));int healed=0;for(UUID id:players){if(id.equals(healer.getUniqueId())||dead.contains(id)||escaped.contains(id))continue;Player ally=Bukkit.getPlayer(id);if(ally!=null&&healBondTarget(healer,ally,radius,shared)){ally.sendActionBar(cc("&d絆 &7――仲間の治療で回復した"));healed++;}}for(UUID id:playerBots){if(id.equals(healer.getUniqueId())||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity ally))continue;if(healBondTarget(healer,ally,radius,shared))healed++;}return healed;}
    private boolean hasInjuredBondAlly(LivingEntity healer){double radius=Math.max(0,getConfig().getDouble("healing.bond-radius",6.0));for(UUID id:players){Player ally=Bukkit.getPlayer(id);if(!id.equals(healer.getUniqueId())&&ally!=null&&!dead.contains(id)&&!escaped.contains(id)&&isInjuredBondTarget(healer,ally,radius))return true;}for(UUID id:playerBots){if(!id.equals(healer.getUniqueId())&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id) instanceof LivingEntity ally&&isInjuredBondTarget(healer,ally,radius))return true;}return false;}
    private boolean isInjuredBondTarget(LivingEntity healer,LivingEntity ally,double radius){return ally.isValid()&&!ally.isDead()&&ally.getWorld().equals(healer.getWorld())&&ally.getLocation().distanceSquared(healer.getLocation())<=radius*radius&&ally.getHealth()<ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()-.01;}
    private boolean healBondTarget(LivingEntity healer,LivingEntity ally,double radius,double amount){if(!ally.isValid()||ally.isDead()||!ally.getWorld().equals(healer.getWorld())||ally.getLocation().distanceSquared(healer.getLocation())>radius*radius)return false;double max=ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(ally.getHealth()>=max-.01)return false;ally.setHealth(Math.min(max,ally.getHealth()+amount));ally.getWorld().spawnParticle(Particle.HEART,ally.getLocation().add(0,1,0),5,.35,.45,.35,.02);ally.getWorld().playSound(ally.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.55f,1.45f);return true;}
    private void stopHealing(Player player,String message){if(healingTasks.remove(player.getUniqueId())!=null&&message!=null)player.sendActionBar(cc(message));}
    private void consumeInventoryAction(Player player,String wanted){for(ItemStack stack:player.getInventory().getContents()){if(stack==null||!stack.hasItemMeta())continue;String action=stack.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(wanted.equals(action)){consumeOne(stack);return;}}}
    private void consumeOne(ItemStack stack){if(stack.getAmount()<=1)stack.setAmount(0);else stack.setAmount(stack.getAmount()-1);}
    private void startChestOpening(Player player,String chestKey){
        UUID id=player.getUniqueId();
        if(state!=GameState.RUNNING){msg(player,"物資箱は試合中のみ使用できます。");return;}
        if(oni!=null&&id.equals(oni)){msg(player,"&c鬼は物資箱を開けられません。");player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,0.7f);return;}
        // v0.24.2: /og join 済みの非鬼参加者も安全側でぷれいやー陣営として扱う。
        // 通常は start() で players に登録されるが、役割同期がずれた場合でも物資箱だけ無反応にならないようにする。
        if(!players.contains(id)){
            if(participants.contains(id)){players.add(id);getLogger().warning("Recovered missing player-side role for "+player.getName()+" while opening loot chest.");}
            else{msg(player,"&cこの試合の参加者ではありません。ロビーで /og join をしてから次の試合に参加してください。");player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,0.7f);return;}
        }
        if(openedLootChests.getOrDefault(id,Set.of()).contains(chestKey)){openPersonalLoot(player,chestKey);return;}
        ChestOpeningTask current=chestOpeningTasks.get(id);if(current!=null&&current.chestKey().equals(chestKey)){chestOpeningTasks.remove(id);player.sendActionBar(cc("&7チェストの開錠を中断した。"));return;}
        if(repairingHeart.containsKey(id))stopRepair(player,"&7心臓への干渉を中断した。");stopHealing(player,"&7回復を中断した。");
        long now=System.currentTimeMillis(),duration=(long)(getConfig().getDouble("loot-chests.open-seconds",5.0)*1000);
        chestOpeningTasks.put(id,new ChestOpeningTask(chestKey,player.getLocation().clone(),now,now+duration));
        player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,1.1f);
        player.sendTitle("",cc("&6&l物資箱を開錠中"),0,12,4);
        player.sendActionBar(cc("&6チェストを開錠中 &7――約 "+String.format("%.1f",duration/1000.0)+"秒静止 / 動くか攻撃を受けると中断"));
    }
    private void startPracticeChestOpening(Player player,String chestKey){if(state!=GameState.WAITING){msg(player,"開錠練習はロビーでのみ使用できます。");return;}ChestOpeningTask current=chestOpeningTasks.get(player.getUniqueId());if(current!=null&&current.chestKey().equals(chestKey)){chestOpeningTasks.remove(player.getUniqueId());player.sendActionBar(cc("&7開錠練習を中断した。"));return;}long now=System.currentTimeMillis(),duration=(long)(getConfig().getDouble("loot-chests.open-seconds",5.0)*1000);chestOpeningTasks.put(player.getUniqueId(),new ChestOpeningTask(chestKey,player.getLocation().clone(),now,now+duration));player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,.8f,1.1f);player.sendTitle("",cc("&e&l開錠練習"),0,12,4);player.sendActionBar(cc("&6開錠練習中 &7――動かずに待機"));}
    private boolean isPracticeChestKey(String key){Location l=LocationStore.get(getConfig(),"locations.practice-loot-chest");return l!=null&&LocationStore.encode(l).equals(key);}
    private void updateChestOpening(){if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();for(Map.Entry<UUID,ChestOpeningTask> entry:new HashMap<>(chestOpeningTasks).entrySet()){Player player=Bukkit.getPlayer(entry.getKey());ChestOpeningTask task=entry.getValue();Location chest=LocationStore.decode(task.chestKey());double tolerance=getConfig().getDouble("loot-chests.move-tolerance",0.55);boolean practice=isPracticeChestKey(task.chestKey());if(player==null||chest==null||(!lootChestKeys.contains(task.chestKey())&&!practice)||!player.getWorld().equals(task.start().getWorld())||player.getLocation().distanceSquared(task.start())>tolerance*tolerance||player.getLocation().distanceSquared(chest.clone().add(.5,.5,.5))>16){chestOpeningTasks.remove(entry.getKey());if(player!=null)player.sendActionBar(cc("&c離れたためチェストの開錠を中断した。"));continue;}if(now>=task.endsAt()){chestOpeningTasks.remove(entry.getKey());player.playSound(player.getLocation(),Sound.BLOCK_CHEST_OPEN,1,1);player.sendTitle("",cc(practice?"&a&l開錠練習 成功":"&6&l開錠成功"),0,15,5);if(practice){player.sendActionBar(cc("&7もう一度右クリックすると再挑戦できます。"));}else{openedLootChests.computeIfAbsent(entry.getKey(),id->new HashSet<>()).add(task.chestKey());openPersonalLoot(player,task.chestKey());}continue;}double ratio=(now-task.startedAt())/(double)Math.max(1,task.endsAt()-task.startedAt());int filled=Math.max(0,Math.min(16,(int)Math.floor(ratio*16)));StringBuilder bar=new StringBuilder("&6開錠 &8[");for(int i=0;i<16;i++)bar.append(i<filled?"&6▰":"&7▱");bar.append("&8] &f").append((int)(ratio*100)).append("%");player.sendActionBar(cc(bar.toString()));}}
    private void openPersonalLoot(Player player,String chestKey){String inventoryKey=player.getUniqueId()+"|"+chestKey;Inventory inventory=personalLootInventories.computeIfAbsent(inventoryKey,key->createPersonalLoot());player.openInventory(inventory);}
    private Inventory createPersonalLoot(){
        Inventory inventory=Bukkit.createInventory(null,18,cc("&4鬼げぇむ &8- &6物資箱"));
        List<String> items=new ArrayList<>(List.of("item:sprint","item:invisible","item:smoke","item:strike","item:heal"));
        Collections.shuffle(items);
        int min=Math.max(1,getConfig().getInt("loot-chests.min-items",1)),max=Math.max(min,getConfig().getInt("loot-chests.max-items",3)),count=Math.min(items.size(),min+new Random().nextInt(max-min+1));
        List<Integer> itemSlots=new ArrayList<>();for(int i=0;i<9;i++)itemSlots.add(i);Collections.shuffle(itemSlots);
        for(int i=0;i<count;i++)inventory.setItem(itemSlots.get(i),lootItem(items.get(i)));
        List<String> equipment=new ArrayList<>(List.of("equipment:leather_helmet","equipment:leather_chestplate","equipment:leather_leggings","equipment:leather_boots","equipment:shinai","equipment:iron_bat","equipment:flare_gun"));
        Collections.shuffle(equipment);
        int equipmentMin=Math.max(0,getConfig().getInt("loot-chests.min-equipment",1)),equipmentMax=Math.max(equipmentMin,getConfig().getInt("loot-chests.max-equipment",1));
        int equipmentCount=Math.min(equipment.size(),equipmentMin+new Random().nextInt(equipmentMax-equipmentMin+1));
        List<Integer> equipmentSlots=new ArrayList<>();for(int i=9;i<18;i++)equipmentSlots.add(i);Collections.shuffle(equipmentSlots);
        for(int i=0;i<equipmentCount;i++)inventory.setItem(equipmentSlots.get(i),lootEquipment(equipment.get(i)));
        return inventory;
    }
    private ItemStack lootItem(String action){return switch(action){case "item:sprint"->consumable(Material.SUGAR,"&b軽量薬",action,"&7短時間だけ移動速度が上がる");case "item:invisible"->consumable(Material.POTION,"&f薄明薬",action,"&7短時間だけ透明になる");case "item:smoke"->consumable(Material.GRAY_DYE,"&8小型煙玉",action,"&7狭い範囲へ煙幕を張る");case "item:strike"->consumable(Material.BLAZE_POWDER,"&c破鬼符",action,"&7鬼を弱く怯ませる");default->consumable(Material.PAPER,"&a包帯","item:heal","&7時間をかけて少量回復する");};}
    private ItemStack lootEquipment(String action){return switch(action){
        case "equipment:leather_helmet"->equipment(Material.LEATHER_HELMET,"&6革の頭防具",action,"&7軽量な防具。頭に装備できる");
        case "equipment:leather_chestplate"->equipment(Material.LEATHER_CHESTPLATE,"&6革の胴防具",action,"&7軽量な防具。胴に装備できる");
        case "equipment:leather_leggings"->equipment(Material.LEATHER_LEGGINGS,"&6革の脚防具",action,"&7軽量な防具。脚に装備できる");
        case "equipment:leather_boots"->equipment(Material.LEATHER_BOOTS,"&6革の足防具",action,"&7軽量な防具。足に装備できる");
        case "equipment:shinai"->equipment(Material.WOODEN_SWORD,"&e竹刀",action,"&7軽く扱いやすい近接武器","&7鬼への基礎ダメージ: &f"+getConfig().getDouble("equipment.shinai-damage",3.0));
        case "equipment:iron_bat"->equipment(Material.IRON_SWORD,"&7鉄バット",action,"&7重い近接武器","&7鬼への基礎ダメージ: &f"+getConfig().getDouble("equipment.iron-bat-damage",5.0));
        default->equipment(Material.BLAZE_ROD,"&cフレアガン","equipment:flare_gun","&7右クリックで照明弾を発射","&7着弾地点付近の鬼を一時的に発光させる");
    };}
    private void startSkillCheck(Player player,long now){boolean aftermind=aftermindPrimed.remove(player.getUniqueId());long duration=(long)(getConfig().getDouble("skill-check-duration-seconds",2.4)*1000)+(aftermind?800:0);double targetRatio=.45+Math.random()*.30;long target=now+(long)(duration*targetRatio);skillChecks.put(player.getUniqueId(),new SkillCheck(now,target,now+duration));player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,0.8f,1.4f);player.sendTitle("",cc(aftermind?"&b残心 &7――判定位置 "+(int)(targetRatio*100)+"%":"&e&lスキルチェック！ &fSHIFT"),0,15,0);}
    private void showSkillCheck(Player player,SkillCheck check,long now){
        int length=15,cursor=Math.min(length-1,(int)(((now-check.startedAt())/(double)(check.endsAt()-check.startedAt()))*length)),great=Math.min(length-1,(int)(((check.targetAt()-check.startedAt())/(double)(check.endsAt()-check.startedAt()))*length));
        boolean hard=hasPassive(player.getUniqueId(),PassiveSkill.DOUBLE_STAKES);
        StringBuilder bar=new StringBuilder("&8[");
        for(int i=0;i<length;i++){
            if(i==cursor)bar.append(i==great?"&6◆":"&f◆");
            else if(i==great)bar.append("&6▰");
            else if(i==great-1||i==great-2)bar.append(hard?"&c▰":"&a▰");
            else bar.append("&7▱");
        }
        bar.append("&8] &eSHIFT");if(hard)bar.append(" &4GREATのみ");player.sendActionBar(cc(bar.toString()));
    }
    private void resolveSkillCheck(Player player){
        SkillCheck check=skillChecks.get(player.getUniqueId());if(check==null)return;long now=System.currentTimeMillis();
        double focus=hasPassive(player.getUniqueId(),PassiveSkill.FOCUS)?0.12:0.0,interference=hasOniPassive(OniPassiveSkill.INTERFERENCE)?Math.max(0.1,getConfig().getDouble("oni-passive-skills.interference-window-multiplier",0.70)):1.0;
        long successWindow=(long)((getConfig().getDouble("skill-check-success-window-seconds",0.40)+focus)*interference*1000),greatWindow=(long)((getConfig().getDouble("skill-check-great-window-seconds",0.12)+(focus*.25))*interference*1000);
        long greatHalf=Math.max(20,greatWindow/2),greatStart=check.targetAt()-greatHalf,greatEnd=check.targetAt()+greatHalf;
        long normalStart=greatStart-(successWindow*2);
        Location heart=LocationStore.decode(repairingHeart.get(player.getUniqueId()));if(heart==null)return;
        if(now>=greatStart&&now<=greatEnd){skillChecks.remove(player.getUniqueId());scheduleNextSkillCheck(player.getUniqueId(),now);if(hasPassive(player.getUniqueId(),PassiveSkill.AFTERMIND))aftermindPrimed.add(player.getUniqueId());advanceHeart(player,heart.getBlock(),doubleStakesGreatBonus(player));player.sendTitle("",cc("&6&lGREAT!"),0,18,6);player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.8f,1.65f);}
        else if(now>=normalStart&&now<greatStart){if(hasPassive(player.getUniqueId(),PassiveSkill.DOUBLE_STAKES)){failSkillCheck(player,heart,LocationStore.encode(heart),"GREAT以外は失敗");}else{skillChecks.remove(player.getUniqueId());scheduleNextSkillCheck(player.getUniqueId(),now);if(hasOniPassive(OniPassiveSkill.INTERFERENCE)){regressHeart(heart.getBlock(),Math.max(0,getConfig().getInt("oni-passive-skills.interference-normal-regression",2)));player.sendTitle("",cc("&5&l妨害 &c進行後退"),0,18,6);player.playSound(player.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,0.8f,0.75f);}else{advanceHeart(player,heart.getBlock(),Math.max(0,getConfig().getInt("skill-check-success-bonus",3)));player.sendTitle("",cc("&a&l成功"),0,15,5);player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.8f,1.5f);}}}
        else failSkillCheck(player,heart,LocationStore.encode(heart),"タイミング失敗");
    }
    private void failSkillCheck(Player player,Location heart,String heartKey,String reason){skillChecks.remove(player.getUniqueId());doubleStakesGreatStreak.remove(player.getUniqueId());long now=System.currentTimeMillis();if(hasPassive(player.getUniqueId(),PassiveSkill.PRACTICED))repairStartedAt.put(player.getUniqueId(),now);scheduleNextSkillCheck(player.getUniqueId(),now);int max=getConfig().getInt("heart-max-health",100),penalty=Math.max(0,getConfig().getInt("skill-check-failure-penalty",8));heartHp.computeIfPresent(heartKey,(k,hp)->Math.min(max,hp+penalty));updateHeartCracks(heart.getBlock(),heartHp.getOrDefault(heartKey,max));player.sendTitle(cc("&4&l失敗"),cc("&c"+reason),0,25,8);heart.getWorld().playSound(heart,Sound.ENTITY_GENERIC_EXPLODE,1.2f,0.7f);heart.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,heart.clone().add(.5,.5,.5),2);if(state==GameState.RUNNING)forceHeartAlert(heart);}
    private void regressHeart(Block block,int amount){String key=LocationStore.encode(block.getLocation());int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+amount));updateHeartCracks(block,heartHp.getOrDefault(key,max));block.getWorld().spawnParticle(Particle.SMOKE_NORMAL,block.getLocation().add(.5,.7,.5),10,.3,.3,.3,.02);}
    private void advanceHeart(Player player,Block block,double amount){String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;applyScaledHeartDamage(player.getUniqueId(),key,amount);int hp=heartHp.getOrDefault(key,0);updateHeartCracks(block,hp);block.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,block.getLocation().add(.5,.6,.5),2,0.25,0.25,0.25,0.02);int max=getConfig().getInt("heart-max-health",100),percent=(int)Math.round((1-hp/(double)Math.max(1,max))*100);boolean practice=isPracticeHeartKey(key)&&state==GameState.WAITING;if(!skillChecks.containsKey(player.getUniqueId()))player.sendActionBar(cc((practice?"&d練習心臓 損傷 ":"&4心臓損傷 &c")+percent+"% &7――作業中"));if(!practice)notifyHeartAttack(block.getLocation(),key,System.currentTimeMillis());if(hp<=0){if(practice)completePracticeHeart(player,block,key);else destroyHeart(block);}}
    private void completePracticeHeart(Player player,Block block,String key){for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player p=Bukkit.getPlayer(id);if(p!=null)stopRepair(p,null);}for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),0f);player.sendTitle(cc("&d&l心臓破壊 成功"),cc("&7練習完了"),0,30,10);block.getWorld().playSound(block.getLocation(),Sound.ENTITY_WITHER_DEATH,.55f,.8f);heartHp.put(key,getConfig().getInt("heart-max-health",100));Bukkit.getScheduler().runTaskLater(this,()->{Material m=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN"));if(m==null)m=Material.CRYING_OBSIDIAN;block.setType(m);updateHeartCracks(block,getConfig().getInt("heart-max-health",100));},20L);}
    private void applyExpandedHeartBreakPassives(Location heart){
        LivingEntity hunter=getOniEntity();double radius=Math.max(1,getConfig().getDouble("oni-passive-skills.heart-break-effect-radius",8.0));
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(heart.getWorld())||q.getLocation().distanceSquared(heart)>radius*radius)continue;if(hasOniPassive(OniPassiveSkill.CURSED_VEIN)){q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,Math.max(20,getConfig().getInt("oni-passive-skills.cursed-vein-ticks",80)),0,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,40,0,false,true));}if(hasOniPassive(OniPassiveSkill.BACKFLOW)){Vector v=q.getLocation().toVector().subtract(heart.toVector()).setY(.25);if(v.lengthSquared()<.01)v=new Vector(0,.3,0);else v.normalize().multiply(1.0).setY(.35);q.setVelocity(v);}}
        if(hunter!=null&&hasOniPassive(OniPassiveSkill.HOMING)&&hunter.getWorld().equals(heart.getWorld())&&hunter.getLocation().distanceSquared(heart)>400)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.homing-speed-ticks",80)),1,false,true));
    }
    private void destroyHeart(Block block){String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player p=Bukkit.getPlayer(id);if(p!=null)stopRepair(p,null);}heartHp.remove(key);for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),0f);block.setType(Material.AIR);brokenHearts++;applyExpandedHeartBreakPassives(block.getLocation());for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q!=null)trackHeart(q);}all("&c鬼の心臓が破壊された！ &f("+brokenHearts+"/"+totalHearts+")");block.getWorld().playSound(block.getLocation(),Sound.ENTITY_WITHER_DEATH,0.7f,0.55f);weakenOni();if(brokenHearts==totalHearts){
            finalPhase=true;
            secondsLeft=Math.max(1,getConfig().getInt("final-crimson.remaining-seconds",180));
            all("&4&l鬼の不死性が消滅した――最終局面！");
            all("&c鬼を討伐するか、&b脱出するか。 &f残された時間は &e3分&f。");
            startFinalCrimson(block.getWorld());
            if(oniType==OniType.JAKUTSUKI)grantJakutsukiFinalWeapons();
            startFinalPhaseBgm();
        }}
    private void startFinalCrimson(World world){
        if(world==null)return;
        if(crimsonTicker!=null){crimsonTicker.cancel();crimsonTicker=null;}
        if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}
        boolean enabled=getConfig().getBoolean("final-crimson.enabled",true);
        long playerTime=Math.floorMod(getConfig().getLong("final-crimson.player-time",12000L),24000L);
        for(UUID id:participants){
            Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world))continue;
            if(enabled)p.setPlayerTime(playerTime,false);
            if(enabled&&getConfig().getBoolean("final-crimson.blood-sky.force-downfall",true))p.setPlayerWeather(WeatherType.DOWNFALL);
            p.sendTitle(cc("&4&l最 終 局 面"),cc("&c鬼を討伐するか、&b脱出するか &7―― &e残り3:00"),10,70,20);
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,.8f,.65f);
        }
        world.playSound(world.getSpawnLocation(),Sound.ENTITY_WITHER_SPAWN,.65f,.55f);
        if(enabled&&getConfig().getBoolean("final-crimson.blood-sky.enabled",true)){
            long delay=Math.max(0,getConfig().getLong("final-crimson.blood-sky.delay-ticks",100));
            Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)showBloodSkyOverlay(world);},delay);
        }
        if(!enabled||!getConfig().getBoolean("final-crimson.red-haze-particles",true))return;
        int count=Math.max(1,getConfig().getInt("final-crimson.red-haze-count",18));
        double height=Math.max(4.0,getConfig().getDouble("final-crimson.red-haze-height",12.0));
        Particle.DustOptions dust=new Particle.DustOptions(Color.fromRGB(180,0,0),2.0f);
        crimsonTicker=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(state!=GameState.RUNNING||!finalPhase){if(crimsonTicker!=null)crimsonTicker.cancel();crimsonTicker=null;return;}
            for(UUID id:participants){
                Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world)||p.getGameMode()==GameMode.SPECTATOR)continue;
                Location base=p.getLocation().clone().add(0,height,0);
                p.spawnParticle(Particle.REDSTONE,base,count,14.0,4.0,14.0,0,dust);
                p.spawnParticle(Particle.ASH,base.clone().add(0,-3,0),Math.max(4,count/2),10.0,5.0,10.0,.01);
            }
        },1L,10L);
    }
    private void startFinalEffectTest(Player p){
        clearFinalEffectTest();finalEffectTestPlayer=p.getUniqueId();
        long playerTime=Math.floorMod(getConfig().getLong("final-crimson.player-time",12000L),24000L);p.setPlayerTime(playerTime,false);
        if(getConfig().getBoolean("final-crimson.blood-sky.force-downfall",true))p.setPlayerWeather(WeatherType.DOWNFALL);
        p.sendTitle(cc("&4&l最 終 局 面"),cc("&c演出テスト &7―― &f/og finaltest clear で解除"),5,50,10);p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,.8f,.65f);
        String fontRaw=getConfig().getString("final-crimson.blood-sky.font","onigame:final_sky"),glyph=getConfig().getString("final-crimson.blood-sky.glyph","\uE101");if(glyph==null||glyph.isBlank())glyph="\uE101";
        Key font;try{font=Key.key(fontRaw==null?"onigame:final_sky":fontRaw);}catch(IllegalArgumentException ex){font=Key.key("onigame","final_sky");}
        Title overlay=Title.title(Component.text(glyph).font(font),Component.empty(),Title.Times.times(Duration.ofMillis(200),Duration.ofMillis(600000),Duration.ofMillis(200)));
        if(getConfig().getBoolean("final-crimson.blood-sky.enabled",false))Bukkit.getScheduler().runTaskLater(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q!=null)q.showTitle(overlay);},65L);
        p.stopSound("onigame:final_phase",SoundCategory.RECORDS);p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);
        Bukkit.getScheduler().runTaskLater(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q!=null)q.playSound(q.getLocation(),"onigame:chase_tatari",SoundCategory.RECORDS,(float)getConfig().getDouble("final-phase-bgm.volume",.22),(float)getConfig().getDouble("final-phase-bgm.pitch",1.0));},4L);
        finalEffectTestTicker=Bukkit.getScheduler().runTaskTimer(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q==null){clearFinalEffectTest();return;}int count=Math.max(1,getConfig().getInt("final-crimson.red-haze-count",42));double h=Math.max(4,getConfig().getDouble("final-crimson.red-haze-height",16));q.spawnParticle(Particle.REDSTONE,q.getLocation().add(0,h*.55,0),count,12,h*.55,12,0,new Particle.DustOptions(Color.fromRGB(190,0,0),1.8f));q.spawnParticle(Particle.ASH,q.getLocation().add(0,h*.45,0),Math.max(5,count/2),10,h*.45,10,.01);},0L,10L);
    }
    private void clearFinalEffectTest(){
        if(finalEffectTestTicker!=null){finalEffectTestTicker.cancel();finalEffectTestTicker=null;}
        if(finalEffectTestPlayer!=null){Player p=Bukkit.getPlayer(finalEffectTestPlayer);if(p!=null){p.resetPlayerTime();p.resetPlayerWeather();p.sendTitle("","",0,1,0);p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);p.stopSound("onigame:final_phase",SoundCategory.RECORDS);}}finalEffectTestPlayer=null;
    }

    private void showBloodSkyOverlay(World world){
        if(world==null||!getConfig().getBoolean("final-crimson.blood-sky.enabled",true))return;
        String fontRaw=getConfig().getString("final-crimson.blood-sky.font","onigame:final_sky");
        String glyph=getConfig().getString("final-crimson.blood-sky.glyph","\uE101");
        if(glyph==null||glyph.isBlank())glyph="\uE101";
        Key font;try{font=Key.key(fontRaw==null?"onigame:final_sky":fontRaw);}catch(IllegalArgumentException ex){font=Key.key("onigame","final_sky");}
        long fadeIn=Math.max(0,getConfig().getLong("final-crimson.blood-sky.fade-in-ticks",12))*50L;
        long stay=Math.max(20,getConfig().getLong("final-crimson.blood-sky.stay-ticks",3700))*50L;
        long fadeOut=Math.max(0,getConfig().getLong("final-crimson.blood-sky.fade-out-ticks",10))*50L;
        Title overlay=Title.title(Component.text(glyph).font(font),Component.empty(),Title.Times.times(Duration.ofMillis(fadeIn),Duration.ofMillis(stay),Duration.ofMillis(fadeOut)));
        for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world)p.showTitle(overlay);}
    }
    private void resetFinalCrimsonVisuals(){
        for(UUID id:participants){
            Player p=Bukkit.getPlayer(id);
            if(p!=null){p.resetPlayerTime();p.resetPlayerWeather();p.sendTitle("","",0,1,0);}
        }
    }
    private void startLongNightDawn(World world){
        if(world==null||!getConfig().getBoolean("final-dawn.enabled",true))return;
        if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}
        long start=Math.floorMod(getConfig().getLong("final-dawn.start-time",18000L),24000L);
        long endRaw=getConfig().getLong("final-dawn.end-time",1000L);
        long end=endRaw;
        while(end<=start)end+=24000L;
        int durationTicks=Math.max(20,getConfig().getInt("final-dawn.duration-seconds",35)*20);
        int stepTicks=Math.max(1,getConfig().getInt("final-dawn.step-ticks",2));
        world.setStorm(false);world.setThundering(false);world.setTime(start);
        for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world){p.sendTitle(cc("&f&l―― 永い夜が明ける ――"),cc("&7東の空が、ゆっくりと白み始めた。"),10,60,20);p.playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.55f,.70f);}}
        all("&f&l―― 永い夜が明ける。");
        final long begin=start, finish=end;final int[] elapsed={0};final BukkitTask[] ref=new BukkitTask[1];
        ref[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(state!=GameState.RUNNING||world==null){if(ref[0]!=null)ref[0].cancel();dawnTicker=null;return;}
            elapsed[0]+=stepTicks;double progress=Math.min(1.0,elapsed[0]/(double)durationTicks);
            double eased=progress*progress*(3.0-2.0*progress);
            long absolute=begin+Math.round((finish-begin)*eased);world.setTime(Math.floorMod(absolute,24000L));
            if(progress>=1.0){
                world.setTime(Math.floorMod(finish,24000L));
                if(ref[0]!=null)ref[0].cancel();dawnTicker=null;
                applyDawnOniDebuff();
                for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world)p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.45f,1.25f);}
            }
        },stepTicks,stepTicks);
        dawnTicker=ref[0];
    }
    private void applyDawnOniDebuff(){
        if(!getConfig().getBoolean("final-dawn.oni-debuff.enabled",true))return;
        LivingEntity hunter=getOniEntity();if(hunter==null||!hunter.isValid()||hunter.isDead())return;
        int duration=Math.max(20,getConfig().getInt("final-dawn.oni-debuff.duration-seconds",9999)*20);
        int weakness=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.weakness-amplifier",2));
        int slowness=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.slowness-amplifier",1));
        int fatigue=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.mining-fatigue-amplifier",1));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,duration,weakness,false,true));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,duration,slowness,false,true));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,duration,fatigue,false,true));
        if(getConfig().getBoolean("final-dawn.oni-debuff.glowing",true))hunter.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,duration,0,false,false));
        double maxLoss=Math.max(0.0,getConfig().getDouble("final-dawn.oni-debuff.max-health-loss",20.0));
        if(maxLoss>0&&hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null){
            double currentMax=hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double minMax=Math.max(1.0,getConfig().getDouble("final-dawn.oni-debuff.minimum-max-health",20.0));
            double reduced=Math.max(minMax,currentMax-maxLoss);
            hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(reduced);
            if(hunter.getHealth()>reduced)hunter.setHealth(reduced);
        }
        all("&6&l朝日が昇る――鬼の力が焼かれていく！");
        if(hunter instanceof Player p){
            p.sendTitle(cc("&6&l朝日の呪い"),cc("&c夜の加護が失われ、力が大きく弱まった"),5,60,15);
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_HURT,1.0f,1.35f);
        }else{
            hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WITHER_HURT,1.0f,1.35f);
        }
        hunter.getWorld().spawnParticle(Particle.FLAME,hunter.getLocation().add(0,1,0),55,.7,1.0,.7,.04);
        hunter.getWorld().spawnParticle(Particle.END_ROD,hunter.getLocation().add(0,1,0),35,.7,1.0,.7,.05);
    }
    private ItemStack jakutsukiSnakeSlashItem(){
        ItemStack i=item(Material.NETHERITE_SWORD,"&f&l蛇斬","jakutsuki_snake_slash");
        ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setLore(List.of(cc("&7蛇窟姫の蛇を一撃で斬り伏せる退魔刀。"),cc("&7最終盤では蛇窟姫への攻撃にも高い威力を発揮する。"),cc("&d対蛇: 一撃撃破 &7/ &c対蛇窟姫: 高威力")));i.setItemMeta(m);return i;
    }
    private boolean isJakutsukiSnakeSlash(ItemStack stack){return "jakutsuki_snake_slash".equals(actionOf(stack));}
    private void grantJakutsukiFinalWeapons(){
        all("&f&l――蛇窟が崩れ、退魔刀『蛇斬』が顕現した。");
        for(UUID id:players){
            if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null)continue;
            ItemStack blade=jakutsukiSnakeSlashItem();HashMap<Integer,ItemStack> overflow=p.getInventory().addItem(blade);for(ItemStack left:overflow.values())p.getWorld().dropItemNaturally(p.getLocation(),left);
            p.sendTitle(cc("&f&l蛇 斬"),cc("&d反攻の刻――蛇を断ち、蛇窟姫を討て"),5,55,12);p.playSound(p.getLocation(),Sound.ITEM_TRIDENT_RETURN,1.0f,.75f);p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation().add(0,1,0),35,.65,.9,.65,.06);
        }
        for(UUID id:playerBots){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.isValid()||bot.isDead())continue;
            bot.getEquipment().setItemInMainHand(jakutsukiSnakeSlashItem());bot.getEquipment().setItemInMainHandDropChance(0);bot.getWorld().spawnParticle(Particle.END_ROD,bot.getLocation().add(0,1,0),25,.55,.75,.55,.05);
        }
        LivingEntity hunter=getOniEntity();if(hunter!=null){hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WITHER_DEATH,.8f,.55f);hunter.getWorld().spawnParticle(Particle.SQUID_INK,hunter.getLocation().add(0,1,0),85,1.2,1.2,1.2,.08);}
    }
    private void updateHeartCracks(Block block,int hp){int max=getConfig().getInt("heart-max-health",100);float progress=Math.max(0f,Math.min(1f,1f-hp/(float)Math.max(1,max)));for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),progress);}
    private void notifyHeartAttack(Location heart,String heartKey,long now){if(getConfig().getBoolean("bot.hunter-ai.enabled",true)&&oni!=null){Player hunter=Bukkit.getPlayer(oni);if(hunter!=null){int ticks=Math.max(0,getConfig().getInt("bot.hunter-ai.human-oni-heart-alert-speed-ticks",50));int amp=Math.max(0,getConfig().getInt("bot.hunter-ai.human-oni-heart-alert-speed-amplifier",0));if(ticks>0)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amp,false,true));}}double seconds=getConfig().getDouble("heart-alert-cooldown-seconds",5.0);if(hasOniPassive(OniPassiveSkill.GUARDIAN))seconds*=Math.max(0.1,getConfig().getDouble("oni-passive-skills.guardian-alert-interval-multiplier",0.50));long cooldown=(long)(seconds*1000L);if(now-heartAlertAt.getOrDefault(heartKey,0L)<cooldown)return;heartAlertAt.put(heartKey,now);LivingEntity hunter=getOniEntity();if(hunter==null)return;if(hunter instanceof Player p)msg(p,"&4心臓が傷つけられている…… &c"+directionFrom(p.getLocation(),heart));if(hasOniPassive(OniPassiveSkill.HOMING)&&hunter.getWorld().equals(heart.getWorld())&&hunter.getLocation().distanceSquared(heart)>400)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.homing-speed-ticks",80)),1,false,true));if(hasOniPassive(OniPassiveSkill.PULSE)){int ticks=Math.max(20,getConfig().getInt("oni-passive-skills.pulse-speed-ticks",70));int amp=Math.max(0,getConfig().getInt("oni-passive-skills.pulse-speed-amplifier",0));hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amp,false,true));if(hunter instanceof Player p)p.sendActionBar(cc("&d脈動 &7――心臓の悲鳴を辿り加速"));}hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,1.0f,0.55f);}
    private void forceHeartAlert(Location heart){LivingEntity hunter=getOniEntity();if(hunter==null)return;if(hunter instanceof Player p)msg(p,"&4作業失敗の大きな音！ &c"+directionFrom(p.getLocation(),heart));hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,0.8f,0.7f);}
    private String directionFrom(Location from,Location to){double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();String ns=Math.abs(dz)<2?"":(dz<0?"北":"南"),ew=Math.abs(dx)<2?"":(dx>0?"東":"西");String result=ns+ew;return result.isEmpty()?"すぐ近く":result+"方向";}
    private String chaseBgmSound(){return getConfig().getString("chase-bgm.sound","onigame:chase_tatari");}
    private void cancelNormalBgmFade(UUID id){List<BukkitTask> tasks=normalBgmFadeTasks.remove(id);if(tasks!=null)for(BukkitTask t:tasks)if(t!=null)t.cancel();}
    private void enterChaseBgm(Player p){
        if(p==null||!getConfig().getBoolean("chase-bgm.enabled",true)||finalPhase)return;
        UUID id=p.getUniqueId();
        BukkitTask release=chaseReleaseTasks.remove(id);if(release!=null)release.cancel();
        cancelNormalBgmFade(id);
        String normal=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");
        p.stopSound(normal,SoundCategory.RECORDS);
        if(chaseBgmPlayers.add(id)){
            String sound=chaseBgmSound();
            float volume=(float)getConfig().getDouble("chase-bgm.volume",0.9),pitch=(float)getConfig().getDouble("chase-bgm.pitch",1.0);
            p.stopSound(sound,SoundCategory.RECORDS);
            p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);
        }
    }
    private void scheduleLeaveChaseBgm(Player p){
        if(p==null)return;UUID id=p.getUniqueId();
        BukkitTask old=chaseReleaseTasks.remove(id);if(old!=null)old.cancel();
        long delay=Math.max(0,getConfig().getLong("chase-bgm.release-delay-ticks",50));
        BukkitTask task=Bukkit.getScheduler().runTaskLater(this,()->{
            chaseReleaseTasks.remove(id);
            if(state!=GameState.RUNNING||finalPhase||chased.contains(id))return;
            leaveChaseBgmNow(p);
        },delay);
        chaseReleaseTasks.put(id,task);
    }
    private void leaveChaseBgmNow(Player p){
        if(p==null)return;UUID id=p.getUniqueId();
        if(!chaseBgmPlayers.remove(id))return;
        p.stopSound(chaseBgmSound(),SoundCategory.RECORDS);
        resumeNormalBgmSoft(p);
    }
    private void resumeNormalBgmSoft(Player p){
        if(p==null||state!=GameState.RUNNING||finalPhase)return;
        UUID id=p.getUniqueId();cancelNormalBgmFade(id);
        long startDelay=Math.max(0,getConfig().getLong("chase-bgm.resume-delay-ticks",12));
        int steps=Math.max(1,getConfig().getInt("chase-bgm.resume-fade-steps",4));
        long stepTicks=Math.max(1,getConfig().getLong("chase-bgm.resume-fade-step-ticks",10));
        float normalVolume=(float)getConfig().getDouble("game-start-bgm.volume",0.8);
        float startVolume=(float)Math.max(0.01,Math.min(normalVolume,getConfig().getDouble("chase-bgm.resume-volume",0.28)));
        String normal=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");
        float pitch=(float)getConfig().getDouble("game-start-bgm.pitch",1.0);
        List<BukkitTask> tasks=new ArrayList<>();
        for(int i=0;i<steps;i++){
            final int idx=i;
            BukkitTask t=Bukkit.getScheduler().runTaskLater(this,()->{
                if(state!=GameState.RUNNING||finalPhase||chased.contains(id)||chaseBgmPlayers.contains(id))return;
                float vol=startVolume+(normalVolume-startVolume)*((idx+1)/(float)steps);
                p.stopSound(normal,SoundCategory.RECORDS);
                p.playSound(p.getLocation(),normal,SoundCategory.RECORDS,vol,pitch);
            },startDelay+idx*stepTicks);
            tasks.add(t);
        }
        normalBgmFadeTasks.put(id,tasks);
    }
    private void stopAllChaseBgm(){
        for(BukkitTask t:chaseReleaseTasks.values())if(t!=null)t.cancel();chaseReleaseTasks.clear();
        for(List<BukkitTask> list:normalBgmFadeTasks.values())for(BukkitTask t:list)if(t!=null)t.cancel();normalBgmFadeTasks.clear();
        String sound=chaseBgmSound();
        for(UUID id:new HashSet<>(chaseBgmPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)p.stopSound(sound,SoundCategory.RECORDS);}
        chaseBgmPlayers.clear();
    }
    private void startGameStartBgm(){if(!getConfig().getBoolean("game-start-bgm.enabled",true))return;stopGameStartBgm();activeGameStartBgmSound=getConfig().getString("game-start-bgm.sound","onigame:game_start");playGameStartBgmToAll();long replay=Math.max(0,getConfig().getLong("game-start-bgm.replay-seconds",166));if(replay>0)gameStartBgmTicker=Bukkit.getScheduler().runTaskTimer(this,this::playGameStartBgmToAll,replay*20L,replay*20L);}
    private void playGameStartBgmToAll(){if(state!=GameState.RUNNING||finalPhase)return;for(UUID id:participants){if(chased.contains(id)||chaseBgmPlayers.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null)playGameStartBgm(p);}}
    private void playGameStartBgm(Player p){String sound=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");float volume=(float)getConfig().getDouble("game-start-bgm.volume",0.8),pitch=(float)getConfig().getDouble("game-start-bgm.pitch",1.0);p.stopSound(sound,SoundCategory.RECORDS);p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);}
    private void stopGameStartBgm(){if(gameStartBgmTicker!=null){gameStartBgmTicker.cancel();gameStartBgmTicker=null;}String sound=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");for(Player p:Bukkit.getOnlinePlayers())p.stopSound(sound,SoundCategory.RECORDS);activeGameStartBgmSound=null;}
    private String finalPhaseBgmSection(){return oniType==OniType.JAKUTSUKI?"jakutsuki-final-phase-bgm":"final-phase-bgm";}
    private void startFinalPhaseBgm(){
        stopAllChaseBgm();stopGameStartBgm();
        String section=finalPhaseBgmSection();
        finalPhase=true;
        if(!getConfig().getBoolean(section+".enabled",true))return;
        if(bgmTicker!=null){bgmTicker.cancel();bgmTicker=null;}
        String configured=getConfig().getString(section+".sound","onigame:chase_tatari");
        activeBgmSound=(configured==null||configured.isBlank())?"onigame:chase_tatari":configured;
        // 通常追跡曲と祟りを両方いったん停止し、最終フェーズでは祟りを先頭から確実に再生する。
        for(Player p:Bukkit.getOnlinePlayers()){
            p.stopSound(getConfig().getString("chase-bgm.sound","onigame:final_phase"),SoundCategory.RECORDS);
            p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);
        }
        Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)playFinalPhaseBgmToAll();},4L);
        Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)playFinalPhaseBgmToAll();},24L);
        long replay=Math.max(0,getConfig().getLong(section+".replay-seconds",271));
        if(replay>0)bgmTicker=Bukkit.getScheduler().runTaskTimer(this,this::playFinalPhaseBgmToAll,replay*20L,replay*20L);
    }
    private void playFinalPhaseBgmToAll(){if(state!=GameState.RUNNING)return;for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null)playConfiguredBgm(p);}}
    private void playConfiguredBgm(Player p){String section=finalPhaseBgmSection();String sound=activeBgmSound!=null?activeBgmSound:getConfig().getString(section+".sound","onigame:chase_tatari");if(sound==null||sound.isBlank())sound="onigame:chase_tatari";float volume=(float)getConfig().getDouble(section+".volume",0.8),pitch=(float)getConfig().getDouble(section+".pitch",1.0);p.stopSound(sound,SoundCategory.RECORDS);p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);}
    private void stopFinalPhaseBgm(){if(bgmTicker!=null){bgmTicker.cancel();bgmTicker=null;}String sound=activeBgmSound!=null?activeBgmSound:getConfig().getString(finalPhaseBgmSection()+".sound",oniType==OniType.JAKUTSUKI?"onigame:jakutsuki_final_phase":"onigame:final_phase");for(Player p:Bukkit.getOnlinePlayers())p.stopSound(sound,SoundCategory.RECORDS);activeBgmSound=null;}
    @EventHandler public void onEntityPlace(EntityPlaceEvent e){
        if(!(e.getEntity() instanceof ArmorStand stand)||e.getPlayer()==null)return;
        ItemStack held=e.getHand()==EquipmentSlot.HAND?e.getPlayer().getInventory().getItemInMainHand():e.getPlayer().getInventory().getItemInOffHand();
        if(!held.hasItemMeta())return;String action=held.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(!"heart_marker".equals(action))return;
        configureMarker(stand);msg(e.getPlayer(),"心臓地点マーカーを設置しました。ゲーム開始時、この位置に心臓が生成されます。");
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent e){
        if(personalLootInventories.containsValue(e.getView().getTopInventory())){if(e.getClickedInventory()==null)return;if(e.getClickedInventory().equals(e.getView().getTopInventory())&&isConsumableItem(e.getCurrentItem())&&e.getWhoClicked() instanceof Player player&&carriedConsumables(player)>=getConfig().getInt("loot-chests.max-carried-items",3)){e.setCancelled(true);msg(player,"使い切りアイテムは合計3個まで持てます。");return;}if(e.getClickedInventory().equals(e.getView().getBottomInventory())&&e.isShiftClick()){e.setCancelled(true);return;}if(e.getClickedInventory().equals(e.getView().getTopInventory())&&((e.getCursor()!=null&&!e.getCursor().getType().isAir())||e.getClick().isKeyboardClick()))e.setCancelled(true);return;}
        if(e.getInventory().getHolder() instanceof MenuHolder holder){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null||!e.getCurrentItem().hasItemMeta())return;
            String action=e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
            if(action==null)return;
            if(action.equals("menu_page:presets")){openPresetMenu(p);return;}
            if(action.equals("menu_page:skills")){openSkillMenu(p);return;}
            if(action.startsWith("menu_page:skill:")){openSkillPage(p,Integer.parseInt(action.substring("menu_page:skill:".length())));return;}
            if(action.startsWith("menu_page:passive:")){openPassivePage(p,Integer.parseInt(action.substring("menu_page:passive:".length())));return;}
            if(action.startsWith("menu_oni_passive_page:")){openOniPassivePage(p,Integer.parseInt(action.substring("menu_oni_passive_page:".length())));return;}
            if(action.equals("menu_oni_back")){openOniMenu(p);return;}
            if(holder.type.equals("presets")&&action.startsWith("menu_build:")){applyTacticalPreset(p,action.substring("menu_build:".length()));openPresetMenu(p);return;}
            if(holder.type.startsWith("skills")&&action.startsWith("menu_skill:")){
                PlayerSkill skill=PlayerSkill.valueOf(action.substring("menu_skill:".length()));
                selectedSkill.put(p.getUniqueId(),skill);
                selectedPresetNames.remove(p.getUniqueId());
                msg(p,"メインスキルを &b"+skill.display+" &fに設定しました。");
                openSkillPage(p,menuPageFromType(holder.type));
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:IGAMI_KYOYA")){
                selectedPresetNames.put(p.getUniqueId(),"IGAMI_KYOYA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.ONI_STRIKE);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.ATTACK_BOOST);
                selected.add(PassiveSkill.DURABILITY_BOOST);
                msg(p,"プリセット &6伊神京也（イガミキョウヤ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZANAMI_MISAKI")){
                selectedPresetNames.put(p.getUniqueId(),"AZANAMI_MISAKI");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SPRINT);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.LIGHT_FOOTED);
                selected.add(PassiveSkill.DEEP_BREATH);
                msg(p,"プリセット &b字那美咲（アザナミサキ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:ARIKAWA_FUUKA")){
                selectedPresetNames.put(p.getUniqueId(),"ARIKAWA_FUUKA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SMOKE);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.COWARDICE);
                selected.add(PassiveSkill.EXORCISM);
                msg(p,"プリセット &d有川風香（アリカワフウカ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZANAMI_REN")){
                selectedPresetNames.put(p.getUniqueId(),"AZANAMI_REN");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.OBSESSION);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.LIGHT_FOOTED);
                selected.add(PassiveSkill.DURABILITY_BOOST);
                msg(p,"プリセット &3字那美蓮（アザナミレン） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:MEDIC")){
                selectedPresetNames.put(p.getUniqueId(),"MEDIC");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.HEAL);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.FOCUS);
                selected.add(PassiveSkill.BOND);
                msg(p,"プリセット &a佐倉夏海（サクラナツミ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AKASAKA_HIIRO")){
                selectedPresetNames.put(p.getUniqueId(),"AKASAKA_HIIRO");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SAFE_LANDING);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.NINJA_BLOOD);
                selected.add(PassiveSkill.LEAP);
                msg(p,"EXプリセット &c赤坂陽彩（アカサカヒイロ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:KAGAYA_RION")){
                selectedPresetNames.put(p.getUniqueId(),"KAGAYA_RION");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.BLINK);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.QUICK_TURN);
                selected.add(PassiveSkill.CORNERED_RAT);
                msg(p,"EXプリセット &a加賀谷凛音（カガヤリオン） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AMANAI_IONA")){
                selectedPresetNames.put(p.getUniqueId(),"AMANAI_IONA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.ECHO);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.SILENT_BREATH);
                selected.add(PassiveSkill.DECOY);
                msg(p,"EXプリセット &9天内伊御奈（アマナイイオナ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZAKUJI_HIRO")){
                if(isAzakujiLocked()){msg(p,"&6字九字ひろ &fは現在ロックされています。");openPresetMenu(p);return;}
                selectedPresetNames.put(p.getUniqueId(),"AZAKUJI_HIRO");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.BUGEI);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.ATTACK_BOOST);
                selected.add(PassiveSkill.DIVINE_TECHNIQUE);
                msg(p,"EXプリセット &6字九字ひろ（アザクジヒロ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.startsWith("passives")&&action.startsWith("menu_passive:")){
                PassiveSkill passive=PassiveSkill.valueOf(action.substring("menu_passive:".length()));
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selectedPresetNames.remove(p.getUniqueId());
                if(selected.remove(passive))msg(p,"パッシブ &a"+passive.display+" &fを解除しました。");
                else if(selected.size()>=2)msg(p,"パッシブスキルは2個までです。選択済みを解除してください。");
                else{selected.add(passive);msg(p,"パッシブ &a"+passive.display+" &fを設定しました。");}
                openPassivePage(p,menuPageFromType(holder.type));
            }else if(holder.type.equals("oni")&&action.startsWith("menu_oni:")){
                OniType requested=OniType.valueOf(action.substring("menu_oni:".length()));if(isOniTypeLocked(requested)){msg(p,"&8EX鬼 蛇窟姫（ジャクツキ） &fは現在ロックされています。");openOniMenu(p);return;}selectedOniType=requested;
                msg(p,"次の鬼を &c"+selectedOniType.display+" &fに設定しました。");
                openOniMenu(p);
            }else if(holder.type.startsWith("oni_passives")&&action.startsWith("menu_oni_passive:")){
                OniPassiveSkill passive=OniPassiveSkill.valueOf(action.substring("menu_oni_passive:".length()));
                LinkedHashSet<OniPassiveSkill> selected=selectedOniPassives;
                if(selected.remove(passive))msg(p,"鬼パッシブ &5"+passive.display+" &fを解除しました。");
                else if(selected.size()>=4)msg(p,"鬼パッシブは4個までです。選択済みを解除してください。");
                else{selected.add(passive);msg(p,"鬼パッシブ &5"+passive.display+" &fを設定しました。");}
                openOniPassivePage(p,menuPageFromType(holder.type));
            }
            return;
        }
        if(state==GameState.RUNNING&&e.getWhoClicked() instanceof Player player&&(isMainSkillItem(e.getCurrentItem())||isMainSkillItem(e.getCursor())||(e.getHotbarButton()>=0&&isMainSkillItem(player.getInventory().getItem(e.getHotbarButton()))))){e.setCancelled(true);return;}
        if(state!=GameState.RUNNING||oni==null||!e.getWhoClicked().getUniqueId().equals(oni))return;if(e.getSlotType()==InventoryType.SlotType.ARMOR||isDakkoArmor(e.getCurrentItem())||isDakkoArmor(e.getCursor())||isJakutsukiArmor(e.getCurrentItem())||isJakutsukiArmor(e.getCursor()))e.setCancelled(true);
    }
    @EventHandler public void onInventoryDrag(InventoryDragEvent e){if((personalLootInventories.containsValue(e.getView().getTopInventory())||e.getView().getTopInventory().getHolder() instanceof MenuHolder)&&e.getRawSlots().stream().anyMatch(slot->slot<e.getView().getTopInventory().getSize()))e.setCancelled(true);}
    private void throwDecoyStone(Player player){
        if(!players.contains(player.getUniqueId())||!hasPassive(player.getUniqueId(),PassiveSkill.DECOY))return;
        if(!technicalReady(player.getUniqueId(),"DECOY",getConfig().getLong("technical-passives.decoy-cooldown-seconds",10)))return;
        Item decoy=player.getWorld().dropItem(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(.35)),new ItemStack(Material.SNOWBALL));
        decoy.setPickupDelay(Integer.MAX_VALUE);
        Vector velocity=player.getEyeLocation().getDirection().normalize().multiply(1.15);
        velocity.setY(Math.max(.22,velocity.getY()+.18));
        decoy.setVelocity(velocity);
        fakeNoiseUntil.put(decoy.getUniqueId(),System.currentTimeMillis()+Math.max(1,getConfig().getLong("technical-passives.decoy-duration-seconds",6))*1000L);
        player.swingMainHand();
        player.playSound(player.getLocation(),Sound.ENTITY_SNOWBALL_THROW,.8f,.8f);
        player.sendActionBar(cc("&b陽動石 &7――投擲地点に偽の走行音を発生"));
    }
    @EventHandler public void onDrop(PlayerDropItemEvent e){
        if(state!=GameState.RUNNING)return;
        String action=actionOf(e.getItemDrop().getItemStack());
        if("passive:decoy".equals(action)){
            e.setCancelled(true);
            e.getPlayer().sendActionBar(cc("&7陽動石は &e右クリック &7で投げます。"));
            return;
        }
        if(isMainSkillItem(e.getItemDrop().getItemStack())||(e.getPlayer().getUniqueId().equals(oni)&&(isDakkoArmor(e.getItemDrop().getItemStack())||isJakutsukiArmor(e.getItemDrop().getItemStack()))))e.setCancelled(true);
    }
    @EventHandler public void onPickup(EntityPickupItemEvent e){if(fakeNoiseUntil.containsKey(e.getItem().getUniqueId())){e.setCancelled(true);return;}if(state==GameState.RUNNING&&e.getEntity() instanceof Player player&&isConsumableItem(e.getItem().getItemStack())&&carriedConsumables(player)>=getConfig().getInt("loot-chests.max-carried-items",3))e.setCancelled(true);}
    private void weakenOni(){
        LivingEntity entity=getOniEntity();if(entity==null)return;
        double max;if(oniType==OniType.JAKUTSUKI){double base=Math.max(1.0,getConfig().getDouble("jakutsuki-stats.max-health",200.0)),loss=Math.max(0.0,getConfig().getDouble("jakutsuki-stats.max-health-loss-per-heart",10.0));max=Math.max(1.0,base-(brokenHearts*loss));}else max=Math.max(30,60-(brokenHearts*6));entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);if(entity.getHealth()>max)entity.setHealth(max);if(oniType==OniType.JAKUTSUKI){if(entity instanceof Player jp)jp.sendActionBar(cc("&5蛇窟姫 &7最大体力が &c"+(int)max+" &7まで低下した"));}
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,120,Math.min(3,brokenHearts-1),false,true));
        if(entity instanceof Player p)refreshOniUnlockedSkills(p,true);
    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false)
    public void onLobbyUtilityInteractEarly(PlayerInteractEvent e){
        if(state!=GameState.WAITING||e.getClickedBlock()==null||!e.getAction().isRightClick()||e.getHand()!=EquipmentSlot.HAND)return;
        Block block=e.getClickedBlock();
        if(isSkillChest(block)){queueLobbyUtilityGui(e.getPlayer(),"skill");return;}
        if(isOniChest(block)){queueLobbyUtilityGui(e.getPlayer(),"oni");return;}
        if(isPracticeChest(block)){queueLobbyUtilityGui(e.getPlayer(),"practice-chest");return;}
        if(isPracticeHeartKey(LocationStore.encode(block.getLocation())))queueLobbyUtilityGui(e.getPlayer(),"practice-heart");
    }
    private void queueLobbyUtilityGui(Player player,String kind){
        UUID id=player.getUniqueId();
        if(!lobbyUtilityGuiQueued.add(id))return;
        Bukkit.getScheduler().runTaskLater(this,()->{
            lobbyUtilityGuiQueued.remove(id);
            if(!player.isOnline()||state!=GameState.WAITING)return;
            switch(kind){
                case "skill"->openSkillMenu(player);
                case "oni"->openOniMenu(player);
                case "practice-chest"->{
                    Location configured=LocationStore.get(getConfig(),"locations.practice-loot-chest");
                    if(configured!=null)startPracticeChestOpening(player,LocationStore.encode(configured));
                }
                case "practice-heart"->{
                    Location configured=LocationStore.get(getConfig(),"locations.practice-heart");
                    if(configured!=null&&configured.getWorld()!=null&&configured.getWorld().equals(player.getWorld())
                            &&player.getLocation().distanceSquared(configured.clone().add(.5,.5,.5))<=7.5625)
                        toggleHeartRepair(player,configured.getBlock());
                }
            }
        },1L);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void onInteract(PlayerInteractEvent e){
        if(e.getClickedBlock()!=null&&isSkillChest(e.getClickedBlock())){
            // Always intercept the registered skill chest ourselves. This deliberately does
            // not rely on vanilla chest-use permission (spawn protection / region plugins
            // often allow OPs while denying normal players). Open the OniGame GUI on the
            // next server tick so a later vanilla/protection decision cannot replace it.
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND){
                Player player=e.getPlayer();
                if(state==GameState.WAITING)queueLobbyUtilityGui(player,"skill");
                else msg(player,"スキル設定チェストはロビーでのみ使用できます。");
            }
            return;
        }
        if(e.getClickedBlock()!=null&&isOniChest(e.getClickedBlock())){
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND){
                Player player=e.getPlayer();
                if(state==GameState.WAITING)queueLobbyUtilityGui(player,"oni");
                else msg(player,"鬼設定チェストはロビーでのみ使用できます。");
            }
            return;
        }
        if(state==GameState.WAITING&&e.getClickedBlock()!=null&&isPracticeChest(e.getClickedBlock())){e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)queueLobbyUtilityGui(e.getPlayer(),"practice-chest");return;}
        if(state==GameState.WAITING&&e.getClickedBlock()!=null&&isPracticeHeartKey(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)queueLobbyUtilityGui(e.getPlayer(),"practice-heart");return;}
        if(state==GameState.WAITING&&isInTrainingArea(e.getPlayer().getLocation())&&e.getItem()!=null&&e.getAction().isRightClick()&&e.getItem().hasItemMeta()){String trainingAction=e.getItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(trainingAction!=null&&trainingAction.startsWith("training_skill:")){e.setCancelled(true);PlayerSkill skill=PlayerSkill.valueOf(trainingAction.substring("training_skill:".length()));usePlayerSkill(e.getPlayer(),skill);trainingLog(e.getPlayer(),"&b"+skill.display+" &7を使用");return;}if(trainingAction!=null&&trainingAction.startsWith("training_oni:")){e.setCancelled(true);String action=trainingAction.substring("training_oni:".length());useOniSkill(e.getPlayer(),action);trainingLog(e.getPlayer(),"&c鬼スキル &7を使用");return;}}
        if(state!=GameState.RUNNING)return;
        if(e.getClickedBlock()!=null&&lootChestKeys.contains(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)startChestOpening(e.getPlayer(),LocationStore.encode(e.getClickedBlock().getLocation()));return;}
        if(e.getClickedBlock()!=null&&heartHp.containsKey(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)toggleHeartRepair(e.getPlayer(),e.getClickedBlock());return;}
        if(e.getItem()==null||!e.getAction().isRightClick()||!e.getItem().hasItemMeta())return;String action=e.getItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(action==null)return;e.setCancelled(true);Player p=e.getPlayer();if(action.startsWith("skill:")){if(!players.contains(p.getUniqueId()))return;usePlayerSkill(p,PlayerSkill.valueOf(action.substring(6)));}else if(action.startsWith("item:")){if(!players.contains(p.getUniqueId()))return;useConsumable(p,action,e.getItem());}else if(action.equals("equipment:flare_gun")){if(!players.contains(p.getUniqueId()))return;useFlareGun(p);}else if(action.startsWith("equipment:")){e.setCancelled(false);}else if(action.equals("passive:decoy")){throwDecoyStone(p);}else if(oni!=null&&p.getUniqueId().equals(oni))useOniSkill(p,action);else if(action.equals("tracker"))trackHeart(p);
    }
    private boolean isSkillChest(Block block){return matchesConfiguredChest(block,"locations.skill-chest");}
    private boolean isOniChest(Block block){return matchesConfiguredChest(block,"locations.oni-chest");}
    private boolean matchesConfiguredChest(Block block,String path){
        Location configured=LocationStore.get(getConfig(),path);
        if(configured==null||block==null||configured.getWorld()==null||!block.getWorld().equals(configured.getWorld()))return false;
        if(LocationStore.encode(configured).equals(LocationStore.encode(block.getLocation())))return true;
        // A configured chest may later become one half of a large chest.  In that case
        // Bukkit reports the clicked half's block location, so accept either half of the
        // same DoubleChest instead of requiring an exact single-block coordinate match.
        Block configuredBlock=configured.getBlock();
        if(!(configuredBlock.getState() instanceof org.bukkit.block.Chest configuredChest)||!(block.getState() instanceof org.bukkit.block.Chest clickedChest))return false;
        InventoryHolder configuredHolder=configuredChest.getInventory().getHolder();
        InventoryHolder clickedHolder=clickedChest.getInventory().getHolder();
        if(configuredHolder instanceof org.bukkit.block.DoubleChest configuredDouble&&containsChestLocation(configuredDouble,block.getLocation()))return true;
        return clickedHolder instanceof org.bukkit.block.DoubleChest clickedDouble&&containsChestLocation(clickedDouble,configured);
    }
    private boolean containsChestLocation(org.bukkit.block.DoubleChest chest,Location location){
        return chestSideAt(chest.getLeftSide(),location)||chestSideAt(chest.getRightSide(),location);
    }
    private boolean chestSideAt(InventoryHolder holder,Location location){
        return holder instanceof org.bukkit.block.Chest chest&&LocationStore.encode(chest.getLocation()).equals(LocationStore.encode(location));
    }
    @EventHandler public void onSneak(PlayerToggleSneakEvent e){Player player=e.getPlayer();UUID id=player.getUniqueId();boolean practice=state==GameState.WAITING&&repairingHeart.containsKey(id)&&isPracticeHeartKey(repairingHeart.get(id));if(state!=GameState.RUNNING&&!practice)return;if(!practice&&!players.contains(id))return;long now=System.currentTimeMillis();if(!e.isSneaking()){sneakStartedAt.remove(id);return;}sneakStartedAt.put(id,now);parrySneakAt.put(id,now);if(repairingHeart.containsKey(id)&&skillChecks.containsKey(id))resolveSkillCheck(player);if(hasPassive(id,PassiveSkill.QUICK_TURN)&&player.isSprinting()){Vector movement=player.getVelocity().setY(0),look=player.getEyeLocation().getDirection().setY(0);if(movement.lengthSquared()>.01&&look.lengthSquared()>.01&&Math.abs(movement.normalize().dot(look.normalize()))<.45&&technicalReady(id,"QUICK_TURN",getConfig().getLong("technical-passives.quick-turn-cooldown-seconds",12))){player.setVelocity(look.multiply(getConfig().getDouble("technical-passives.quick-turn-velocity",.9)).setY(.18));player.getWorld().spawnParticle(Particle.CLOUD,player.getLocation(),12,.25,.1,.25,.04);player.sendActionBar(cc("&b急転 &7――サイドステップ"));}}}
    private boolean technicalReady(UUID id,String key,long cooldownSeconds){long now=System.currentTimeMillis();String mapKey=id+":"+key;if(now<technicalPassiveReadyAt.getOrDefault(mapKey,0L))return false;technicalPassiveReadyAt.put(mapKey,now+Math.max(1,cooldownSeconds)*1000L);return true;}
    private boolean updateDesperateState(Player player,long now){String key=player.getUniqueId()+":DESPERATE";Long until=timedSkillUntil.get(key);if(until==null)return false;if(now<until)return true;timedSkillUntil.remove(key);stamina.put(player.getUniqueId(),0.0);player.setFoodLevel(0);player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,100,1,false,true));player.sendActionBar(cc("&c決死行終了 &7――スタミナ枯渇"));return false;}
    private void clearFakeNoises(){for(UUID id:new HashSet<>(fakeNoiseUntil.keySet())){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}fakeNoiseUntil.clear();}
    @EventHandler public void onMove(PlayerMoveEvent e){if(e.getTo()==null)return;if(state==GameState.WAITING){boolean was=isInTrainingArea(e.getFrom()),now=isInTrainingArea(e.getTo());if(!was&&now)enterTraining(e.getPlayer());else if(was&&!now)leaveTraining(e.getPlayer());return;}if(state!=GameState.RUNNING||!participants.contains(e.getPlayer().getUniqueId()))return;Player p=e.getPlayer();Location from=e.getFrom(),to=e.getTo();double horizontal=Math.pow(to.getX()-from.getX(),2)+Math.pow(to.getZ()-from.getZ(),2);if(horizontal<0.0001)return;long now=System.currentTimeMillis();if(p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR&&p.isSprinting()&&to.getY()-from.getY()>0.12&&now-dashJumpPenaltyAt.getOrDefault(p.getUniqueId(),0L)>700){dashJumpPenaltyAt.put(p.getUniqueId(),now);double value=Math.max(0,stamina.getOrDefault(p.getUniqueId(),20.0)-getConfig().getDouble("stamina.dash-jump-cost",5.0));stamina.put(p.getUniqueId(),value);p.setFoodLevel((int)Math.ceil(value));p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,Math.max(0,getConfig().getInt("stamina.dash-jump-slow-ticks",20)),0,false,false));if(value<=0)p.setSprinting(false);p.sendActionBar(cc("&cダッシュジャンプ &7――スタミナを大きく消費"));return;}if(!getConfig().getBoolean("auto-step.enabled",true)||Math.abs(to.getY()-from.getY())>0.08||!p.isOnGround())return;Vector direction=new Vector(to.getX()-from.getX(),0,to.getZ()-from.getZ());if(direction.lengthSquared()<0.0001)return;direction.normalize();Location probe=to.clone().add(direction.multiply(getConfig().getDouble("auto-step.probe-distance",0.45)));Block obstacle=probe.getWorld().getBlockAt(probe.getBlockX(),from.getBlockY(),probe.getBlockZ());double height=obstacle.getBoundingBox().getHeight();if(!obstacle.getType().isSolid()||height<0.99||height>1.01||!obstacle.getRelative(0,1,0).isPassable()||!obstacle.getRelative(0,2,0).isPassable())return;Location stepped=to.clone();stepped.setX(probe.getX());stepped.setY(obstacle.getY()+1.0);stepped.setZ(probe.getZ());e.setTo(stepped);p.setFallDistance(0);}
    @EventHandler public void onTechnicalMovement(PlayerMoveEvent e){if(state!=GameState.RUNNING||e.getTo()==null||!players.contains(e.getPlayer().getUniqueId()))return;Vector current=e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);if(current.lengthSquared()<.002)return;UUID id=e.getPlayer().getUniqueId();if(e.getPlayer().isSneaking()&&hasPassive(id,PassiveSkill.SILENT_BREATH))sneakStartedAt.put(id,System.currentTimeMillis());current.normalize();Vector previous=lastMoveDirection.put(id,current.clone());if(previous!=null&&previous.dot(current)<-.65)reversalAt.put(id,System.currentTimeMillis());}
    @EventHandler public void onPassiveLeap(PlayerToggleFlightEvent e){Player p=e.getPlayer();UUID id=p.getUniqueId();if(state!=GameState.RUNNING||!players.contains(id)||!hasPassive(id,PassiveSkill.LEAP)||p.getGameMode()==GameMode.SPECTATOR)return;e.setCancelled(true);p.setFlying(false);long now=System.currentTimeMillis(),readyAt=passiveLeapReadyAt.getOrDefault(id,0L);if(now<readyAt){p.setAllowFlight(false);return;}p.setAllowFlight(false);passiveLeapReadyAt.put(id,now+Math.max(1,getConfig().getLong("passive-skills.leap-cooldown-seconds",10))*1000L);Vector facing=p.getEyeLocation().getDirection();Vector horizontal=new Vector(facing.getX(),0,facing.getZ());if(horizontal.lengthSquared()<0.0001)horizontal=new Vector(0,0,1);horizontal.normalize().multiply(getConfig().getDouble("passive-skills.leap-forward-velocity",1.25));horizontal.setY(getConfig().getDouble("passive-skills.leap-upward-velocity",0.85));p.setVelocity(horizontal);p.setFallDistance(0);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),22,.35,.12,.35,.05);p.playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.65f,1.55f);p.sendActionBar(cc("&b跳躍 &7――前方へ大きく跳んだ &8(CT "+getConfig().getLong("passive-skills.leap-cooldown-seconds",10)+"秒)"));}
    private boolean isInTrainingArea(Location location){if(location==null||state!=GameState.WAITING||!getConfig().getBoolean("training.enabled",true))return false;Location a=LocationStore.get(getConfig(),"locations.training-pos1"),b=LocationStore.get(getConfig(),"locations.training-pos2");if(a==null||b==null||a.getWorld()!=location.getWorld()||b.getWorld()!=location.getWorld())return false;double minX=Math.min(a.getX(),b.getX()),maxX=Math.max(a.getX(),b.getX())+1,minY=Math.min(a.getY(),b.getY()),maxY=Math.max(a.getY(),b.getY())+1,minZ=Math.min(a.getZ(),b.getZ()),maxZ=Math.max(a.getZ(),b.getZ())+1;return location.getX()>=minX&&location.getX()<maxX&&location.getY()>=minY&&location.getY()<maxY&&location.getZ()>=minZ&&location.getZ()<maxZ;}
    private void enterTraining(Player p){if(!trainingPlayers.add(p.getUniqueId()))return;applyTrainingPassives(p);giveTrainingItems(p);p.sendTitle(cc("&b&lTRAINING MODE"),cc("&fスキル＋パッシブ実戦テスト"),5,35,10);p.sendActionBar(cc("&b練習モード &7――選択中のぷれいやー/鬼パッシブ有効・鬼スキル全解放"));Location dl=LocationStore.get(getConfig(),"locations.training-dummy");if(dl!=null&&(trainingDummy==null||Bukkit.getEntity(trainingDummy)==null))spawnTrainingDummy(dl);}
    private void leaveTraining(Player p){trainingPlayers.remove(p.getUniqueId());removeTrainingItems(p);cooldowns.keySet().removeIf(k->k.startsWith(p.getUniqueId()+":"));resetTrainingPassiveStats(p);p.sendActionBar(cc("&7練習モードを終了しました。"));}
    private void applyTrainingPassives(Player p){
        resetTrainingPassiveStats(p);
        if(hasPassive(p.getUniqueId(),PassiveSkill.DURABILITY_BOOST)){double max=Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);p.setHealth(Math.min(max,Math.max(p.getHealth(),max)));}
        if(hasPassive(p.getUniqueId(),PassiveSkill.NINJA_BLOOD))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23));
        else if(hasPassive(p.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.divine-technique-walk-speed",0.21));
        stamina.put(p.getUniqueId(),20.0);p.setFoodLevel(20);
        if(hasPassive(p.getUniqueId(),PassiveSkill.DECOY))p.getInventory().addItem(menuItem(Material.SNOWBALL,"&b&l[練習] 陽動石","passive:decoy","&7選択中パッシブ: 陽動"));
        if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null&&selectedOniPassives.contains(OniPassiveSkill.ANCHOR))p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))));
    }
    private void resetTrainingPassiveStats(Player p){
        if(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null){p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);if(p.getHealth()>20)p.setHealth(20);}
        if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
        p.setWalkSpeed((float)getConfig().getDouble("player-walk-speed",0.20));
    }
    private void giveTrainingItems(Player p){removeTrainingItems(p);applyTrainingPassives(p);PlayerSkill sk=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);p.getInventory().addItem(playerSkillIconItem(sk,"&b&l[練習] "+sk.display,"training_skill:"+sk.name()));OniType type=selectedOniType==null?OniType.DAKKO:selectedOniType;if(isOniTypeLocked(type))type=OniType.DAKKO;if(type==OniType.DAKKO){p.getInventory().addItem(oniSkillIconItem("dakko_tp","&d&l[練習] 狐渡り","training_oni:dakko_tp"),oniSkillIconItem("dakko_clone","&6&l[練習] 分霊","training_oni:dakko_clone"),oniSkillIconItem("dakko_fox_fire","&6&l[練習] 狐火","training_oni:dakko_fox_fire"),oniSkillIconItem("dakko_heavenly_arrival","&d&l[練習] 天来","training_oni:dakko_heavenly_arrival"));}else if(type==OniType.KISHIN){p.getInventory().addItem(oniSkillIconItem("kishin_charge","&c&l[練習] 鬼突","training_oni:kishin_charge"),oniSkillIconItem("kishin_slam","&4&l[練習] 地砕","training_oni:kishin_slam"),oniSkillIconItem("kishin_roar","&4&l[練習] 鬼吼","training_oni:kishin_roar"),oniSkillIconItem("kishin_iron_body","&6&l[練習] 剛身","training_oni:kishin_iron_body"));}else{p.getInventory().addItem(oniSkillIconItem("jakutsuki_black_mirror","&5&l[練習] 黒鏡","training_oni:jakutsuki_black_mirror"),oniSkillIconItem("jakutsuki_sweep","&5&l[練習] 薙ぎ払い","training_oni:jakutsuki_sweep"),oniSkillIconItem("jakutsuki_snakefall","&8&l[練習] 蛇崩","training_oni:jakutsuki_snakefall"),oniSkillIconItem("jakutsuki_release","&4&l[練習] 解放","training_oni:jakutsuki_release"),oniSkillIconItem("jakutsuki_piercing_blast","&5&l[練習] 黒の波動","training_oni:jakutsuki_piercing_blast"));}}
    private void removeTrainingItems(Player p){for(int i=0;i<p.getInventory().getSize();i++){ItemStack stack=p.getInventory().getItem(i);String action=actionOf(stack);if(action!=null&&(action.startsWith("training_skill:")||action.startsWith("training_oni:")||("passive:decoy".equals(action)&&stack.hasItemMeta()&&stack.getItemMeta().hasDisplayName()&&ChatColor.stripColor(stack.getItemMeta().getDisplayName()).contains("[練習]"))))p.getInventory().setItem(i,null);}}
    private void trainingLog(Player p,String text){if(getConfig().getBoolean("training.show-actionbar-log",true))p.sendActionBar(cc("&8[&bTRAINING&8] "+text));}
    private void spawnTrainingDummy(Location location){removeTrainingDummy();if(location==null||location.getWorld()==null)return;Husk dummy=location.getWorld().spawn(location,Husk.class);dummy.setAI(false);dummy.setSilent(true);dummy.setRemoveWhenFarAway(false);dummy.setCustomName(cc("&c&l鬼ダミー &7[TRAINING]"));dummy.setCustomNameVisible(true);dummy.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200);dummy.setHealth(200);dummy.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"training_dummy");trainingDummy=dummy.getUniqueId();}
    private void removeTrainingDummy(){if(trainingDummy!=null){Entity e=Bukkit.getEntity(trainingDummy);if(e!=null)e.remove();trainingDummy=null;}}
    private boolean ready(Player p,String id,int cd){if(players.contains(p.getUniqueId())&&System.currentTimeMillis()<oniSkillSealUntil.getOrDefault(p.getUniqueId(),0L)){msg(p,"&5封殺 &7――スキルが封じられている。");return false;}boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());if(training)cd=Math.max(1,getConfig().getInt("training.cooldown-seconds",1));boolean oniSkill=isOniSkillAction(id)&&(training||(oni!=null&&p.getUniqueId().equals(oni)));if(oniSkill&&hasOniPassive(OniPassiveSkill.MASTERY))cd=Math.max(1,(int)Math.ceil(cd*Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.mastery-cooldown-multiplier",.85)))));if(oniSkill&&hasOniPassive(OniPassiveSkill.TERRITORY)&&oniNearAliveHeart(p,getConfig().getDouble("oni-passive-skills.territory-radius",8.0)))cd=Math.max(1,(int)Math.ceil(cd*Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.territory-cooldown-multiplier",.80)))));String k=p.getUniqueId()+":"+id;long now=System.currentTimeMillis();long until=cooldowns.getOrDefault(k,0L);if(until>now){msg(p,"あと &e"+((until-now+999)/1000)+"秒 &f待ってください。");return false;}cooldowns.put(k,now+cd*1000L);if(oniSkill)recoverOniSkillMomentum();return true;}
    private boolean readyAutomatic(Player p,String id,int cd){if(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))cd=Math.max(1,getConfig().getInt("training.cooldown-seconds",1));String k=p.getUniqueId()+":"+id;long now=System.currentTimeMillis();if(cooldowns.getOrDefault(k,0L)>now)return false;cooldowns.put(k,now+cd*1000L);return true;}
    private void usePlayerSkill(Player p,PlayerSkill s){boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());if(s==PlayerSkill.HEAL){if(training&&p.getHealth()>=p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()-.01)p.setHealth(Math.max(1,p.getHealth()-8));startHealing(p,true);return;}if(s==PlayerSkill.SAFE_LANDING){msg(p,"&e安定着地 &fは落下ダメージが発生する着地時に自動発動します。");return;}if(!training&&s==PlayerSkill.DESPERATE_RUN&&p.getHealth()>p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*.30){msg(p,"&c決死行は体力30%以下でのみ使用できます。");return;}if(!training&&s==PlayerSkill.RESONANCE&&stamina.getOrDefault(p.getUniqueId(),20.0)<6){msg(p,"&c共鳴にはスタミナが6必要です。");return;}if(training&&s==PlayerSkill.RESONANCE)stamina.put(p.getUniqueId(),20.0);if(!ready(p,s.name(),s.cooldown))return;switch(s){case SPRINT->p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(1,getConfig().getInt("skills.sprint-duration-ticks",100)),Math.max(0,getConfig().getInt("skills.sprint-amplifier",2)),false,true));case INVISIBLE->p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,120,0,false,true));case SMOKE->{p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,p.getLocation(),160,4,2,4,.03);LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<64){boolean resist=consumeGrudgeResistance(p.getUniqueId());o.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,resist?30:80,0,false,true));registerOniGrudge(p.getUniqueId());if(resist&&o instanceof Player op)op.sendActionBar(cc("&5怨返し &7――煙幕への耐性を発動"));}}case ONI_STRIKE->{LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<=25){boolean resist=consumeGrudgeResistance(p.getUniqueId());double damage=brokenHearts==totalHearts?12:3;o.damage(damage,p);o.setVelocity(o.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(resist?.65:1.5).setY(resist?.2:.5));registerOniGrudge(p.getUniqueId());if(resist&&o instanceof Player op)op.sendActionBar(cc("&5怨返し &7――破鬼撃への耐性を発動"));}else msg(p,"鬼が5ブロック以内にいません。");}case OBSESSION->{int ticks=Math.max(1,getConfig().getInt("skills.obsession-glow-seconds",8))*20;p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));LivingEntity o=getOniEntity();if(o!=null)o.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,0.8f,1.25f);p.sendActionBar(cc("&e執念 &7――鬼の輪郭を捉えた"));}case BLINK->useBlink(p);case ECHO->useEcho(p);case CLAIRVOYANCE->useClairvoyance(p);case UNYIELDING->{timedSkillUntil.put(p.getUniqueId()+":UNYIELDING",System.currentTimeMillis()+6000);p.sendTitle("",cc("&6&l不退転 &f6秒"),0,25,5);}case SEALING_CIRCLE->{sealingCircles.put(p.getUniqueId(),p.getLocation().clone());sealingCircleUntil.put(p.getUniqueId(),System.currentTimeMillis()+10000);p.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,p.getLocation(),80,2,.2,2,.1);p.sendActionBar(cc("&9封鬼陣 &7――足元へ結界を設置"));}case RESONANCE->useResonance(p);case SUBSTITUTE->useSubstitute(p);case DESPERATE_RUN->{timedSkillUntil.put(p.getUniqueId()+":DESPERATE",System.currentTimeMillis()+8000);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,160,1,false,true));p.sendTitle("",cc("&c&l決死行 &f8秒"),0,25,5);}case BUGEI->useBugei(p);case HEAL,SAFE_LANDING->{}}}
    private void showBugeiSlotCinematic(Player p,UUID id){
        if(p==null)return;
        int step=Math.max(2,getConfig().getInt("skills.bugei.slot-step-ticks",4));
        // Presentation only: never random. Always stops at 跳躍 / 武芸 / キック.
        // Keep the center text compact by using only the smaller subtitle line.
        p.sendTitle("",cc("&e跳躍 &8│ &7・・・ &8│ &7・・・"),0,step+2,0);
        p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.32f,1.75f);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            p.sendTitle("",cc("&e跳躍 &8│ &6武芸 &8│ &7・・・"),0,step+2,0);
            p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.36f,1.55f);
        },step);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            p.sendTitle("",cc("&e跳躍 &8│ &6武芸 &8│ &cキック"),0,step+4,1);
            p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.42f,1.32f);
            p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.28f,1.85f);
        },step*2L);
    }
    private void useBugei(Player p){
        LivingEntity target=getOniEntity();
        double range=Math.max(3.0,getConfig().getDouble("skills.bugei.target-range",18.0));
        if(target==null||target.getWorld()!=p.getWorld()||target.getLocation().distanceSquared(p.getLocation())>range*range){msg(p,"&c武芸 &f――鬼が"+(int)range+"m以内にいません。");return;}
        UUID id=p.getUniqueId();bugeiActive.add(id);p.setFallDistance(0);
        p.setVelocity(new Vector(0,getConfig().getDouble("skills.bugei.launch-upward-velocity",1.05),0));
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.75f,1.45f);
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),24,.35,.15,.35,.08);
        showBugeiSlotCinematic(p,id);
        int slotDelay=Math.max(6,getConfig().getInt("skills.bugei.slot-step-ticks",4)*3);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
            task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
                ticks[0]++;LivingEntity oniEntity=getOniEntity();
                boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());
                if((state!=GameState.RUNNING&&!training)||!p.isOnline()||oniEntity==null||oniEntity.getWorld()!=p.getWorld()){
                    bugeiActive.remove(id);task[0].cancel();return;
                }
                Vector to=oniEntity.getEyeLocation().toVector().subtract(p.getLocation().toVector());
                double distance=to.length();
                double speed=getConfig().getDouble("skills.bugei.dive-speed",1.75);
                double boostDistance=getConfig().getDouble("skills.bugei.final-boost-distance",5.0);
                if(distance<=boostDistance)speed*=getConfig().getDouble("skills.bugei.final-boost-multiplier",1.35);
                if(distance>0.01)p.setVelocity(to.normalize().multiply(speed));
                p.setFallDistance(0);

                Location trail=p.getLocation().add(0,.85,0);
                Vector back=p.getVelocity().clone();
                if(back.lengthSquared()>.001)back.normalize().multiply(-.35);
                int density=Math.max(4,getConfig().getInt("skills.bugei.trail-density",10));
                for(int i=0;i<density;i++){
                    double spread=(i/(double)density)*1.6;
                    Location pt=trail.clone().add(back.clone().multiply(spread));
                    p.getWorld().spawnParticle(Particle.REDSTONE,pt,1,0,0,0,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,45,20),1.25f));
                    if(i%2==0)p.getWorld().spawnParticle(Particle.REDSTONE,pt.clone().add(0,.06,0),1,0,0,0,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,190,35),1.0f));
                }
                p.getWorld().spawnParticle(Particle.CRIT,trail,4,.12,.12,.12,.02);
                p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),2,.08,.08,.08,.01);
                if(distance<=boostDistance){
                    p.getWorld().spawnParticle(Particle.FLAME,trail,3,.12,.12,.12,.015);
                    if(ticks[0]%4==0)p.playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,.35f,1.8f);
                }
                if(distance<=getConfig().getDouble("skills.bugei.impact-distance",2.2)){
                    double damage=brokenHearts>=totalHearts?getConfig().getDouble("skills.bugei.final-damage",18.0):getConfig().getDouble("skills.bugei.sealed-damage",10.0);
                    oniEntity.damage(damage,p);
                    Vector push=oniEntity.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=p.getEyeLocation().getDirection();oniEntity.setVelocity(push.normalize().multiply(getConfig().getDouble("skills.bugei.knockback",1.35)).setY(.55));
                    Location impact=oniEntity.getLocation().add(0,1,0);
                    p.getWorld().playSound(impact,Sound.ENTITY_PLAYER_ATTACK_CRIT,1.0f,.68f);
                    p.getWorld().playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,.55f,1.55f);
                    p.getWorld().playSound(impact,Sound.BLOCK_ANVIL_LAND,.32f,1.75f);
                    p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,impact,2,.15,.15,.15,.01);
                    p.getWorld().spawnParticle(Particle.CRIT_MAGIC,impact,30,.55,.65,.55,.10);
                    p.getWorld().spawnParticle(Particle.FLAME,impact,18,.45,.45,.45,.05);

                    int ring=Math.max(18,getConfig().getInt("skills.bugei.impact-ring-particles",42));
                    for(int i=0;i<ring;i++){
                        double a=(Math.PI*2.0*i)/ring;
                        double radius=1.7;
                        Location r=impact.clone().add(Math.cos(a)*radius,-.55,Math.sin(a)*radius);
                        Particle.DustOptions dust=(i%2==0)
                            ?new Particle.DustOptions(org.bukkit.Color.fromRGB(255,35,15),1.35f)
                            :new Particle.DustOptions(org.bukkit.Color.fromRGB(255,205,40),1.15f);
                        p.getWorld().spawnParticle(Particle.REDSTONE,r,1,0,0,0,0,dust);
                    }

                    p.setVelocity(p.getVelocity().multiply(-.12).setY(.46));p.setFallDistance(0);
                    p.sendTitle("",cc("&6&lライダーキック &8- &eRIDER KICK"),0,14,5);
                    p.sendActionBar(cc("&6武芸 &f――鬼へ強烈な一撃！"));
                    bugeiActive.remove(id);task[0].cancel();return;
                }
                if(ticks[0]>=Math.max(8,getConfig().getInt("skills.bugei.max-dive-ticks",32))){
                    bugeiActive.remove(id);p.setFallDistance(0);task[0].cancel();p.sendActionBar(cc("&7武芸――攻撃は届かなかった"));
                }
            },0L,1L);
        },slotDelay);
    }

    private void useBlink(Player p){Vector direction=p.getVelocity().setY(0);if(direction.lengthSquared()<.02)direction=p.getEyeLocation().getDirection().setY(0);if(direction.lengthSquared()<.01)return;direction.normalize();Location destination=p.getLocation().clone();for(int i=0;i<8;i++){Location next=destination.clone().add(direction.clone().multiply(.5));if(!next.getBlock().isPassable()||!next.clone().add(0,1,0).getBlock().isPassable())break;destination=next;}p.teleport(destination);double value=Math.max(0,stamina.getOrDefault(p.getUniqueId(),20.0)-4);stamina.put(p.getUniqueId(),value);p.setFoodLevel((int)Math.ceil(value));p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation(),30,.3,.5,.3,.2);}
    private void useEcho(Player p){ArmorStand echo=p.getWorld().spawn(p.getLocation(),ArmorStand.class);echo.setVisible(false);echo.setArms(true);echo.setBasePlate(false);echo.setGravity(true);echo.setInvulnerable(true);echo.setCustomName("残響");ItemStack head=new ItemStack(Material.PLAYER_HEAD);SkullMeta skull=(SkullMeta)head.getItemMeta();skull.setOwningPlayer(p);head.setItemMeta(skull);echo.getEquipment().setHelmet(head);echo.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));echo.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));echo.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));echo.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand().clone());Vector initialDirection=p.getEyeLocation().getDirection().setY(0);if(initialDirection.lengthSquared()<.01)initialDirection=new Vector(0,0,1);initialDirection.normalize();long durationMillis=15000;fakeNoiseUntil.put(echo.getUniqueId(),System.currentTimeMillis()+durationMillis);timedSkillUntil.put(p.getUniqueId()+":ECHO_SILENT",System.currentTimeMillis()+1000);Vector startDirection=initialDirection;new Runnable(){private int elapsedTicks=0,nextTurnTicks=20+(int)(Math.random()*31);private Vector direction=startDirection;@Override public void run(){if(!echo.isValid()||elapsedTicks>=300){fakeNoiseUntil.remove(echo.getUniqueId());if(echo.isValid())echo.remove();return;}Location here=echo.getLocation();Block feetAhead=here.clone().add(direction.clone().multiply(.7)).getBlock(),headAhead=feetAhead.getRelative(0,1,0);boolean blocked=feetAhead.getType().isSolid()||headAhead.getType().isSolid();if(blocked||elapsedTicks>=nextTurnTicks){double turn=blocked?(Math.random()<.5?-1:1)*(Math.PI*.55+Math.random()*.7):(Math.random()-.5)*1.25;double cos=Math.cos(turn),sin=Math.sin(turn);direction=new Vector(direction.getX()*cos-direction.getZ()*sin,0,direction.getX()*sin+direction.getZ()*cos).normalize();nextTurnTicks=elapsedTicks+20+(int)(Math.random()*41);}Vector velocity=echo.getVelocity();velocity.setX(direction.getX()*.22).setZ(direction.getZ()*.22);if(echo.isOnGround()&&(blocked||Math.random()<.018))velocity.setY(.36);echo.setVelocity(velocity);float yaw=(float)Math.toDegrees(Math.atan2(-direction.getX(),direction.getZ()));echo.setRotation(yaw,0);double swing=Math.sin(elapsedTicks*.65)*.7;echo.setRightArmPose(new EulerAngle(swing,0,0));echo.setLeftArmPose(new EulerAngle(-swing,0,0));echo.setRightLegPose(new EulerAngle(-swing,0,0));echo.setLeftLegPose(new EulerAngle(swing,0,0));if(elapsedTicks%10==0)echo.getWorld().playSound(echo.getLocation(),Sound.BLOCK_GRASS_STEP,.65f,1.1f);elapsedTicks+=2;Bukkit.getScheduler().runTaskLater(OniGamePlugin.this,this,2);}}.run();p.sendActionBar(cc("&d残響 &7――15秒間、偽の姿と走行音を放った"));}
    private void useClairvoyance(Player p){final BukkitTask[] task=new BukkitTask[1];final int[] count={0};task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{if((state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))||!p.isOnline()||count[0]++>=8){task[0].cancel();return;}Set<String> visibleHearts=new HashSet<>(heartHp.keySet());visibleHearts.addAll(fakeHeartKeys);for(String key:visibleHearts){Location l=LocationStore.decode(key);if(l!=null&&l.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.REDSTONE,l.clone().add(.5,1,.5),12,.3,.5,.3,0,new Particle.DustOptions(Color.RED,1.5f));}for(String key:lootChestKeys){Location l=LocationStore.decode(key);if(l!=null&&l.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.VILLAGER_HAPPY,l.clone().add(.5,1,.5),10,.3,.4,.3,.02);}if(state==GameState.WAITING){Location ph=LocationStore.get(getConfig(),"locations.practice-heart");if(ph!=null&&ph.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.REDSTONE,ph.clone().add(.5,1,.5),12,.3,.5,.3,0,new Particle.DustOptions(Color.RED,1.5f));Location pc=LocationStore.get(getConfig(),"locations.practice-loot-chest");if(pc!=null&&pc.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.VILLAGER_HAPPY,pc.clone().add(.5,1,.5),10,.3,.4,.3,.02);if(trainingDummy!=null&&Bukkit.getEntity(trainingDummy) instanceof LivingEntity dummy&&dummy.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.SOUL_FIRE_FLAME,dummy.getLocation().add(0,2,0),10,.3,.4,.3,.02);}for(UUID id:players){Player ally=Bukkit.getPlayer(id);if(ally!=null&&!id.equals(p.getUniqueId())&&ally.getWorld().equals(p.getWorld())&&ally.getHealth()<ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())p.spawnParticle(Particle.HEART,ally.getLocation().add(0,2,0),5,.2,.2,.2,.01);}if(brokenHearts>=totalHearts&&activeExit!=null)p.spawnParticle(Particle.END_ROD,activeExit.clone().add(.5,1,.5),20,.5,1,.5,.03);},0,20);}
    private void useResonance(Player p){UUID id=p.getUniqueId();double self=Math.max(0,stamina.getOrDefault(id,20.0)-6);stamina.put(id,self);double radius=64;Collection<UUID> resonanceTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID allyId:resonanceTargets){if(allyId.equals(id))continue;Player ally=Bukkit.getPlayer(allyId);if(ally!=null&&ally.getWorld().equals(p.getWorld())&&ally.getLocation().distanceSquared(p.getLocation())<=radius){double value=Math.min(20,stamina.getOrDefault(allyId,20.0)+3);stamina.put(allyId,value);ally.setFoodLevel((int)Math.ceil(value));ally.sendActionBar(cc("&d共鳴 &7――スタミナ回復"));}}for(UUID botId:playerBots)if(Bukkit.getEntity(botId) instanceof LivingEntity bot&&bot.getWorld().equals(p.getWorld())&&bot.getLocation().distanceSquared(p.getLocation())<=radius)playerBotStamina.put(botId,Math.min(20,playerBotStamina.getOrDefault(botId,20.0)+3));p.setFoodLevel((int)Math.ceil(self));p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation(),50,4,1,4,.05);}
    private void useSubstitute(Player p){Collection<UUID> substituteTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;Player target=substituteTargets.stream().filter(id->!id.equals(p.getUniqueId())&&!dead.contains(id)&&!escaped.contains(id)).map(Bukkit::getPlayer).filter(Objects::nonNull).filter(q->q.getWorld().equals(p.getWorld())&&q.getLocation().distanceSquared(p.getLocation())<=225&&p.hasLineOfSight(q)).min(Comparator.comparingDouble(q->q.getLocation().distanceSquared(p.getLocation()))).orElse(null);if(target==null){msg(p,"&c身代わりにできる仲間が視線内15mにいません。");return;}Location first=p.getLocation().clone(),second=target.getLocation().clone();p.teleport(second);target.teleport(first);p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,60,0,false,false));target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,1,false,true));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,1,1);}
    private void useConsumable(Player p,String action,ItemStack stack){switch(action){case "item:sprint"->{p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,1,false,true));consumeOne(stack);}case "item:invisible"->{p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,60,0,false,true));consumeOne(stack);}case "item:smoke"->{p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,p.getLocation(),80,2.5,1.5,2.5,.02);LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<36)o.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,0,false,true));consumeOne(stack);}case "item:strike"->{LivingEntity o=getOniEntity();if(o==null||!o.getWorld().equals(p.getWorld())||o.getLocation().distanceSquared(p.getLocation())>16){msg(p,"鬼が4ブロック以内にいません。");return;}o.damage(brokenHearts==totalHearts?7:2,p);o.setVelocity(o.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(.8).setY(.25));consumeOne(stack);}case "item:heal"->startHealing(p,false);}}
    private void useFlareGun(Player player){
        if(!ready(player,"equipment_flare_gun",Math.max(1,getConfig().getInt("equipment.flare-gun-cooldown-seconds",15))))return;
        Snowball flare=player.launchProjectile(Snowball.class);flare.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.45));flare.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"equipment:flare_projectile");
        flare.setGlowing(true);player.getWorld().playSound(player.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,.9f,1.15f);player.sendActionBar(cc("&cフレアガン &7――照明弾を発射"));
    }
    @EventHandler public void onFlareHit(ProjectileHitEvent e){
        String action=e.getEntity().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(!"equipment:flare_projectile".equals(action))return;
        Location hit=e.getEntity().getLocation();World world=hit.getWorld();if(world==null)return;
        world.spawnParticle(Particle.FIREWORKS_SPARK,hit,70,1.3,1.3,1.3,.12);world.spawnParticle(Particle.END_ROD,hit,30,.8,.8,.8,.06);world.playSound(hit,Sound.ENTITY_FIREWORK_ROCKET_BLAST,1.2f,1.1f);
        LivingEntity oniEntity=getOniEntity();double radius=Math.max(1,getConfig().getDouble("equipment.flare-gun-reveal-radius",8.0));if(oniEntity!=null&&oniEntity.getWorld().equals(world)&&oniEntity.getLocation().distanceSquared(hit)<=radius*radius){int ticks=Math.max(1,getConfig().getInt("equipment.flare-gun-glow-seconds",5))*20;oniEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));}
        e.getEntity().remove();
    }
    private void useOniSkill(Player p,String action){
        boolean training=state==GameState.WAITING&&trainingPlayers.contains(p.getUniqueId())&&isInTrainingArea(p.getLocation());
        int heartPenalty=training?0:brokenHearts*3;
        int unlock=oniSkillUnlockHearts(action);
        // 練習場では心臓破壊数に関係なく、選択中の鬼の全スキルを試せる。
        if(!training&&unlock>0&&!oniSkillUnlocked(action)){msg(p,"&7この能力は心臓が &c"+unlock+"個 &7破壊されると解放されます。");return;}
        switch(action){
            case "dakko_tp"->{if(!ready(p,action,20+heartPenalty))return;Block b=p.getTargetBlockExact(Math.max(6,20-brokenHearts*3));if(b!=null)p.teleport(b.getLocation().add(.5,1,.5).setDirection(p.getLocation().getDirection()));}
            case "dakko_clone"->{if(!ready(p,action,35+heartPenalty))return;spawnDakkoCloneBots(p,p.getLocation());}
            case "dakko_fox_fire"->useFoxFire(p);
            case "dakko_heavenly_arrival"->useHeavenlyArrival(p);
            case "kishin_charge"->{if(!ready(p,action,18+heartPenalty))return;p.setVelocity(p.getLocation().getDirection().normalize().multiply(Math.max(1.4,2.6-brokenHearts*.2)).setY(.15));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);}
            case "kishin_slam"->{if(!ready(p,action,30+heartPenalty))return;p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation(),4);Collection<UUID> slamTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:slamTargets){Player q=Bukkit.getPlayer(id);if(q!=null&&!dead.contains(id)&&q.getWorld().equals(p.getWorld())&&q.getLocation().distanceSquared(p.getLocation())<49){q.damage(6,p);q.setVelocity(q.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(.8));q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,1));}}}
            case "kishin_roar"->useKishinRoar(p);
            case "kishin_iron_body"->useKishinIronBody(p);
            case "jakutsuki_black_mirror"->useBlackMirror(p);
            case "jakutsuki_sweep"->useJakutsukiSweep(p);
            case "jakutsuki_snakefall"->useSnakefall(p);
            case "jakutsuki_release"->useJakutsukiRelease(p);
            case "jakutsuki_piercing_blast"->useJakutsukiPiercingBlast(p);
        }
    }


    private void useKishinRoar(Player p){
        if(!ready(p,"kishin_roar",getConfig().getInt("kishin-skills.roar.cooldown-seconds",28)+brokenHearts*3))return;
        double radius=getConfig().getDouble("kishin-skills.roar.radius",8.0),damage=getConfig().getDouble("kishin-skills.roar.fixed-damage",4.0),knock=getConfig().getDouble("kishin-skills.roar.knockback",.65);
        int slowTicks=getConfig().getInt("kishin-skills.roar.slowness-ticks",60),weakTicks=getConfig().getInt("kishin-skills.roar.weakness-ticks",80);
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1.35f,.55f);p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation().add(0,1,0),8,1.4,.8,1.4,.05);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,1,0),90,radius*.55,.8,radius*.55,.08);
        Collection<UUID> ids=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;
        for(UUID id:ids){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(p,q,radius))continue;dealFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(p,q,knock,.32);q.sendActionBar(cc("&4鬼吼 &7――身体が竦んだ！"));}
        for(UUID id:playerBots){if(!validPlayerBotSkillTarget(p,id,radius))continue;LivingEntity q=(LivingEntity)Bukkit.getEntity(id);dealFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(p,q,knock,.32);}
    }
    private void useKishinIronBody(Player p){
        if(!ready(p,"kishin_iron_body",getConfig().getInt("kishin-skills.iron-body.cooldown-seconds",35)+brokenHearts*3))return;
        int ticks=Math.max(20,getConfig().getInt("kishin-skills.iron-body.duration-ticks",120));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,ticks,0,false,true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,ticks,1,false,true));
        p.getWorld().spawnParticle(Particle.CRIT,p.getLocation().add(0,1,0),60,.65,1,.65,.08);
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_SHIELD_BLOCK,1.1f,.65f);
        p.sendActionBar(cc("&6剛身 &7――耐性I＋速度II＋攻撃力上昇II"));
        UUID id=p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(state==GameState.RUNNING&&oni!=null&&oni.equals(id)&&p.isOnline()&&p.isValid()&&!p.isDead())
                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,Integer.MAX_VALUE,0,false,false));
        },ticks+1L);
    }
    private void pushAway(LivingEntity source,LivingEntity target,double horizontal,double vertical){Vector v=target.getLocation().toVector().subtract(source.getLocation().toVector()).setY(0);if(v.lengthSquared()<.01)v=new Vector(0,0,1);target.setVelocity(v.normalize().multiply(horizontal).setY(vertical));}

    private void useBlackMirror(Player p){
        boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());
        if(p.isSneaking()||!blackMirrorLocations.containsKey(p.getUniqueId())){
            Block target=p.getTargetBlockExact(getConfig().getInt("jakutsuki-skills.black-mirror.place-range",12));
            if(target==null){msg(p,"&5黒鏡 &fを置く地点へ視点を合わせてください。");return;}
            Location placed=target.getLocation().add(.5,1.05,.5);removeBlackMirror(p.getUniqueId());
            ArmorStand stand=p.getWorld().spawn(placed,ArmorStand.class);stand.setVisible(false);stand.setMarker(true);stand.setGravity(false);stand.setInvulnerable(true);stand.setCustomName(cc("&5&l黒鏡"));stand.setCustomNameVisible(true);stand.getEquipment().setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));
            blackMirrorLocations.put(p.getUniqueId(),placed);blackMirrorEntities.put(p.getUniqueId(),stand.getUniqueId());p.getWorld().playSound(placed,Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,.8f,.55f);msg(p,"&5黒鏡を設置した。 &7右クリックで転移できます。");return;
        }
        if(!ready(p,"jakutsuki_black_mirror",getConfig().getInt("jakutsuki-skills.black-mirror.cooldown-seconds",60)))return;
        Location dest=blackMirrorLocations.get(p.getUniqueId());if(dest==null||dest.getWorld()==null){msg(p,"黒鏡が設置されていません。");return;}dest=dest.clone();dest.setDirection(p.getLocation().getDirection());p.getWorld().spawnParticle(Particle.SMOKE_LARGE,p.getLocation(),45,.5,1,.5,.04);p.teleport(dest);p.getWorld().spawnParticle(Particle.PORTAL,dest,60,.6,1,.6,.2);p.getWorld().playSound(dest,Sound.ENTITY_ENDERMAN_TELEPORT,1f,.55f);if(training)trainingLog(p,"&5黒鏡へ転移");
    }
    private void removeBlackMirror(UUID owner){UUID id=blackMirrorEntities.remove(owner);if(id!=null){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}blackMirrorLocations.remove(owner);}
    private void removeBlackMirrors(){for(UUID id:new HashSet<>(blackMirrorEntities.keySet()))removeBlackMirror(id);}
    private void useJakutsukiSweep(Player p){if(!ready(p,"jakutsuki_sweep",getConfig().getInt("jakutsuki-skills.sweep.cooldown-seconds",16)))return;double radius=getConfig().getDouble("jakutsuki-skills.sweep.radius",7.0),damage=getConfig().getDouble("jakutsuki-skills.sweep.damage",7.0),half=Math.toRadians(getConfig().getDouble("jakutsuki-skills.sweep.angle-degrees",100.0)/2.0);Vector forward=p.getEyeLocation().getDirection().setY(0);if(forward.lengthSquared()<.01)forward=new Vector(0,0,1);forward.normalize();for(int i=0;i<=20;i++){double angle=-half+(half*2*i/20.0);Vector v=forward.clone().rotateAroundY(angle);for(double d=1;d<=radius;d+=1.0)p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,p.getLocation().add(0,1,0).add(v.clone().multiply(d)),1,0,0,0,0);}Collection<UUID> targets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:targets){if(id.equals(p.getUniqueId()))continue;Player q=Bukkit.getPlayer(id);if(q==null||!validSkillTarget(p,q,radius))continue;Vector to=q.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(to.lengthSquared()<.01)continue;if(forward.angle(to.normalize())<=half){dealFixedDamage(q,damage);q.setVelocity(to.normalize().multiply(.65).setY(.22));}}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity q)||q.getWorld()!=p.getWorld()||q.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;Vector to=q.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(to.lengthSquared()>.01&&forward.angle(to.normalize())<=half)dealFixedDamage(q,damage);}p.getWorld().playSound(p.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.2f,.6f);}
    private void useSnakefall(Player p){if(!ready(p,"jakutsuki_snakefall",getConfig().getInt("jakutsuki-skills.snakefall.cooldown-seconds",45)))return;int count=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.count",30)),life=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.lifetime-seconds",20));double radius=Math.max(3,getConfig().getDouble("jakutsuki-skills.snakefall.spawn-radius",22.0));List<Location> anchors=new ArrayList<>();for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q!=null&&!dead.contains(id)&&!escaped.contains(id)&&q.getWorld()==p.getWorld())anchors.add(q.getLocation());}for(UUID id:playerBots){Entity e=Bukkit.getEntity(id);if(e!=null&&e.getWorld()==p.getWorld())anchors.add(e.getLocation());}if(anchors.isEmpty())anchors.add(p.getLocation());Random r=new Random();for(int i=0;i<count;i++){Location base=anchors.get(r.nextInt(anchors.size()));Location at=base.clone().add((r.nextDouble()*2-1)*radius,0,(r.nextDouble()*2-1)*radius);int y=at.getWorld().getHighestBlockYAt(at.getBlockX(),at.getBlockZ(),HeightMap.MOTION_BLOCKING_NO_LEAVES);at.setY(Math.max(base.getY()-4,Math.min(base.getY()+4,y+1)));Silverfish snake=at.getWorld().spawn(at,Silverfish.class);snake.setCustomName(cc("&8蛇窟の蛇"));snake.setCustomNameVisible(false);snake.setRemoveWhenFarAway(false);snake.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"jakutsuki_snake");jakutsukiSnakes.add(snake.getUniqueId());Bukkit.getScheduler().runTaskLater(this,()->{jakutsukiSnakes.remove(snake.getUniqueId());if(snake.isValid())snake.remove();},life*20L);}p.getWorld().playSound(p.getLocation(),Sound.ENTITY_SILVERFISH_AMBIENT,1.5f,.55f);all("&8蛇窟姫の『蛇崩』――蛇が溢れ出した。");}
    private void removeJakutsukiSnakes(){for(UUID id:new HashSet<>(jakutsukiSnakes)){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}jakutsukiSnakes.clear();}
    private void useJakutsukiPiercingBlast(Player p){
        UUID id=p.getUniqueId();if(jakutsukiPierceCharging.contains(id)){msg(p,"&d黒の波動 &fは現在溜め中です。");return;}
        int cooldown=Math.max(1,getConfig().getInt("jakutsuki-skills.piercing-blast.cooldown-seconds",35));if(!ready(p,"jakutsuki_piercing_blast",cooldown))return;
        int chargeTicks=Math.max(5,getConfig().getInt("jakutsuki-skills.piercing-blast.charge-ticks",50));double range=Math.max(3,getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)),width=Math.max(.5,getConfig().getDouble("jakutsuki-skills.piercing-blast.half-width",2.0)),damage=Math.max(0,getConfig().getDouble("jakutsuki-skills.piercing-blast.damage",16.0));
        Vector direction=p.getEyeLocation().getDirection().normalize();Location origin=p.getEyeLocation().clone();jakutsukiPierceCharging.add(id);p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,chargeTicks+5,3,false,true));p.sendTitle(cc("&5&l蛇 穿"),cc("&7呪力を収束している……"),0,chargeTicks,5);p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1.0f,.55f);
        final int[] elapsed={0};final BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(!p.isOnline()||(!jakutsukiPierceCharging.contains(id))||(state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))){jakutsukiPierceCharging.remove(id);task[0].cancel();return;}
            elapsed[0]+=2;double progress=Math.min(1.0,elapsed[0]/(double)chargeTicks);Location muzzle=p.getEyeLocation().clone().add(direction.clone().multiply(1.0));p.getWorld().spawnParticle(Particle.REVERSE_PORTAL,muzzle,6,.25,.25,.25,.04);p.getWorld().spawnParticle(Particle.REDSTONE,muzzle,8,.22,.22,.22,0,new Particle.DustOptions(Color.fromRGB(150,20,180),1.4f));
            if(elapsed[0]%10==0)p.getWorld().playSound(p.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.7f,.65f+(float)(progress*.5));
            if(elapsed[0]<chargeTicks)return;task[0].cancel();jakutsukiPierceCharging.remove(id);fireJakutsukiPiercingBlast(p,origin,direction,range,width,damage);
        },0L,2L);
    }
    private void fireJakutsukiPiercingBlast(LivingEntity caster,Location origin,Vector direction,double range,double halfWidth,double damage){
        World world=origin.getWorld();if(world==null)return;Vector dir=direction.clone().normalize();Particle.DustOptions darkPurple=new Particle.DustOptions(Color.fromRGB(105,0,135),2.0f),crimson=new Particle.DustOptions(Color.fromRGB(190,0,65),1.5f);
        for(double d=0;d<=range;d+=.55){Location point=origin.clone().add(dir.clone().multiply(d));world.spawnParticle(Particle.REDSTONE,point,3,.28,.28,.28,0,darkPurple);world.spawnParticle(Particle.REDSTONE,point,2,.18,.18,.18,0,crimson);if(((int)(d*10))%22==0)world.spawnParticle(Particle.SQUID_INK,point,2,.18,.18,.18,.01);}
        world.playSound(caster.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,1.15f,.72f);world.playSound(caster.getLocation(),Sound.ENTITY_WITHER_SHOOT,.9f,.65f);
        Collection<LivingEntity> targets=new ArrayList<>();for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player q=Bukkit.getPlayer(id);if(q!=null)targets.add(q);}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity q&&q.isValid()&&!q.isDead())targets.add(q);}
        for(LivingEntity q:targets){if(q.getUniqueId().equals(caster.getUniqueId())||q.getWorld()!=world)continue;Vector rel=q.getLocation().add(0,1,0).toVector().subtract(origin.toVector());double along=rel.dot(dir);if(along<0||along>range)continue;Vector perpendicular=rel.clone().subtract(dir.clone().multiply(along));if(perpendicular.lengthSquared()>halfWidth*halfWidth)continue;dealFixedDamage(q,damage);Vector knock=dir.clone().multiply(getConfig().getDouble("jakutsuki-skills.piercing-blast.knockback",1.15));knock.setY(.22);q.setVelocity(q.getVelocity().add(knock));q.getWorld().spawnParticle(Particle.CRIT_MAGIC,q.getLocation().add(0,1,0),25,.45,.7,.45,.08);if(q instanceof Player qp)qp.sendActionBar(cc("&d黒の波動に貫かれた！"));}
    }
    private void useBotJakutsukiPiercingBlast(Mob bot,LivingEntity target){
        long now=System.currentTimeMillis();int chargeTicks=Math.max(5,getConfig().getInt("jakutsuki-skills.piercing-blast.charge-ticks",50));double range=Math.max(3,getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)),width=Math.max(.5,getConfig().getDouble("jakutsuki-skills.piercing-blast.half-width",2.0)),damage=Math.max(0,getConfig().getDouble("jakutsuki-skills.piercing-blast.damage",16.0));Vector direction=target.getLocation().add(0,1,0).toVector().subtract(bot.getEyeLocation().toVector()).normalize();Location origin=bot.getEyeLocation().clone();UUID id=bot.getUniqueId();jakutsukiPierceCharging.add(id);bot.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,chargeTicks+5,8,false,true));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1.0f,.55f);
        final int[] elapsed={0};final BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{if(!bot.isValid()||bot.isDead()||!jakutsukiPierceCharging.contains(id)||state!=GameState.RUNNING){jakutsukiPierceCharging.remove(id);task[0].cancel();return;}elapsed[0]+=2;Location muzzle=bot.getEyeLocation().clone();bot.getWorld().spawnParticle(Particle.REVERSE_PORTAL,muzzle,6,.25,.25,.25,.04);bot.getWorld().spawnParticle(Particle.REDSTONE,muzzle,8,.22,.22,.22,0,new Particle.DustOptions(Color.fromRGB(150,20,180),1.4f));if(elapsed[0]%10==0)bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.7f,.75f);if(elapsed[0]<chargeTicks)return;task[0].cancel();jakutsukiPierceCharging.remove(id);fireJakutsukiPiercingBlast(bot,origin,direction,range,width,damage);},0L,2L);
        botNextAbilityAt=now+oniBotAbilityCooldownMillis(Math.max(1,getConfig().getInt("jakutsuki-skills.piercing-blast.cooldown-seconds",35)));recoverOniSkillMomentum();
    }
    private void useJakutsukiRelease(Player p){boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());int remaining=Math.max(0,totalHearts-brokenHearts);if(!training&&remaining!=1){msg(p,"&4解放 &fは心臓が残り1つの時だけ使用できます。 &7(現在 "+remaining+"つ)");return;}if(!ready(p,"jakutsuki_release",getConfig().getInt("jakutsuki-skills.release.cooldown-seconds",9999)))return;int seconds=Math.max(1,getConfig().getInt("jakutsuki-skills.release.exit-seal-seconds",60));exitSealedUntil=System.currentTimeMillis()+seconds*1000L;int amp=Math.max(0,getConfig().getInt("jakutsuki-skills.release.strength-amplifier",1)),speed=Math.max(0,getConfig().getInt("jakutsuki-skills.release.speed-amplifier",1));p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,seconds*20,amp,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,seconds*20,speed,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,seconds*20,0,false,true));p.sendTitle(cc("&4&l解 放"),cc("&8蛇窟姫、狂化――脱出口封印 "+seconds+"秒"),5,45,10);all("&4&l蛇窟姫が『解放』した。 &c脱出口は"+seconds+"秒間封印される！");p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,1.2f,.65f);p.getWorld().spawnParticle(Particle.SQUID_INK,p.getLocation().add(0,1,0),100,1.8,1.4,1.8,.08);Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&System.currentTimeMillis()>=exitSealedUntil)all("&e蛇窟姫の封印が解け、脱出口が再び有効になった。");},seconds*20L);}

    @EventHandler public void onJakutsukiSnakeDamage(EntityDamageByEntityEvent e){if(e.getDamager() instanceof LivingEntity attacker)lastCombatAt.put(attacker.getUniqueId(),System.currentTimeMillis());if(e.getEntity() instanceof LivingEntity victim)lastCombatAt.put(victim.getUniqueId(),System.currentTimeMillis());
        if(e.getEntity() instanceof Silverfish snake&&jakutsukiSnakes.contains(snake.getUniqueId())){
            Entity raw=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();ItemStack held=null;
            if(raw instanceof Player p)held=p.getInventory().getItemInMainHand();else if(raw instanceof LivingEntity le&&playerBots.contains(le.getUniqueId()))held=le.getEquipment().getItemInMainHand();
            if(isJakutsukiSnakeSlash(held)){e.setCancelled(true);jakutsukiSnakes.remove(snake.getUniqueId());snake.getWorld().spawnParticle(Particle.SWEEP_ATTACK,snake.getLocation().add(0,.5,0),3,.25,.25,.25,0);snake.getWorld().spawnParticle(Particle.CRIT_MAGIC,snake.getLocation().add(0,.5,0),18,.35,.35,.35,.08);snake.getWorld().playSound(snake.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.9f,1.25f);snake.remove();return;}
        }
        if(!(e.getDamager() instanceof Silverfish snake)||!jakutsukiSnakes.contains(snake.getUniqueId())||!(e.getEntity() instanceof Player target))return;int ticks=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.glow-seconds",4))*20;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));target.sendActionBar(cc("&8蛇に噛まれた―― &f短時間、位置が露見する"));
    }

    private void useFoxFire(Player p){
        if(!ready(p,"dakko_fox_fire",getConfig().getInt("dakko-skills.fox-fire.cooldown-seconds",30)))return;
        double radius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0),damage=getConfig().getDouble("dakko-skills.fox-fire.fixed-damage",5.0);
        int fireTicks=getConfig().getInt("dakko-skills.fox-fire.fire-ticks",100);
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_FIRECHARGE_USE,1.2f,.65f);p.getWorld().spawnParticle(Particle.FLAME,p.getLocation().add(0,1,0),100,radius*.55,1.2,radius*.55,.08);p.getWorld().spawnParticle(Particle.LAVA,p.getLocation(),25,radius*.5,.7,radius*.5,.03);
        Collection<UUID> foxTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:foxTargets){Player q=Bukkit.getPlayer(id);if(id.equals(p.getUniqueId())||!validSkillTarget(p,q,radius))continue;q.setFireTicks(Math.max(q.getFireTicks(),fireTicks));dealFixedDamage(q,damage);q.sendActionBar(cc("&6狐火に焼かれている！"));}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.getWorld().equals(p.getWorld())||bot.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;bot.setFireTicks(Math.max(bot.getFireTicks(),fireTicks));dealFixedDamage(bot,damage);}
    }

    private void useHeavenlyArrival(Player p){
        int range=getConfig().getInt("dakko-skills.heavenly-arrival.target-range",24);
        Block target=p.getTargetBlockExact(range);if(target==null){msg(p,"天来する地点へ視点を合わせてください。");return;}
        if(!ready(p,"dakko_heavenly_arrival",getConfig().getInt("dakko-skills.heavenly-arrival.cooldown-seconds",35)))return;
        Location landing=target.getLocation().add(.5,1.05,.5);double height=getConfig().getDouble("dakko-skills.heavenly-arrival.launch-height",10.0);
        Location sky=landing.clone().add(0,height,0);sky.setY(Math.min(sky.getY(),p.getWorld().getMaxHeight()-2));sky.setDirection(p.getLocation().getDirection());
        heavenlyArrival.add(p.getUniqueId());p.teleport(sky);p.setFallDistance(0);p.setVelocity(new Vector(0,-1.35,0));p.getWorld().playSound(sky,Sound.ENTITY_PHANTOM_FLAP,1.2f,.55f);
        final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
        task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{ticks[0]++;if((state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))||!p.isOnline()||!heavenlyArrival.contains(p.getUniqueId())){heavenlyArrival.remove(p.getUniqueId());task[0].cancel();return;}p.getWorld().spawnParticle(Particle.FLAME,p.getLocation(),8,.25,.4,.25,.02);if((ticks[0]>4&&(p.isOnGround()||p.getLocation().getY()<=landing.getY()+.6))||ticks[0]>=60){p.teleport(landing);p.setFallDistance(0);heavenlyArrival.remove(p.getUniqueId());task[0].cancel();impactHeavenlyArrival(p);}},1L,1L);
    }

    private void impactHeavenlyArrival(Player p){
        double radius=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-radius",5.0),damage=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-damage",7.0);
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.2f,.65f);p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation(),5,.8,.4,.8,.05);p.getWorld().spawnParticle(Particle.FLAME,p.getLocation(),70,radius*.45,.8,radius*.45,.06);
        Collection<UUID> heavenlyTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:heavenlyTargets){if(id.equals(p.getUniqueId()))continue;Player q=Bukkit.getPlayer(id);if(!validSkillTarget(p,q,radius))continue;dealFixedDamage(q,damage);Vector push=q.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);q.setVelocity(push.normalize().multiply(.85).setY(.45));}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.getWorld().equals(p.getWorld())||bot.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;dealFixedDamage(bot,damage);Vector push=bot.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);bot.setVelocity(push.normalize().multiply(.85).setY(.45));}
    }

    private boolean validSkillTarget(LivingEntity oniEntity,Player target,double radius){return target!=null&&!dead.contains(target.getUniqueId())&&!escaped.contains(target.getUniqueId())&&!isDownEscape(target.getUniqueId())&&target.getWorld().equals(oniEntity.getWorld())&&target.getLocation().distanceSquared(oniEntity.getLocation())<=radius*radius;}
    private void dealFixedDamage(LivingEntity target,double damage){if(damage<=0)return;if(state==GameState.RUNNING&&target instanceof Player gracePlayer&&players.contains(gracePlayer.getUniqueId())&&hasPlayerHitGrace(gracePlayer.getUniqueId()))return;if(state==GameState.WAITING&&target instanceof Player trainingTarget&&isInTrainingArea(trainingTarget.getLocation())){trainingLog(trainingTarget,"&c被ダメージ想定 "+String.format(Locale.US,"%.1f",damage)+" &7(体力は減りません)");return;}if(playerBots.contains(target.getUniqueId())&&isDownEscape(target.getUniqueId()))return;if(playerBots.contains(target.getUniqueId())&&target.getHealth()-damage<=0&&tryConsumePlayerBotExtraLife(target,damage))return;if(playerBots.contains(target.getUniqueId())&&hasPlayerBotPassive(target.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-damage-taken-multiplier",0.90);if(target instanceof Player player){if(healingTasks.containsKey(player.getUniqueId()))stopHealing(player,"&c攻撃を受け、回復が中断された。");if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(player.getUniqueId()+":UNYIELDING",0L)&&target.getHealth()-damage<=0){target.setHealth(1);player.sendActionBar(cc("&6不退転 &7――致死ダメージを耐えた"));return;}if(isDownEscape(player.getUniqueId()))return;if(tryConsumeExtraLife(player,damage))return;}target.setHealth(Math.max(0,target.getHealth()-damage));if(state==GameState.RUNNING&&target instanceof Player hitPlayer&&players.contains(hitPlayer.getUniqueId())&&hitPlayer.getHealth()>0)grantPlayerHitGrace(hitPlayer);checkPredation(target);}
    private boolean hasPlayerHitGrace(UUID id){return System.currentTimeMillis()<playerHitGraceUntil.getOrDefault(id,0L);}
    private void grantPlayerHitGrace(Player p){
        if(!getConfig().getBoolean("player-hit-grace.enabled",true)||!players.contains(p.getUniqueId())||dead.contains(p.getUniqueId())||escaped.contains(p.getUniqueId()))return;
        int ticks=Math.max(1,getConfig().getInt("player-hit-grace.duration-ticks",60));
        int amplifier=Math.max(0,getConfig().getInt("player-hit-grace.speed-amplifier",1));
        playerHitGraceUntil.put(p.getUniqueId(),System.currentTimeMillis()+ticks*50L);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amplifier,false,true));
        p.sendActionBar(cc("&b被弾加速 &7――3秒間、加速＋無敵"));
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,.2,0),16,.35,.12,.35,.04);
    }
    private void checkPredation(LivingEntity victim){if(!hasOniPassive(OniPassiveSkill.PREDATION)||predationTriggered.contains(victim.getUniqueId()))return;double max=victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(victim.getHealth()>max*.30)return;predationTriggered.add(victim.getUniqueId());double amount=Math.max(0,getConfig().getDouble("oni-passive-skills.predation-stamina-recovery",6.0));if(oniBot!=null)botStamina=Math.min(20,botStamina+amount);else if(oni!=null){stamina.put(oni,Math.min(20,stamina.getOrDefault(oni,20.0)+amount));Player oniPlayer=Bukkit.getPlayer(oni);if(oniPlayer!=null){oniPlayer.setFoodLevel((int)Math.ceil(stamina.get(oni)));oniPlayer.sendActionBar(cc("&4捕食 &7――スタミナを回復した"));}}}
    private void trackHeart(Player p){Set<String> trackable=new HashSet<>(heartHp.keySet());trackable.addAll(fakeHeartKeys);Location nearest=trackable.stream().map(LocationStore::decode).filter(Objects::nonNull).filter(l->l.getWorld().equals(p.getWorld())).min(Comparator.comparingDouble(l->l.distanceSquared(p.getLocation()))).orElse(null);if(nearest==null){p.setCompassTarget(p.getWorld().getSpawnLocation());msg(p,"残っている心臓はありません。");return;}p.setCompassTarget(nearest.clone().add(.5,.5,.5));msg(p,"最寄りの心臓まで約 &e"+(int)nearest.distance(p.getLocation())+"m");}

    @EventHandler public void onDamage(EntityDamageByEntityEvent e){
        if(state!=GameState.RUNNING)return;
        Entity rawAttacker=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();
        boolean attackerIsBot=oniBot!=null&&rawAttacker.getUniqueId().equals(oniBot);
        boolean attackerIsOni=(oni!=null&&rawAttacker.getUniqueId().equals(oni))||attackerIsBot;
        boolean executionMarked=attackerIsOni&&hasOniPassive(OniPassiveSkill.EXECUTION)&&e.getEntity() instanceof LivingEntity marked&&marked.hasPotionEffect(PotionEffectType.GLOWING);
        boolean victimIsOni=(oni!=null&&e.getEntity().getUniqueId().equals(oni))||(oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot));
        if(attackerIsOni&&e.getEntity() instanceof Player markedPlayer&&players.contains(markedPlayer.getUniqueId())&&hasOniPassive(OniPassiveSkill.BLOOD_MARK)){
            UUID mid=markedPlayer.getUniqueId();int marks=bloodMarkTokens.getOrDefault(mid,0);
            if(marks>=3){bloodMarkTokens.put(mid,0);long cut=Math.max(1000,getConfig().getLong("oni-passive-skills.blood-mark-cooldown-recovery-millis",6000));if(oniBot!=null)botNextAbilityAt=Math.max(System.currentTimeMillis(),botNextAbilityAt-cut);if(oni!=null){String prefix=oni.toString()+":";for(Map.Entry<String,Long> ce:new HashMap<>(cooldowns).entrySet())if(ce.getKey().startsWith(prefix))cooldowns.put(ce.getKey(),Math.max(System.currentTimeMillis(),ce.getValue()-cut));}if(rawAttacker instanceof Player op)op.sendActionBar(cc("&4血印 &7――3印回収、鬼スキルCTを短縮"));markedPlayer.sendActionBar(cc("&4血印が回収された――"));}
            else{marks++;bloodMarkTokens.put(mid,marks);markedPlayer.sendActionBar(cc("&4血印 &7―― "+marks+"/3"));if(rawAttacker instanceof Player op)op.sendActionBar(cc("&4血印 &7――"+markedPlayer.getName()+" "+marks+"/3"));}
        }
        if(playerBots.contains(rawAttacker.getUniqueId())&&!victimIsOni){e.setCancelled(true);return;}
        if(e.getEntity() instanceof Player victim){
            boolean victimIsPlayer=players.contains(victim.getUniqueId());
            if((attackerIsOni&&victimIsOni)||(!attackerIsOni&&victimIsPlayer&&rawAttacker instanceof Player)&&!getConfig().getBoolean("friendly-fire",false)){
                e.setCancelled(true);
                return;
            }
            if(attackerIsBot){e.setDamage(getConfig().getDouble("bot.attack-damage",7.0));if(rawAttacker instanceof LivingEntity botAttacker&&isKishinIronBodyActive(botAttacker))e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("kishin-skills.iron-body.attack-damage-multiplier",1.35)));}
            if(executionMarked)e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("oni-passive-skills.execution-damage-multiplier",1.15)));
            long now=System.currentTimeMillis();
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.BLOOD_FRENZY)){UUID vid=victim.getUniqueId();long prev=oniLastHitAt.getOrDefault(vid,0L);int hits=now-prev<=Math.max(500,getConfig().getLong("oni-passive-skills.blood-frenzy-window-millis",3500))?Math.min(3,oniBloodFrenzyHits.getOrDefault(vid,0)+1):1;oniBloodFrenzyHits.put(vid,hits);oniLastHitAt.put(vid,now);e.setDamage(e.getDamage()*(1.0+hits*Math.max(0,getConfig().getDouble("oni-passive-skills.blood-frenzy-damage-step",.05))));}
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.SKILL_SEAL)){oniSkillSealUntil.put(victim.getUniqueId(),now+Math.max(500,getConfig().getLong("oni-passive-skills.skill-seal-millis",1500)));victim.sendActionBar(cc("&5封殺 &7――短時間スキル使用不可"));}
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.FINISHER_CHASE)&&victim.getHealth()<=victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*Math.max(.1,Math.min(.9,getConfig().getDouble("oni-passive-skills.finisher-health-ratio",.40)))){LivingEntity hunter=getOniEntity();if(hunter!=null)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.finisher-speed-ticks",45)),0,false,true));}UUID victimId=victim.getUniqueId();if(attackerIsOni&&victimIsPlayer&&hasPassive(victimId,PassiveSkill.PARRY)&&now-parrySneakAt.getOrDefault(victimId,0L)<=getConfig().getLong("technical-passives.parry-window-millis",320)&&technicalReady(victimId,"PARRY",getConfig().getLong("technical-passives.parry-cooldown-seconds",20))){e.setDamage(e.getDamage()*getConfig().getDouble("technical-passives.parry-damage-multiplier",.5));double parryStamina=Math.max(0,stamina.getOrDefault(victimId,20.0)-getConfig().getDouble("technical-passives.parry-stamina-cost",4.0));stamina.put(victimId,parryStamina);victim.setFoodLevel((int)Math.ceil(parryStamina));Vector side=victim.getEyeLocation().getDirection().setY(0);side=new Vector(-side.getZ(),0,side.getX()).normalize().multiply(.65).setY(.2);victim.setVelocity(side);victim.playSound(victim.getLocation(),Sound.ITEM_SHIELD_BLOCK,1f,1.25f);victim.sendActionBar(cc("&b受け流し &7――攻撃を逸らした"));}if(attackerIsOni&&victimIsPlayer&&hasPassive(victimId,PassiveSkill.CORNERED_RAT)&&now-reversalAt.getOrDefault(victimId,0L)<=getConfig().getLong("technical-passives.cornered-rat-window-millis",550)&&technicalReady(victimId,"CORNERED_RAT",getConfig().getLong("technical-passives.cornered-rat-cooldown-seconds",18))){victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,getConfig().getInt("technical-passives.cornered-rat-speed-ticks",45),1,false,true));victim.sendActionBar(cc("&e窮鼠 &7――反転から加速"));}
            if(attackerIsOni&&repairingHeart.containsKey(victim.getUniqueId()))stopRepair(victim,"&c攻撃を受け、心臓への干渉が中断された。");
        }
        if(executionMarked&&!(e.getEntity() instanceof Player))e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("oni-passive-skills.execution-damage-multiplier",1.15)));
        if(victimIsOni&&playerBots.contains(rawAttacker.getUniqueId())&&rawAttacker instanceof LivingEntity attackerBot&&isJakutsukiSnakeSlash(attackerBot.getEquipment().getItemInMainHand()))e.setDamage(getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0));
        if(victimIsOni&&rawAttacker instanceof Player attacker){
            if(!players.contains(attacker.getUniqueId())){
                e.setCancelled(true);
                return;
            }
            String heldAction=actionOf(attacker.getInventory().getItemInMainHand());
            if("equipment:shinai".equals(heldAction))e.setDamage(getConfig().getDouble("equipment.shinai-damage",3.0));
            else if("equipment:iron_bat".equals(heldAction))e.setDamage(getConfig().getDouble("equipment.iron-bat-damage",5.0));
            else if(oniType==OniType.JAKUTSUKI&&"jakutsuki_snake_slash".equals(heldAction))e.setDamage(getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0));
            if(hasPassive(attacker.getUniqueId(),PassiveSkill.ATTACK_BOOST))e.setDamage(e.getDamage()*1.20);if(hasPassive(attacker.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))e.setDamage(e.getDamage()*getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10));
        }
    }
    @EventHandler public void onPlayerBotTarget(EntityTargetLivingEntityEvent e){if(state==GameState.RUNNING&&playerBots.contains(e.getEntity().getUniqueId())&&e.getTarget()!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onOniDamageResolved(EntityDamageByEntityEvent e){Entity raw=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();boolean fromOni=(oni!=null&&raw.getUniqueId().equals(oni))||(oniBot!=null&&raw.getUniqueId().equals(oniBot));if(!fromOni||!(e.getEntity() instanceof LivingEntity victim))return;Bukkit.getScheduler().runTask(this,()->{if(victim.isValid()&&!victim.isDead()){if(victim instanceof Player p&&players.contains(p.getUniqueId()))grantPlayerHitGrace(p);checkPredation(victim);}});}
    @EventHandler public void onAnyDamage(EntityDamageEvent e){if(e.getEntity() instanceof LivingEntity damaged)lastCombatAt.put(damaged.getUniqueId(),System.currentTimeMillis());if(state==GameState.WAITING&&trainingDummy!=null&&e.getEntity().getUniqueId().equals(trainingDummy)){double amount=Math.max(0,e.getFinalDamage());e.setCancelled(true);for(UUID id:trainingPlayers){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld().equals(e.getEntity().getWorld())&&p.getLocation().distanceSquared(e.getEntity().getLocation())<=625)trainingLog(p,"&fダミーに &c"+String.format(Locale.US,"%.1f",amount)+" &fダメージ");}return;}if(e.getCause()==EntityDamageEvent.DamageCause.FALL&&(heavenlyArrival.contains(e.getEntity().getUniqueId())||bugeiActive.contains(e.getEntity().getUniqueId()))){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(state==GameState.WAITING&&e.getEntity() instanceof Player){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(state!=GameState.RUNNING)return;if(e.getEntity() instanceof LivingEntity escapingBot&&playerBots.contains(escapingBot.getUniqueId())&&isDownEscape(escapingBot.getUniqueId())){e.setCancelled(true);escapingBot.setFallDistance(0);return;}if(e.getEntity() instanceof LivingEntity lifeBot&&playerBots.contains(lifeBot.getUniqueId())&&e.getFinalDamage()>=lifeBot.getHealth()&&tryConsumePlayerBotExtraLife(lifeBot,e.getFinalDamage())){e.setCancelled(true);return;}if(e.getEntity() instanceof Player gracePlayer&&players.contains(gracePlayer.getUniqueId())&&hasPlayerHitGrace(gracePlayer.getUniqueId())){e.setCancelled(true);gracePlayer.setFallDistance(0);return;}if(e.getEntity() instanceof Player escapingPlayer&&players.contains(escapingPlayer.getUniqueId())&&isDownEscape(escapingPlayer.getUniqueId())){e.setCancelled(true);escapingPlayer.setFallDistance(0);return;}if(e.getEntity() instanceof Player divinePlayer&&players.contains(divinePlayer.getUniqueId())&&hasPassive(divinePlayer.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))e.setDamage(e.getDamage()*getConfig().getDouble("passive-skills.divine-technique-damage-taken-multiplier",0.90));if(e.getEntity() instanceof Player unyieldingPlayer&&players.contains(unyieldingPlayer.getUniqueId())&&System.currentTimeMillis()<timedSkillUntil.getOrDefault(unyieldingPlayer.getUniqueId()+":UNYIELDING",0L)&&e.getFinalDamage()>=unyieldingPlayer.getHealth()){e.setDamage(Math.max(0,unyieldingPlayer.getHealth()-1));unyieldingPlayer.sendActionBar(cc("&6不退転 &7――致死ダメージを耐えた"));}if(!e.isCancelled()&&e.getCause()==EntityDamageEvent.DamageCause.FALL&&e.getEntity() instanceof Player landingPlayer&&players.contains(landingPlayer.getUniqueId())&&selectedSkill.getOrDefault(landingPlayer.getUniqueId(),PlayerSkill.SPRINT)==PlayerSkill.SAFE_LANDING&&e.getFinalDamage()>0&&readyAutomatic(landingPlayer,"SAFE_LANDING",PlayerSkill.SAFE_LANDING.cooldown)){e.setCancelled(true);landingPlayer.setFallDistance(0);landingPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(1,getConfig().getInt("skills.safe-landing-speed-ticks",80)),Math.max(0,getConfig().getInt("skills.safe-landing-speed-amplifier",2)),false,true));landingPlayer.getWorld().spawnParticle(Particle.CLOUD,landingPlayer.getLocation(),24,.45,.12,.45,.06);landingPlayer.playSound(landingPlayer.getLocation(),Sound.ENTITY_HORSE_LAND,.9f,1.35f);landingPlayer.sendTitle("",cc("&e&l安定着地 &7――速度上昇III"),0,24,8);return;}if(e.getEntity() instanceof Player player&&e.getFinalDamage()>0){if(healingTasks.containsKey(player.getUniqueId()))stopHealing(player,"&c攻撃を受け、回復が中断された。");if(chestOpeningTasks.remove(player.getUniqueId())!=null)player.sendActionBar(cc("&c攻撃を受け、チェストの開錠が中断された。"));if(players.contains(player.getUniqueId())&&e.getFinalDamage()>=player.getHealth()&&tryConsumeExtraLife(player,e.getFinalDamage())){e.setCancelled(true);return;}}if(brokenHearts>=totalHearts)return;boolean protectedOni=(oni!=null&&e.getEntity().getUniqueId().equals(oni))||(oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot));if(protectedOni)e.setDamage(e.getDamage()*Math.max(0,Math.min(1,getConfig().getDouble("oni-heart-protection.damage-taken-multiplier",0.25))));}
    @EventHandler public void onFoodChange(FoodLevelChangeEvent e){if(!(e.getEntity() instanceof Player p))return;if(state==GameState.WAITING){e.setCancelled(true);p.setFoodLevel(20);p.setSaturation(20);return;}if(state==GameState.RUNNING&&participants.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void onJoin(PlayerJoinEvent e){Player p=e.getPlayer();migrateLegacyJakutsukiNames(p);if(state==GameState.RUNNING&&isDownEscape(p.getUniqueId())){for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(p.getUniqueId()))viewer.hidePlayer(this,p);p.setCollidable(false);return;}if(state!=GameState.WAITING)return;p.setFoodLevel(20);p.setSaturation(20);p.setFallDistance(0);p.setFireTicks(0);if(isInTrainingArea(p.getLocation()))Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&isInTrainingArea(p.getLocation()))enterTraining(p);},1);}
    @EventHandler public void onCombust(EntityCombustEvent e){if(state==GameState.RUNNING&&((oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot))||playerBots.contains(e.getEntity().getUniqueId())))e.setCancelled(true);}
    @EventHandler public void onEntityDeath(EntityDeathEvent e){if(state!=GameState.RUNNING)return;UUID id=e.getEntity().getUniqueId();if(oniBot!=null&&id.equals(oniBot)){e.getDrops().clear();e.setDroppedExp(0);oniBot=null;end(false,"&c鬼 討 滅――ぷれいやーの勝利");return;}if(playerBots.contains(id)){restorePlayerBotDownEscape(id);checkPredation(e.getEntity());e.getDrops().clear();e.setDroppedExp(0);deadPlayerBots.add(id);clearPlayerBotWork(id);all("&7ぷれいやーBot は鬼に喰われた……");checkPlayerOutcome();}}
    @EventHandler public void onDeath(PlayerDeathEvent e){if(state!=GameState.RUNNING||!participants.contains(e.getPlayer().getUniqueId()))return;e.getDrops().clear();e.setDeathMessage(null);if(!e.getPlayer().getUniqueId().equals(oni))checkPredation(e.getPlayer());Bukkit.getScheduler().runTask(this,()->{Player p=e.getPlayer();p.spigot().respawn();p.setGameMode(GameMode.SPECTATOR);if(p.getUniqueId().equals(oni))end(false,"&c鬼 討 滅――ぷれいやーの勝利");else{dead.add(p.getUniqueId());downEscapeUntil.remove(p.getUniqueId());restoreDownEscapeVisual(p);chased.remove(p.getUniqueId());leaveChaseBgmNow(p);all("&7"+p.getName()+" は鬼に喰われた……");checkPlayerOutcome();}});}
    @EventHandler public void onQuit(PlayerQuitEvent e){if(state!=GameState.RUNNING)return;UUID id=e.getPlayer().getUniqueId();if(id.equals(oni))end(false,"鬼が退出したため、ぷれいやーの勝利");else if(players.contains(id)){dead.add(id);checkPlayerOutcome();}}

    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("join","leave","skills","skill","gmbook","onimenu","lobbyall","ally","arenasetup","skillchest","onichest","lobbyhide","forceoni","oniblock","practicechest","practiceheart","training","heartmode","marker","markers","chest","chests","finaltest","bgm","bgmtest","bgmstop","botgame","onibotgame","aibotmatch","botmatch","testbook","testdestroy","set","heart","start","stop","status","exlock");if(a.length==2&&a[0].equalsIgnoreCase("finaltest"))return List.of("clear");if(a.length==2&&a[0].equalsIgnoreCase("ally")){List<String> presets=new ArrayList<>(List.of("status","list","clear","IGAMI_KYOYA","AZANAMI_MISAKI","ARIKAWA_FUUKA","AZANAMI_REN","MEDIC","AKASAKA_HIIRO","KAGAYA_RION","AMANAI_IONA"));if(!isAzakujiLocked())presets.add("AZAKUJI_HIRO");return presets;}if(a.length==2&&a[0].equalsIgnoreCase("arenasetup"))return List.of("info","clear");if(a.length==2&&a[0].equalsIgnoreCase("skill"))return List.of("sprint","invisible","smoke","strike","heal","obsession","landing");if(a.length==2&&a[0].equalsIgnoreCase("heartmode"))return List.of("random","manual");if(a.length==2&&a[0].equalsIgnoreCase("bgm"))return List.of("onigame:final_phase","off");if(a.length==2&&a[0].equalsIgnoreCase("set"))return List.of("lobby","player","oni","exit");if(a.length==2&&(a[0].equalsIgnoreCase("start")||a[0].equalsIgnoreCase("botgame")||a[0].equalsIgnoreCase("onibotgame")||a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch")))return isJakutsukiLocked()?List.of("dakko","kishin"):List.of("dakko","kishin","jakutsuki");if(a.length==2&&a[0].equalsIgnoreCase("exlock"))return List.of("azakuji","jakutsuki","status");if(a.length==3&&a[0].equalsIgnoreCase("exlock"))return List.of("lock","unlock","status");if(a.length==2&&a[0].equalsIgnoreCase("forceoni")){List<String> names=new ArrayList<>();names.add("clear");for(Player p:Bukkit.getOnlinePlayers())names.add(p.getName());return names;}if(a.length==2&&(a[0].equalsIgnoreCase("oniblock")||a[0].equalsIgnoreCase("practicechest")||a[0].equalsIgnoreCase("practiceheart")))return List.of("clear");if(a.length==2&&a[0].equalsIgnoreCase("training"))return List.of("pos1","pos2","clear","info","dummy","refresh");if(a.length==3&&a[0].equalsIgnoreCase("training")&&a[1].equalsIgnoreCase("dummy"))return List.of("clear");if(a.length==3&&(a[0].equalsIgnoreCase("onibotgame")||a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch")))return List.of("1","2","3","4","5","6","7","8");return List.of();}
}
