/*
Copyright (c) 2012-2014 Jon Petraglia of Qweex

This software is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any damages arising from the use of this software.

Permission is granted to anyone to use this software for any purpose, including commercial applications, and to alter it and redistribute it freely, subject to the following restrictions:

    1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.

    2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.

    3. This notice may not be removed or altered from any source distribution.
 */
package com.qweex.nitrodroid;


//TODO: Modify list w/ updating times

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import com.qweex.utils.QweexUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** A tool for communicating with the SQlite database
 *  It has 3 different tables in it: episodes, queue, and calendar
 * @author MrQweex
 */

public class DatabaseConnector 
{
	//------Basic Functions
	private static final String DATABASE_NAME = "Nitro.db", TASKS_TABLE = "tasks", TASKS_TIME_TABLE = "ttimes",
								LISTS_TABLE = "lists", LISTS_TIME_TABLE = "ltimes", TASKS_DEL_TABLE = "deleted";
	private SQLiteDatabase database;
	private DatabaseOpenHelper databaseOpenHelper = null;
	
	/** Constructor for the class.
	 * @param context The context to associate with the connector.
	 * */
	public DatabaseConnector(Context context) 
	{
	    databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
//	    open();
//	    this.createThemTables(database);
	}
	
	/** Opens the database so that it can be read or written. */
	public void open() throws SQLException 
	{
	   database = databaseOpenHelper.getWritableDatabase();
	}
	
	/** Closes the database when you are done with it. */
	public void close() 
	{
	   if (database != null)
	      database.close();
	}
	
	
	
	public void insertTask(String hash,
							  String name,
							  long priority,
							  long date,
							  String notes,
							  String list,
							  long logged,
							  String tags,
							  int order)
	{
	   ContentValues newTask = new ContentValues();
	   newTask.put("hash", hash);
	   newTask.put("name", name);
	   newTask.put("priority", priority);
	   newTask.put("date", date);
	   newTask.put("notes", notes);
	   newTask.put("list", list);
	   newTask.put("logged", logged);
	   newTask.put("tags", tags);
	   newTask.put("order_num", order);
	
	   database.insert(TASKS_TABLE, null, newTask);
	}
	
	public void insertTaskTimes(String hash,
			  long name,
			  long priority,
			  long date,
			  long notes,
			  long list,
			  long logged,
			  long tags)
	{
		ContentValues newTask = new ContentValues();
		newTask.put("hash", hash);
		newTask.put("name", name);
		newTask.put("priority", priority);
		newTask.put("date", date);
		newTask.put("notes", notes);
		newTask.put("list", list);
		newTask.put("logged", logged);
		newTask.put("tags", tags);
		
		database.insert(TASKS_TIME_TABLE, null, newTask);
	}
	
	public boolean deleteTask(String hash) 
	{
		boolean x = database.delete(TASKS_TABLE, "hash='" + hash + "'", null) > 0;
		if(x)
		{
			database.delete(TASKS_TIME_TABLE, "hash='" + hash + "'", null);
			insertDeleted(hash, (new java.util.Date()).getTime());
		}
	    return x;
	}

    public boolean deleteList(String hash)
    {
        String TAG= QweexUtils.TAG();
        boolean x = database.delete(LISTS_TABLE, "hash='" + hash + "'", null) > 0;
        if(x)
        {
            Cursor c = getTasksOfList(hash,null);
            if(c.getCount()>0)
                c.moveToFirst();
            while(!c.isAfterLast())
            {
                Log.i(TAG,"DELETING: " + c.getString(c.getColumnIndex("name")));
                database.delete(TASKS_TABLE, "hash='" + c.getString(c.getColumnIndex("hash")) + "'", null);
                database.delete(TASKS_TIME_TABLE, "hash='" + c.getString(c.getColumnIndex("hash")) + "'", null);
                c.moveToNext();
            }
            database.delete(LISTS_TIME_TABLE, "hash='" + hash + "'", null);
            insertDeleted(hash, (new java.util.Date()).getTime());
        }
        return x;
    }

