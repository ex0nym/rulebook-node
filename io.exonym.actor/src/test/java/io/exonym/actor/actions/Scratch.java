package io.exonym.actor.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Scratch {

	public Scratch() {
	}

	public static void main(String[] args) {
		JsonObject json = new JsonObject();
		JsonObject child = new JsonObject();

		JsonArray fields = new JsonArray(); 
		fields.add("primaryAdmin");
		fields.add("admin");

		json.add("index", child);
		child.add("fields", fields);

		System.out.println(json.toString());

	}
}
