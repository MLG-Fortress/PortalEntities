package to.us.tf.PortalEntities;

import com.matejdro.bukkit.portalstick.Portal;
import com.matejdro.bukkit.portalstick.PortalStick;
import de.V10lator.PortalStick.V10Location;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by RoboMWM on 11/22/2016.
 * What a dumb name
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
}
