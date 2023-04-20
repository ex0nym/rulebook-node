package io.exonym.rulebook.schema;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.exonym.lite.pojo.IApiKey;
import io.exonym.lite.pojo.IUser;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Administrators {
	
	private HashSet<Administrator> administrators = new HashSet<>();
	
	public HashSet<Administrator> getAdministrators() {
		return administrators;
		
	}

	public void setAdministrators(HashSet<Administrator> administrators) {
		this.administrators = administrators;
		
	}
	
	public static JsonObject create(List<IUser> administrators, List<IApiKey> keys) throws Exception {
		if (administrators==null) {
			throw new NullPointerException();
			
		}
		Collections.sort(administrators);
		Administrators result = new Administrators();
		
		for (IUser u : administrators) {
			Administrator a = new Administrator();
			a.setId(u.get_id());
			a.setUsername(u.getUsername());
			result.getAdministrators().add(a);

		}

		for (IApiKey key : keys){
			Administrator a0 = new Administrator();
			a0.setId(key.getUuid());
			a0.setUsername(key.getUuid());
			result.getAdministrators().add(a0);

		}
		Gson g = new Gson();
		String jsonString = g.toJson(result);

		return (JsonObject) JsonParser.parseString(jsonString);
		
	}
}
