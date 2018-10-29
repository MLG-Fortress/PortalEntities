package to.us.tf.PortalEntities;

import com.destroystokyo.paper.ParticleBuilder;
import com.matejdro.bukkit.portalstick.Portal;
import com.matejdro.bukkit.portalstick.PortalStick;
import com.matejdro.bukkit.portalstick.events.PlayerPortalGunShootEvent;
import de.V10lator.PortalStick.V10Location;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by RoboMWM on 11/22/2016.
 * What a dumb plogen name, u come up wit a bettar 1
 */
public class PortalEntities extends JavaPlugin implements Listener
{
    PortalStick portalStick;
    static PortalEntities instance; //boo

    public void onEnable()
    {
        portalStick = (PortalStick)getServer().getPluginManager().getPlugin("PortalStick");
        getServer().getPluginManager().registerEvents(this, this);
        instance = this;
    }

    /**
     * Use at beginning of every event handler
     * @return if world does not use portals
     */
    public boolean disabledWorld(World world)
    {
        return portalStick.config.DisabledWorlds.contains(world.getName());
    }

    public boolean disabledWorld(Location location)
    {
        return disabledWorld(location.getWorld());
    }

    public void smartTrackEntity(Entity entity)
    {
        if (entity.getType() == EntityType.PLAYER)
            return;
        if (!entity.hasMetadata("TRACKED") && isNearPortal(entity.getLocation()))
        {
            trackEntity(entity, false);
        }
    }

    public boolean isNearPortal(Location location)
    {
        World world = location.getWorld();
        if(portalStick.config.DisabledWorlds.contains(location.getWorld().getName()))
            return false;
        for (Portal portal : portalStick.portalManager.portals)
        {
            Location portalLocation = convertV10Location(portal.inside[0]);
            try
            {
                if (location.distanceSquared(portalLocation) < 49) //7 blocks
                    return true;
                continue;
            }
            catch (Exception e)
            {
                continue; //Just skip if there's an issue (null, not same world, etc.)
            }
        }
        return false;
    }

    public Location convertV10Location(V10Location v10Location)
    {
        return v10Location.getHandle();
    }

    public void trackEntity(Entity entity)
    {
        trackEntity(entity, true);
    }

