package com.greenlemonmobile.app.ebook.books.model;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TouchableItem
{
    public enum ItemType {
        UNKNOWN,
        VIDEO,
        AUDIO,
        IMAGE,
        TABLE,
        BUTTON,
        LINK
    }

    public final String id;
    public final Rect bounds;
    public final Boolean hasControls;
    public final String source;
    public final ItemType type;

    public TouchableItem(String id, ItemType type, Rect rect, Boolean boolean1, String s)
    {
        this.id = id;
        this.type = type;
        bounds = rect;
        hasControls = boolean1;
        source = s;
    }

    public static ItemType typeFromString(String s)
    {
        ItemType i = ItemType.UNKNOWN;
        if (s.equalsIgnoreCase("video"))
            i = ItemType.VIDEO;
        else if (s.equalsIgnoreCase("audio"))
            i = ItemType.AUDIO;
        else if (s.equalsIgnoreCase("a"))
            i = ItemType.LINK;
        else if (s.equalsIgnoreCase("img"))
            i = ItemType.IMAGE;
        else if (s.equalsIgnoreCase("table"))
            i = ItemType.TABLE;
        else if (s.equalsIgnoreCase("button"))
            i = ItemType.BUTTON;
        return i;
    }

    public String typeAsString()
    {
        String s = "unknown";
        switch (type) {
            case VIDEO:
                s = "video";
                break;
            case AUDIO:
                s = "audio";
                break;
            case IMAGE:
                s = "img";
                break;
            case TABLE:
                s = "table";
                break;
            case BUTTON:
                s = "button";
                break;
            case LINK:
                s = "a";
                break;
        }
        return s;
    }

    public static ArrayList<TouchableItem> parseTouchableItems(String s)
    {
        ArrayList<TouchableItem> arraylist = new ArrayList<TouchableItem>();

        try {
            JSONArray jsonarray = new JSONArray(s);
            if (jsonarray.length() > 0)
            {
                for (int i = 0; i < jsonarray.length(); ++i) {
                    JSONObject jsonobject = jsonarray.getJSONObject(i);
                    JSONObject jsonobject1 = jsonobject.getJSONObject("bounds");
                    Rect rect = new Rect(jsonobject1.getInt("left"), jsonobject1.getInt("top"),
                            jsonobject1.getInt("right"), jsonobject1.getInt("bottom"));
                    String source = jsonobject.getString("source");
                    String id = jsonobject.getString("id");
                    TouchableItem.ItemType type = TouchableItem.typeFromString(jsonobject
                            .getString("type"));
                    if (type == TouchableItem.ItemType.UNKNOWN)
                        type = TouchableItem.ItemType.UNKNOWN;
                    arraylist.add(new TouchableItem(id, type, rect, Boolean.valueOf(jsonobject
                            .getBoolean("hasControls")), source));
                }

            }
        } catch (JSONException e) {
            arraylist = null;
        } finally {
        }
        return arraylist;
    }
}
