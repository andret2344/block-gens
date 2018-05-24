package eu.andret.blockgenerator;

import org.junit.Test;

public class BlockGeneratorTest {

    @Test
    public void createGeneratorWithNullValues() {
        new BlockGenerator(0, null, null);
    }
}