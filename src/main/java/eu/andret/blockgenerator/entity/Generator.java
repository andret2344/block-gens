package eu.andret.blockgenerator.entity;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.json.JSONObject;

@Value
@AllArgsConstructor
public class Generator {
	private GeneratorPattern pattern;
	@NonFinal
	private Block block;

	public Generator(GeneratorPattern pattern) {
		this.pattern = pattern;
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("world", block.getWorld().getName());
		result.put("x", block.getX());
		result.put("y", block.getY());
		result.put("z", block.getZ());
		return result;
	}

	public void fromJSON(JSONObject object) {
		block = new Location(Bukkit.getWorld(object.getString("world")),
				object.getInt("x"),
				object.getInt("y"),
				object.getInt("z")).getBlock();
	}
}
