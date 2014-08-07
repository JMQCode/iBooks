package com.greenlemonmobile.app.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Vector;

public class DatabaseUtil {
    static class ColumnData {
        String columnName;
        ArrayList<String> attributes;
    }
    
    public static boolean updateVerify(SQLiteDatabase db, int oldVersion, int newVersion, final String createSql) {
        if (db != null && db.isOpen() && !TextUtils.isEmpty(createSql) && (newVersion > oldVersion)) {
            String tableName = null;
            String sql = createSql;
            sql = sql.trim();
            
            int startOffset = TextUtils.indexOf(sql, "(");
            int endOffset = TextUtils.indexOf(sql, ")", sql.length() - 2);
            
            if (startOffset != -1) {
                int tableOffset = TextUtils.indexOf(sql, "TABLE");
                if (tableOffset != -1) {
                    String temp = TextUtils.substring(sql, tableOffset, startOffset);
                    temp = temp.trim();
                    String[] table = TextUtils.split(temp, " ");
                    if(table.length>0&&!TextUtils.isEmpty(table[table.length-1])){
                    	 tableName = table[table.length-1];
                    }
//                    if (table != null) {
//                        boolean create_table_command_found = false;
//                        for (String text : table) {
//                            if (text != null && !TextUtils.isEmpty(text.trim()))
//                            {
//                                if (text.equals("table")) {
//                                    create_table_command_found = true;
//                                } else if (create_table_command_found) {
//                                    tableName = text;
//                                    break;
//                                }
//                            }
//                        }
//                    }
                }
            }
            
            try {
                boolean isExist = tabbleIsExist(db, tableName);
                if (!isExist) {
                    db.execSQL(createSql);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (startOffset != -1 && endOffset != -1) {
                String columnString = TextUtils.substring(sql, startOffset + 1, endOffset);
                String[] coloumsString = TextUtils.split(columnString, ",");
                
                ArrayList<ColumnData> newVersionColumns = new ArrayList<ColumnData>();
                if (coloumsString != null) {
                    for (String text : coloumsString) {
                    	text = text.trim();
                        ColumnData columnData = new ColumnData();
                        columnData.attributes = new ArrayList<String>();
                        
                        String[] columAttributes = TextUtils.split(text, " ");
                        if (columAttributes != null) {
                            int attributeIndex = 0;
                            for (int index = 0; index < columAttributes.length; ++index) {
                                String attribute = columAttributes[index];
                                if (attribute != null && !TextUtils.isEmpty(attribute.trim()))
                                {
                                    if (attributeIndex == 0) {
                                        columnData.columnName = attribute;
                                    } else {
                                        columnData.attributes.add(attribute);
                                    }
                                    ++attributeIndex;
                                }
                            }
                            if (!TextUtils.isEmpty(columnData.columnName))
                                newVersionColumns.add(columnData);
                        }
                    }
                }
                
                if (!newVersionColumns.isEmpty() && !TextUtils.isEmpty(tableName)) {
                    Cursor cursor = null;
                    try {
                        cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0,1", null);
                        if (cursor != null) {
                            
                            cursor.moveToFirst();
                            String[] oldVersionColumns2 = cursor.getColumnNames();
                            if (oldVersionColumns2 != null) {
                                Vector<String> oldVersionColumns = new Vector<String>();
                                for (String object : oldVersionColumns2)
                                    oldVersionColumns.add(object.toLowerCase());
                                for (ColumnData column : newVersionColumns) {
                                    if (!oldVersionColumns.contains(column.columnName) && !column.attributes.isEmpty()) {
                                        String alertSql = "ALTER TABLE " + tableName + " ADD COLUMN ";
                                        alertSql += column.columnName;
                                        for (String attribute : column.attributes)
                                            alertSql += " " + attribute;
                                        alertSql += ";";
                                        db.execSQL(alertSql);
                                    }
                                }
                            }
    
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                }
            }
        }
        return false;
    }
	
    /** 
     * 
     * @param tabName
     * @return 
     */  
    public static boolean tabbleIsExist(SQLiteDatabase db,String tableName){
            boolean result = false;  
            tableName = tableName.trim();
            if(TextUtils.isEmpty(tableName)){
                    return false;  
            }
//            SQLiteDatabase db = null;  
            Cursor cursor = null;  
            try {  
//                    db = this.getReadableDatabase();  
                    String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='"+tableName+"' ";  
                    cursor = db.rawQuery(sql, null);  
                    if(cursor.moveToNext()){  
                            int count = cursor.getInt(0);  
                            if(count>0){  
                                    result = true;  
                            }  
                    }  
                      
            } catch (Exception e) {  
                    // TODO: handle exception  
            }                  
            return result;  
    }  
}