    public boolean modifyList(String hash, String col, String newname)
    {
        String TAG= QweexUtils.TAG();
        ContentValues args = new ContentValues(), args2 = new ContentValues();
        args.put(col, newname);
        args2.put(col, (new java.util.Date()).getTime());
        boolean x = database.update(LISTS_TABLE, args, "hash='" + hash + "'", null)>0;
        Log.i(TAG,"Updating list" + x);
        if(x)
            database.update(LISTS_TIME_TABLE, args2, "hash='" + hash + "'", null);
        return x;
    }
	
	//OVERLOAD _ALL_ THE FUNCTIONS!
	
	public boolean modifyTask(String hash, String[] columns, String[] new_vals)
	{
        String TAG= QweexUtils.TAG();
		ContentValues args = new ContentValues(), args2 = new ContentValues();
		long now = (new java.util.Date()).getTime();
		for(int i=0; i<columns.length; i++)
		{
			args.put(columns[i], new_vals[i]);
            if(!columns[i].equals("order_num"))
			    args2.put(columns[i], now);
            Log.i(TAG,"Updating Task (" + columns[i] + ") to " + new_vals[i]);
		}
		boolean x = database.update(TASKS_TABLE, args, "hash='" + hash + "'", null)>0;
        if(x)
			database.update(TASKS_TIME_TABLE, args2, "hash='" + hash + "'", null);
	    return x;
	}
	
	public boolean modifyTask(String hash, String column, int new_val)
	{
        String TAG= QweexUtils.TAG();
		ContentValues args = new ContentValues(), args2 = new ContentValues();
	    args.put(column, new_val);
	    args2.put(column, (new java.util.Date()).getTime());
	    boolean x = database.update(TASKS_TABLE, args, "hash='" + hash + "'", null)>0;
        Log.i(TAG, "Updating Task (" + column+ ") to " + new_val + ":" + x);
		if(x && !column.equals("order_num"))
			database.update(TASKS_TIME_TABLE, args2, "hash='" + hash + "'", null);
	    return x;
	}
	
	public boolean modifyTask(String hash, String column, long new_val)
	{
		ContentValues args = new ContentValues(), args2 = new ContentValues();
	    args.put(column, new_val);
	    args2.put(column, (new java.util.Date()).getTime());
	    boolean x = database.update(TASKS_TABLE, args, "hash='" + hash + "'", null)>0;
        System.out.println("Updating Task (" + column+ ") to " + new_val + ":" + x);
        if(x && !column.equals("order_num"))
			database.update(TASKS_TIME_TABLE, args2, "hash='" + hash + "'", null);
	    return x;
	}
	
	public boolean modifyTask(String hash, String column, String new_val)
	{
        String TAG= QweexUtils.TAG();
		ContentValues args = new ContentValues(), args2 = new ContentValues();
	    args.put(column, new_val);
	    args2.put(column, (new java.util.Date()).getTime());
	    boolean x = database.update(TASKS_TABLE, args, "hash='" + hash + "'", null)>0;
        Log.i(TAG,"Updating Task (" + column+ ") to " + new_val + ":" + x);
        if(x && !column.equals("order_num"))
			database.update(TASKS_TIME_TABLE, args2, "hash='" + hash + "'", null);
	    return x;
	}
	
	
	public void modifyOrder(String hash, int new_order)
	{
		modifyTask(hash, "order_num", new_order);
	}
	
	public void modifyListOrder(String hash, String new_order)
	{
	 	ContentValues args = new ContentValues(), args2 = new ContentValues();
	    args.put("tasks_in_order", new_order);
	    args2.put("tasks_in_order", (new java.util.Date()).getTime());
	    database.update(LISTS_TABLE, args, "hash='" + hash + "'", null);
	    database.update(LISTS_TIME_TABLE, args2, "hash='" + hash + "'", null);
	}
	
	public void insertList(String hash,
			  String name,
			  String[] tasks_in_order)
	{
		ContentValues newList = new ContentValues();
		newList.put("hash", hash);
		newList.put("name", name);
		String tasksString = "";
		if(tasks_in_order!=null && tasks_in_order.length>0)
		{
			tasksString = tasks_in_order[0];
			for(int i=1; i<tasks_in_order.length; i++)
				tasksString = tasksString.concat("," + tasks_in_order[i]);
		}
		newList.put("tasks_in_order", tasksString);
		
		database.insert(LISTS_TABLE, null, newList);
	}
	
