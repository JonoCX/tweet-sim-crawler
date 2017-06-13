package br.les.opus.dengue.crawler.gson;

import java.lang.reflect.Type;

import br.les.opus.commons.rest.geo.LatLng;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vividsolutions.jts.geom.Point;


public class JtsPointGsonSerializer implements JsonSerializer<Point> {


	@Override
	public JsonElement serialize(Point src, Type typeOfSrc, JsonSerializationContext context) {
		LatLng latLng = new LatLng(src);
		JsonObject object = new JsonObject();
		object.add("lat", new JsonPrimitive(latLng.getLat()));
		object.add("lng", new JsonPrimitive(latLng.getLng()));
		return object;
	}
}
