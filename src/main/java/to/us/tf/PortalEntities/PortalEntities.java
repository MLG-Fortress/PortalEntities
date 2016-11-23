package to.us.tf.PortalEntities;

import com.matejdro.bukkit.portalstick.Portal;
import com.matejdro.bukkit.portalstick.PortalStick;
import de.V10lator.PortalStick.V10Location;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by RoboMWM on 11/22/2016.
 * What a dumb plogen name, u come up wit a bettar 1
 */
public class PortalEntities extends JavaPlugin implements Listener
{
    PortalStick portalStick;

    public void onEnable()
    {
        portalStick = (PortalStick)getServer().getPluginManager().getPlugin("PortalStick");
        getServer().getPluginManager().registerEvents(this, this);
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
                if (location.distanceSquared(portalLocation) < 25) //5 blocks
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
        return new Location(getServer().getWorld(v10Location.world), v10Location.x, v10Location.y, v10Location.z);
    }

    public void trackEntity(Entity entity)
    {
        new BukkitRunnable()
        {
            //Amount of ticks to track this entity for
            int ticks = 200;
            Location previousLocation = entity.getLocation();
            public void run()
            {
                if (entity.isDead())
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
                
                if (--ticks < 0)
                    this.cancel();
            }
        }.runTaskTimer(this, 1L, 1L);
    }
}