	public void insertListTimes(String hash,
			  long name,
			  long tasks_in_order)
	{
		ContentValues newTask = new ContentValues();
		newTask.put("hash", hash);
		newTask.put("name", name);
		newTask.put("tasks_in_order", tasks_in_order);
		
		database.insert(LISTS_TIME_TABLE, null, newTask);
	}
	
	public Cursor getAllLists()
	{
		return database.query(LISTS_TABLE, new String[] {"_id", "hash", "name", "tasks_in_order"},
				null, null, null, null, null);
	}
	
	public Cursor getTodayTasks(long currentDay, String tag)
	{
        String TAG= QweexUtils.TAG();
        Log.i(TAG, "getting today tasks");
		long msecondsInDay = 60 * 60 * 24 * 1000;
		String query = "SELECT * FROM " + TASKS_TABLE + " " + "WHERE ( "
                + " ( list = 'today' ) "
                + " OR ( date BETWEEN " + 1 + " AND " + (currentDay+msecondsInDay-1) + " ) ";
                //+ " OR ( date < " + currentDay/1000/100 + " )"
        if(tag!=null)
            query = query + " AND tags like '%" + tag + "%'";
        query = query + ") ";
		return database.rawQuery(query, null);
	}


    public Cursor getTasksOfList(String hash, String sort) { return getTasksOfList(hash, sort, false); }
	public Cursor getTasksOfList(String hash, String sort, boolean doneOnly)
	{
        String TAG= QweexUtils.TAG();
		if(hash!=null && !"all".equals(hash))
		{
			if(!hash.equals(""))
			{
				hash = "list = '" + hash + "'";
                if(doneOnly && (!hash.equals("list = 'logbook'") && !ListsActivity.v2))
					hash = hash + " AND logged='0'";
			}
            else if(doneOnly)
                hash = "logged='0'";
		}
        else
            hash = "logged='0'";
        Log.i(TAG, hash);
        sort = "logged, " + sort;
		return database.query(TASKS_TABLE,
			    new String[] {"_id", "hash", "name", "priority", "date", "notes", "list", "logged", "tags"},
			    hash, null, null, null, sort);
	}

    public Cursor getUnloggedDone()
    {
        return database.query(TASKS_TABLE,
                new String[] {"_id", "hash", "logged", "list"},
                "logged>0 AND list!='logbook'", null, null, null, null);
    }

