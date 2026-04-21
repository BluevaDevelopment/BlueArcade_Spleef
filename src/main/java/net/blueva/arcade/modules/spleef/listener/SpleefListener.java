package net.blueva.arcade.modules.spleef.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.spleef.game.SpleefGameManager;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SpleefListener implements Listener {

    private final SpleefGameManager gameManager;

    public SpleefListener(SpleefGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            if (!context.isInsideBounds(event.getTo())) {
                Location spawn = context.getArenaAPI().getRandomSpawn();
                if (spawn != null) {
                    player.teleport(spawn);
                }
            }
            return;
        }

        if (!context.isInsideBounds(event.getTo())) {
            gameManager.handlePlayerElimination(player);
            return;
        }

        Location boundsMin = context.getArenaAPI().getBoundsMin();
        Location boundsMax = context.getArenaAPI().getBoundsMax();
        double minY = Math.min(boundsMin.getY(), boundsMax.getY());
        if (event.getTo().getY() < minY - 1) {
            gameManager.handlePlayerElimination(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        Material type = event.getBlock().getType();
        if (type == Material.SNOW_BLOCK || type == Material.SNOW) {
            event.setCancelled(false);
            event.setDropItems(false);
            event.getBlock().setType(Material.AIR);
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
            gameManager.handleSnowBreak(player);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        if (!(projectile instanceof Snowball)) {
            return;
        }

        if (!(projectile.getShooter() instanceof Player shooter)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(shooter);
        if (context == null || context.getPhase() != GamePhase.PLAYING || !context.isPlayerPlaying(shooter)) {
            return;
        }

        if (event.getHitBlock() != null) {
            Material hitType = event.getHitBlock().getType();
            if (hitType == Material.SNOW_BLOCK || hitType == Material.SNOW) {
                event.getHitBlock().getWorld().playEffect(event.getHitBlock().getLocation(), Effect.STEP_SOUND, hitType);
                event.getHitBlock().setType(Material.AIR);
                gameManager.handleSnowBreak(shooter);
            }
            return;
        }

        if (event.getHitEntity() instanceof Player target) {
            if (!context.isPlayerPlaying(target)) {
                return;
            }

            Vector knockback = projectile.getVelocity();
            if (knockback.lengthSquared() > 0) {
                Vector push = knockback.normalize().multiply(0.6);
                target.setVelocity(target.getVelocity().add(push));
            }
        }
    }
}