    public void trackEntity(Entity entity, boolean check)
    {
        if (check)
        {
            if (entity.getType() == EntityType.PLAYER)
                return;
            if (entity.hasMetadata("TRACKED"))
                return;
        }
        entity.setMetadata("TRACKED", new FixedMetadataValue(this, true));
        new BukkitRunnable()
        {
            //Amount of ticks to track this entity for
            //int ticks = 200;
            Location previousLocation = entity.getLocation();
            public void run()
            {
                if (entity.isDead() || !entity.isValid())
                {
                    this.cancel();
                    return;
                }
                try
                {
                    if (entity.getLocation().distanceSquared(previousLocation) == 0)
                        return; //Don't call if it didn't move
                }
                catch (Exception e) //Somehow moved to another world, or otherwise erroring
                {
                    this.cancel();
                    entity.removeMetadata("TRACKED", PortalEntities.instance);
                    return;
                }

                Location to = portalStick.entityManager.onEntityMove(entity, previousLocation, entity.getLocation(), false);
                if (to != null)
                {
                    entity.teleport(to);
                    previousLocation = to;
                }
                else
                {
                    previousLocation = entity.getLocation();
                }

//                if (--ticks < 0)
//                {
//                    entity.removeMetadata("TRACKED", PortalEntities.instance);
//                    this.cancel();
//                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onBlockFall(EntityChangeBlockEvent event)
    {
        if (disabledWorld(event.getBlock().getLocation())) return;

        if (event.getTo() == Material.AIR && event.getEntityType() == EntityType.FALLING_BLOCK)
            smartTrackEntity(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onItemSpawn(ItemSpawnEvent event)
    {
        if (disabledWorld(event.getLocation())) return;
        smartTrackEntity(event.getEntity());
    }

    //[22:07:03] RoboMWM: is EntityChangeBlockEvent supposed to fire whenever a block turns into a FallingBlock (or primedTNT entity)? Seems to only do so when a blockphysicsevent is called on the block (and it's supposed to fall) or if it's placed in creative mode.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerPlaceABlockThatFalls(BlockPlaceEvent event)
    {
        if (disabledWorld(event.getBlock().getLocation())) return;
        Block block = event.getBlock();
        switch (block.getType())
        {
            case SAND:
            case GRAVEL:
                new BukkitRunnable()
                {
                    public void run()
                    {
                        for (Entity entity : block.getLocation().getChunk().getEntities())
                        {
                            if (entity.getType() == EntityType.FALLING_BLOCK)
                                smartTrackEntity(entity);
                        }
                    }
                }.runTaskLater(this, 1L);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerIgniteTNT(PlayerInteractEvent event)
    {
        Block block = event.getClickedBlock();
        if (disabledWorld(block.getLocation())) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getMaterial() == Material.FLINT_AND_STEEL && block.getType() == Material.TNT)
        {
            new BukkitRunnable()
            {
                public void run()
                {
                    for (Entity entity : block.getLocation().getChunk().getEntities())
                    {
                        if (entity.getType() == EntityType.PRIMED_TNT)
                            smartTrackEntity(entity);
                    }
                }
            }.runTaskLater(this, 1L);
        }
    }

    //We use a plugin to restore the old TNT behavior (ignite when destroyed)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerIgniteTNTByBreaking(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        if (disabledWorld(block.getLocation())) return;

        if (event.getBlock().getType() == Material.TNT)
        {
            new BukkitRunnable()
            {
                public void run()
                {
                    for (Entity entity : block.getLocation().getChunk().getEntities())
                    {
                        if (entity.getType() == EntityType.PRIMED_TNT)
                            smartTrackEntity(entity);
                    }
                }
            }.runTaskLater(this, 1L);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onProjectileSomething(ProjectileLaunchEvent event)
    {
        if (disabledWorld(event.getEntity().getLocation())) return;

        trackEntity(event.getEntity());
    }


// [22:53:46] RoboMWM: hmmm thoughts on making an "EntityMoveEvent" in a plugin via creating a runnable for each entity, checking location on each tick and firing event if distance != 0 ??????????
// [22:54:14] Yamakaja: RoboMWM, lol ?
// [22:54:25] %electronicboy: I think I just died a lil inside

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onMobSpawn(EntitySpawnEvent event)
    {
        if (disabledWorld(event.getLocation())) return;

        trackEntity(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onChunkLoad(ChunkLoadEvent event)
    {
        if (disabledWorld(event.getWorld())) return;

        if (event.isNewChunk())
            return;
        for (Entity entity : event.getChunk().getEntities())
        {
            if (entity instanceof LivingEntity) //Yea I don't really care about armorstands and the like, sorry
            {
                trackEntity(entity);
            }
        }
    }

    /**
     * Extra feature
     * TODO: add color to event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerShootsPortal(PlayerPortalGunShootEvent event)
    {
        if (event.getBlocksInLineOfSight().size() < 2)
            return;
        //double red = Float.MIN_NORMAL; //Double.MIN_NORMAL is too small
        //double green = 0D;
        //double blue = 0D;
        Color color = null;
        switch (event.getAction())
        {
            case LEFT_CLICK_AIR:
                //blue = 1D;
                //red = 0.2D;
                //green = 0.4D;
                color = Color.BLUE;
            case LEFT_CLICK_BLOCK:
                event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), "fortress.portal.blue", SoundCategory.PLAYERS, 1.0f, 1.0f);
                break;
            case RIGHT_CLICK_AIR:
                //red = 1D;
                //green = 0.5D;
                color = Color.ORANGE;
            case RIGHT_CLICK_BLOCK:
                event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), "fortress.portal.orange", SoundCategory.PLAYERS, 1.0f, 1.0f);
                break;
            default:
                return;
        }

        if (color == null)
            return;

        final Color finalColor = color;
        new BukkitRunnable()
        {
            World world = event.getPlayer().getWorld();
            Block block = event.getBlocksInLineOfSight().get(event.getBlocksInLineOfSight().size() - 2);
            Iterator<Vector> vectorIterator = calcLine(event.getPlayer().getLocation().add(0D, 1D, 0D).toVector(), block.getLocation().toVector()).iterator();

            public void run()
            {
                if (!vectorIterator.hasNext())
                {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 9 && vectorIterator.hasNext(); i++)
                {
                    if (i % 3 != 0)
                    {
                        vectorIterator.next();
                        continue;
                    }
                    ParticleBuilder particle = Particle.REDSTONE.builder().color(finalColor);
                    world.spawnParticle(particle.particle(), vectorIterator.next().toLocation(world), 1, particle.data());
                    //world.spawnParticle(particle.builder().color(), vectorIterator.next().toLocation(world), 0, r, g, b, 1D); //toLocation can be rewritten async (List of Locations, then schedule sync task for spawnParticle)
                }
            }
        }.runTaskTimerAsynchronously(this, 1L, 1L);
    }

    //https://www.spigotmc.org/threads/how-to-make-a-straight-particle-line.156411/#post-1661911
    private List<Vector> calcLine(Vector vec1, Vector vec2) {
        List<Vector> vectors = new ArrayList<>();
        double length = vec1.distance(vec2) - 1;
        int points = (int) (length / 1.0D) + 1;
        double gap = length / (points - 1);
        Vector gapVector = vec2.clone().subtract(vec1).normalize().multiply(gap);
        for (int i = 1; i < points; i++) {
            vectors.add(vec1.clone().add(gapVector.clone().multiply(i)));
        }
        return vectors;
    }
}