    public void moveTask(String hash, String target_list)
    {
        String TAG= QweexUtils.TAG();
        long new_timestamp = new java.util.Date().getTime();
        String old_list, tasks_in_order;
        Cursor tempCursor, tempCursor2;
        tempCursor = database.query(TASKS_TABLE, new String[] {"_id", "hash", "list"},
                "hash='" + hash + "'", null, null, null, null);
        tempCursor.moveToFirst();
        old_list = tempCursor.getString(tempCursor.getColumnIndex("list"));

        Log.d(TAG, "old_list: " + old_list);
        //1. Update the "list" field of TASKS
        //2. Update the "list" field of TASKS_TIME
        modifyTask(hash, "list", target_list);

        //3. Remove from old "tasks_in_order" field of LISTS
        //4. Update the old "tasks_in_order" field of LISTS_TIME
        Log.d(TAG, "hash: " + hash);
        tempCursor = database.query(LISTS_TABLE, new String[] {"_id", "hash", "tasks_in_order", "name"},
                "hash='" + old_list + "'", null, null, null, null);
        tempCursor.moveToFirst();
        tasks_in_order = tempCursor.getString(tempCursor.getColumnIndex("tasks_in_order"));
        Log.d(TAG, "BEFORE: Tasksinorder: " + tasks_in_order + " [ " + tempCursor.getString(tempCursor.getColumnIndex("name")) + " ]");
        tasks_in_order = tasks_in_order.replace(hash,"");
        //3.1. Take care of fixing the formatting...
        if(tasks_in_order.startsWith(","))                   // (a) if it's at the beginning (leading ',')
            tasks_in_order = tasks_in_order.substring(1);
        else if(tasks_in_order.endsWith(","))                // (b) if it's at the end (trailing ',')
            tasks_in_order = tasks_in_order.substring(0,tasks_in_order.length()-2);
        else                                                 // (c) if it's in the middle (a ',,' somewhere)
            tasks_in_order = tasks_in_order.replace(",,", ",");
        //3.2 The actual updating
        modifyList(old_list, "tasks_in_order", tasks_in_order);

        Log.d(TAG, "AFTER: Tasksinorder: " + tasks_in_order);
        //5. Update the "tasks_in_order" field of LISTS
        //6. Update the "tasks_in_order" field of LISTS_TIME
        tempCursor2 = database.query(LISTS_TABLE, new String[] {"_id", "hash", "tasks_in_order", "name"},
                "hash='" + target_list + "'", null, null, null, null);
        tempCursor2.moveToFirst();
        String tasks_in_order2 = tempCursor2.getString(tempCursor2.getColumnIndex("tasks_in_order"));
        Log.d(TAG, "NEW: Tasksinorder: " + tasks_in_order2 + " [ " + tempCursor2.getString(tempCursor2.getColumnIndex("name")) + " ]");
        if(tasks_in_order2==null)
            tasks_in_order2 = new String();
        else if(tasks_in_order2.equals(""))
            tasks_in_order2 = hash;
        else
            tasks_in_order2 = hash + "," + tasks_in_order2;
        modifyList(target_list, "tasks_in_order", tasks_in_order2);
        Log.d(TAG, "NEWEST: Tasksinorder: " + tasks_in_order2);
    }

	
	public void clearEverything(Context context)
	{
		database.execSQL("DROP TABLE " + LISTS_TABLE);
		database.execSQL("DROP TABLE " + TASKS_TABLE);
		database.execSQL("DROP TABLE " + LISTS_TIME_TABLE);
		database.execSQL("DROP TABLE " + TASKS_TIME_TABLE);
		database.execSQL("DROP TABLE " + TASKS_DEL_TABLE);
		createThemTables(database);
		open();
	}
	
	public long getListTime(String hash, String column)
	{
		long result = 0;
		Cursor c= database.query(LISTS_TIME_TABLE, new String[] {"_id", column}, 
				"hash='" + hash + "'", null, null, null, null);
		if(c.getCount()>0)
		{
			c.moveToFirst();
			result = c.getLong(c.getColumnIndex(column));
		}
		return result;
	}
	
	public long getTaskTime(String hash, String column)
	{
		long result = 0;
		Cursor c= database.query(TASKS_TIME_TABLE, new String[] {"_id", column}, 
				"hash='" + hash + "'", null, null, null, null);
		if(c.getCount()>0)
		{
			c.moveToFirst();
			result = c.getLong(c.getColumnIndex(column));
		}
		return result;
	}
	
	public void insertDeleted(String hash, long date)
	{
		ContentValues newTask = new ContentValues();
		newTask.put("hash", hash);
		newTask.put("date", date);
		
		database.insert(TASKS_DEL_TABLE, null, newTask);
	}
	
	public long getDelete(String hash)
	{
		long result = 0;
		Cursor c= database.query(TASKS_DEL_TABLE, new String[] {"_id", "date"}, 
				"hash='" + hash + "'", null, null, null, null);
		if(c.getCount()>0)
		{
			c.moveToFirst();
			result = c.getLong(c.getColumnIndex("date"));
		}
		return result;
	}
	
	public Cursor getAllDeleted()
	{
		return database.query(TASKS_DEL_TABLE, new String[] {"_id", "hash", "date"}, 
				null, null, null, null, null);
	}


