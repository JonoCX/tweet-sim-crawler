package br.les.opus.dengue.crawler.gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.les.opus.commons.persistence.date.IsoDateFormat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class IsoSimpleDateGsonSerializer implements JsonSerializer<Date> {

	public static SimpleDateFormat DATE_ISO_FORMATTER;
	
	static {
		DATE_ISO_FORMATTER = new IsoDateFormat();
	}


	@Override
	public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(DATE_ISO_FORMATTER.format(src));
	}

}
