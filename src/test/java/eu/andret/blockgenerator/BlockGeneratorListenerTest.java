package eu.andret.blockgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockGeneratorListenerTest {
    @Mock
    private BlockPlaceEvent blockPlaceEvent;
    @Mock
    private atsBlockGenerator plugin;
    @Mock
    private BlockGenerator blockGenerator;
    @Mock
    private Block block;

    @Test
    public void testPlaceBlockWithNoGeneratorsCreated() {
        BlockGeneratorListener blockGeneratorListener = new BlockGeneratorListener(plugin);
        when(plugin.getGenerators()).thenReturn(new ArrayList<>());
        blockGeneratorListener.place(blockPlaceEvent);
        verify(blockGenerator, times(0)).add(block);
    }

    @Test
    public void testPlaceBlockWithDuplicatedGenerator() {
        ItemStack generator = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(generator.getItemMeta()).thenReturn(meta);
        when(blockGenerator.getGenerator()).thenReturn(generator);
        BlockGeneratorListener blockGeneratorListener = new BlockGeneratorListener(plugin);
        when(blockPlaceEvent.getItemInHand()).thenReturn(generator);
        when(plugin.getGenerators()).thenReturn(Arrays.asList(blockGenerator, blockGenerator));
        blockGeneratorListener.place(blockPlaceEvent);
        verify(blockGenerator, times(1)).add(block);
    }
}