    public Set<String> getTags()
    {
        Set<String> derp = new HashSet<String>();
        Cursor c =  database.query(TASKS_TABLE,
                new String[] {"_id", "tags"},
                null, null, null, null, null);
        if(c.getCount()>0)
            c.moveToFirst();
        while(!c.isAfterLast())
        {
            derp.addAll(java.util.Arrays.asList(c.getString(c.getColumnIndex("tags")).split(",")));
            c.moveToNext();
        }
        return derp;
    }

    public Cursor searchTags(String tag, String list, String sortby)
    {
        String TAG = QweexUtils.TAG();
        String s = "tags like '%" + tag + "%'";
        if("logbook".equals(list))
            s = s + " AND logged>0";
        else if(!("all".equals(list)))
            s = s + " AND list='" + list + "' AND logged=0";
        Log.i(TAG, "searching tags for " + s);
        //term = term + " AND logged='0'";
        return database.query(TASKS_TABLE,
                new String[] {"_id", "hash", "name", "priority", "date", "notes", "list", "logged", "tags"},
                s, null, null, null, sortby);
    }

    public Cursor searchAll(String term, String listHash, String sortby)
    {
        term = "tags like '%" + term + "%' OR " +
               "name like '%" + term + "%' OR " +
               "notes like '%" + term + "%'";
        if(listHash!=null)
            term = "list='" + listHash + "' AND ( " + term + " )";
        return database.query(TASKS_TABLE,
                new String[] {"_id", "hash", "name", "priority", "date", "notes", "list", "logged", "tags"},
                term, null, null, null, sortby);
    }

	
	public void createThemTables(SQLiteDatabase db)
	{
	      String createQuery = "CREATE TABLE " + TASKS_TABLE + " " + 
	 	         "(_id integer primary key autoincrement, " +
	 	    		"hash TEXT, " +
	 	         	"name TEXT, " +
	 	         	"priority INTEGER, " + 
	 	         	"date INTEGER, " +
	 	         	"notes TEXT, " +
	 	         	"list TEXT, " +
	 	         	"logged INTEGER, " +
	 	         	"tags TEXT, " +
	 	         	"order_num INTEGER);";
	 	      db.execSQL(createQuery);
	 	      String createQuery2 = "CREATE TABLE " + TASKS_TIME_TABLE + " " + 
	 	 	         "(_id integer primary key autoincrement, " +
	 	 	         	"hash TEXT, " +
	 	 	         	"name INTEGER, " +
	 	 	         	"priority INTEGER, " + 
	 	 	         	"date INTEGER, " +
	 	 	         	"notes INTEGER, " +
	 	 	         	"list INTEGER, " +
	 	 	         	"logged INTEGER, " +
	 	 	         	"tags TEXT);";
	 	      db.execSQL(createQuery2);
	 	      String createQuery3 = "CREATE TABLE " + LISTS_TABLE + " " + 
	 		 	         "(_id integer primary key autoincrement, " +
	 		 	         	"hash TEXT, " +
	 		 	         	"name TEXT, " +
	 		 	         	"tasks_in_order TEXT);"; //Actually an array; probably pipe delimted
	 	      db.execSQL(createQuery3);
	 	      String createQuery4 = "CREATE TABLE " + LISTS_TIME_TABLE + " " + 
	 		 	         "(_id integer primary key autoincrement, " +
	 		 	         	"hash TEXT, " +
	 		 	         	"name INTEGER, " +
	 		 	         	"tasks_in_order INTEGER);";
	 	      db.execSQL(createQuery4);
	 	     String createQuery5 = "CREATE TABLE " + TASKS_DEL_TABLE + " " + 
 		 	         "(_id integer primary key autoincrement, " +
 		 	         	"hash TEXT, " +
 		 	         	"date INTEGER);";
	 	     db.execSQL(createQuery5);
	}
	
/** Helper open class for DatabaseConnector */
	private class DatabaseOpenHelper extends SQLiteOpenHelper 
	{
	   public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version) 
	   {
	      super(context, name, factory, version);
	      this.getWritableDatabase();
	   }
	
	   @Override
	   public void onCreate(SQLiteDatabase db) 
	   {
	      createThemTables(db);
	   }
	
	   //FEATURE: Need to expect onUpgrade
	   @Override
	   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	   {
	   }
	}
}
