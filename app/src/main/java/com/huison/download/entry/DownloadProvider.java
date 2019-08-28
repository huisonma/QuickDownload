package com.huison.download.entry;

import android.content.Context;

import com.huison.download.utils.CloseUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadProvider {

    private static List<DownloadBean> sDownloadBeans = new ArrayList<>();

    public static List<DownloadBean> getDataFromAssets(Context context) {
        if (sDownloadBeans.isEmpty()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open("data.json")));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                JSONObject jsonObject = new JSONObject(content.toString());
                JSONArray jsonArray = jsonObject.optJSONArray("data");
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (int i = 0, length = jsonArray.length(); i < length; i++) {
                        JSONObject object = jsonArray.optJSONObject(i);
                        String name = object.optString("name");
                        String icon = object.optString("icon");
                        String url = object.optString("url");
                        String size = object.optString("size");
                        String downloadTimes = object.optString("download_times");
                        DownloadBean downloadBean = new DownloadBean(name, icon, url, size, downloadTimes);
                        sDownloadBeans.add(downloadBean);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                CloseUtil.close(reader);
            }
        }
        return sDownloadBeans;
    }
}
