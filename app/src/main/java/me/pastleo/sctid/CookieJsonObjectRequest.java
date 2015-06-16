package me.pastleo.sctid;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by PastLeo on 2015/6/15.
 */
public class CookieJsonObjectRequest extends Request<JSONObject> {

    SharedPreferences settings;
    private Response.Listener<JSONObject> listener;
    private Map<String, String> params;

    protected final static String PREF_SAVED_COOKIE = "PREF_SAVED_COOKIE";
    protected final static String SET_COOKIE = "Set-Cookie";

    public CookieJsonObjectRequest(int method, String url,
                                   Map<String, String> params, Response.Listener<JSONObject> listener,
                                   Response.ErrorListener errorListener,SharedPreferences settings) throws UnsupportedEncodingException {
        super(method, url,errorListener);
        this.params = params;
        this.listener = listener;
        this.settings = settings;
    }

    @Override
    protected Map<String, String> getParams() {
        if(params == null) return new HashMap<>();
        return params;
    };

    @Override
    protected Response<JSONObject> parseNetworkResponse(
            NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            Map<String, String> headers = response.headers;

            if (headers.containsKey(SET_COOKIE)) {
                List<HttpCookie> cl;
                HashMap<String,HttpCookie> cs = new HashMap<>();

                String previous_cookie_str = settings.getString(PREF_SAVED_COOKIE, "");
                if(previous_cookie_str.length() > 0) {
                    cl = HttpCookie.parse(settings.getString(PREF_SAVED_COOKIE, ""));
                    for (int i = 0; i < cl.size(); i++) {
                        cs.put(cl.get(i).getName(),cl.get(i));
                    }
                }

                cl = HttpCookie.parse(headers.get(SET_COOKIE));
                for (int i = 0; i < cl.size(); i++) {
                    cs.put(cl.get(i).getName(),cl.get(i));
                }

                HttpCookie tmp;
                String cookies = "";
                String aa;
                Iterator<Map.Entry<String,HttpCookie>> i = cs.entrySet().iterator();
                while(i.hasNext()){
                    aa = i.next().getKey();
                    tmp = cs.get(aa);
                    cookies += tmp.toString() + "; ";
                }

                settings
                        .edit()
                        .putString(PREF_SAVED_COOKIE, cookies)
                        .commit();
            }
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> customHeaders = super.getHeaders();
        Map<String, String> newHeaders = new HashMap<String, String>();
        newHeaders.putAll(customHeaders);
        String cookieStr = settings.getString(PREF_SAVED_COOKIE, "");
        if (cookieStr.length() > 0) {
            newHeaders.put("cookie", cookieStr);
        }
        return newHeaders;
    }

    public static RequestQueue getRequestQueue(RequestQueue rq,Context mCtx) {
        if (rq == null) {
            rq = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return rq;
    }

}
