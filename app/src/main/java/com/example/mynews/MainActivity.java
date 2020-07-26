package com.example.mynews;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
ArrayList<String> titles;
    ArrayList<String> content;
ArrayAdapter adapter;
ListView listView;

SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.listview);
        titles = new ArrayList<String>();
        adapter = new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,titles);
        listView.setAdapter(adapter);

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER, title VARCHAR, Content VARCHAR)");
        UpdateListView();

        Downloadtask downloadtask = new Downloadtask();
        try {
        downloadtask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
    }catch (Exception e){
        e.printStackTrace();
        }
    }

    public  void UpdateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex =c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if (c.moveToFirst()){
            titles.clear();
            content.clear();
do {
    titles.add(c.getString(titleIndex));
    content.add(c.getString(contentIndex));
} while (c.moveToNext());
    adapter.notifyDataSetChanged();
        }
    }
    public class Downloadtask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection)url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();

                while (data != -1){
                    char current = (char) data;
                    result += current;
                    data = reader.read();

                }

                JSONArray jsonArray = new JSONArray(result);
                int numberofitem = 20;
                if (jsonArray.length()< 20){
                    numberofitem = jsonArray.length();
                }
                articleDB.execSQL("DELETE FROM artciles");
                for(int i= 0; i< numberofitem;i++){
                    String articleid = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleid+".json?print=pretty");
                    urlConnection = (HttpURLConnection)url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String articleinfo = "";
                    while (data != -1){
                        char current = (char)data;
                        articleinfo +=  current;
                        data = reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleinfo);
                    if (!jsonObject.isNull("title")&& !jsonObject.isNull("url")){
                        String articletile = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection)url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);
                        data = reader.read();
                        String articleContent = "";
                        while (data != -1){
                            char current = (char) data;
                            articleinfo += current;
                            data = reader.read();

                        }
                        Log.i("ArticleContent",articleContent);
                        String sql = "INSERT INTO articles(articleID,title,Content)VALUES (?,?,?)";

                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1,articleid);
                        statement.bindString(2,articletile);
                        statement.bindString(1,articleContent);
                        statement.execute();
                    }

                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdateListView();
        }
    }
}
