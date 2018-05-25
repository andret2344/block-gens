package eu.andret.blockgenerator;

import java.security.InvalidParameterException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BlockGeneratorTest {
    @Mock
    private World world;
    @Mock
    private ItemStack generator;
    @Mock
    private ItemStack generated;

    @Test
    public void createGeneratorWithNullValues() {
        new BlockGenerator(1, null, null);
    }

    @Test(expected = InvalidParameterException.class)
    public void createGeneratorWithNegativeRegenDelay() {
        new BlockGenerator(-10, generator, generated);
    }

    @Test
    public void addLocationToGenerator() {
        BlockGenerator uut = new BlockGenerator(1, generator, generated);
        Location location = new Location(world, 0, 0, 0);
        uut.add(location);
        assertTrue(uut.getPlaced().contains(location.getBlock()));
    }
}