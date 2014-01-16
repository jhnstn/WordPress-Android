//Manages data for blog settings

package org.wordpress.android.models;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Map;

public class Blog {
    private int localTableBlogId;
    private String url;
    private String homeURL;
    private String blogName;
    private String username;
    private String password;
    private String imagePlacement;
    private boolean featuredImageCapable;
    private boolean fullSizeImage;
    private boolean scaledImage;
    private int scaledImageWidth;
    private String maxImageWidth;
    private int maxImageWidthId;
    private int lastCommentId;
    private boolean runService;
    private int remoteBlogId;
    private boolean location;
    private String dotcom_username;
    private String dotcom_password;
    private String api_key;
    private String api_blogid;
    private boolean dotcomFlag;
    private String wpVersion;
    private String httpuser;
    private String httppassword;
    private String postFormats;
    private String blogOptions;
    private boolean isAdmin;
    private boolean isHidden;

    public Blog() {

    }

    public Blog(int localTableBlogId, String url, String homeURL, String blogName, String username, String password, String imagePlacement, boolean featuredImageCapable, boolean fullSizeImage, boolean scaledImage, int scaledImageWidth, String maxImageWidth, int maxImageWidthId, int lastCommentId, boolean runService, int remoteBlogId, boolean location, String dotcom_username, String dotcom_password, String api_key, String api_blogid, boolean dotcomFlag, String wpVersion, String httpuser, String httppassword, String postFormats, String blogOptions, boolean isAdmin, boolean isHidden) {
        this.localTableBlogId = localTableBlogId;
        this.url = url;
        this.homeURL = homeURL;
        this.blogName = blogName;
        this.username = username;
        this.password = password;
        this.imagePlacement = imagePlacement;
        this.featuredImageCapable = featuredImageCapable;
        this.fullSizeImage = fullSizeImage;
        this.scaledImage = scaledImage;
        this.scaledImageWidth = scaledImageWidth;
        this.maxImageWidth = maxImageWidth;
        this.maxImageWidthId = maxImageWidthId;
        this.lastCommentId = lastCommentId;
        this.runService = runService;
        this.remoteBlogId = remoteBlogId;
        this.location = location;
        this.dotcom_username = dotcom_username;
        this.dotcom_password = dotcom_password;
        this.api_key = api_key;
        this.api_blogid = api_blogid;
        this.dotcomFlag = dotcomFlag;
        this.wpVersion = wpVersion;
        this.httpuser = httpuser;
        this.httppassword = httppassword;
        this.postFormats = postFormats;
        this.blogOptions = blogOptions;
        this.isAdmin = isAdmin;
        this.isHidden = isHidden;
    }

    public Blog(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.localTableBlogId = -1;
    }

    public int getLocalTableBlogId() {
        return localTableBlogId;
    }

    public void setLocalTableBlogId(int id) {
        this.localTableBlogId = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHomeURL() {
        return homeURL;
    }

    public void setHomeURL(String homeURL) {
        this.homeURL = homeURL;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImagePlacement() {
        return imagePlacement;
    }

    public void setImagePlacement(String imagePlacement) {
        this.imagePlacement = imagePlacement;
    }

    public boolean isFeaturedImageCapable() {
        return featuredImageCapable;
    }

    public void setFeaturedImageCapable(boolean isCapable) {
        this.featuredImageCapable = isCapable;
    }

    public boolean bsetFeaturedImageCapable(boolean isCapable) {
        if (featuredImageCapable == isCapable) {
            return false;
        }
        setFeaturedImageCapable(isCapable);
        return true;
    }

    public boolean isFullSizeImage() {
        return fullSizeImage;
    }

    public void setFullSizeImage(boolean fullSizeImage) {
        this.fullSizeImage = fullSizeImage;
    }

    public String getMaxImageWidth() {
        return StringUtils.notNullStr(maxImageWidth);
    }

    public void setMaxImageWidth(String maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageWidthId() {
        return maxImageWidthId;
    }

    public void setMaxImageWidthId(int maxImageWidthId) {
        this.maxImageWidthId = maxImageWidthId;
    }

    public int getLastCommentId() {
        return lastCommentId;
    }

    public void setLastCommentId(int lastCommentId) {
        this.lastCommentId = lastCommentId;
    }

    public boolean isRunService() {
        return runService;
    }

    public void setRunService(boolean runService) {
        this.runService = runService;
    }

    public int getRemoteBlogId() {
        return remoteBlogId;
    }

    public void setRemoteBlogId(int blogId) {
        this.remoteBlogId = blogId;
    }

    public boolean isLocation() {
        return location;
    }

    public void setLocation(boolean location) {
        this.location = location;
    }

    public String getDotcom_username() {
        return dotcom_username;
    }

    public void setDotcom_username(String dotcomUsername) {
        dotcom_username = dotcomUsername;
    }

    public String getDotcom_password() {
        return dotcom_password;
    }

    public void setDotcom_password(String dotcomPassword) {
        dotcom_password = dotcomPassword;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String apiKey) {
        api_key = apiKey;
    }

    public String getApi_blogid() {
        return api_blogid;
    }

    public void setApi_blogid(String apiBlogid) {
        api_blogid = apiBlogid;
    }

    public boolean isDotcomFlag() {
        return dotcomFlag;
    }

    public void setDotcomFlag(boolean dotcomFlag) {
        this.dotcomFlag = dotcomFlag;
    }

    public String getWpVersion() {
        return wpVersion;
    }

    public void setWpVersion(String wpVersion) {
        this.wpVersion = wpVersion;
    }

    public boolean bsetWpVersion(String wpVersion) {
        if (org.apache.commons.lang.StringUtils.equals(this.wpVersion, wpVersion)) {
            return false;
        }
        setWpVersion(wpVersion);
        return true;
    }

    public String getHttpuser() {
        return httpuser;
    }

    public void setHttpuser(String httpuser) {
        this.httpuser = httpuser;
    }

    public String getHttppassword() {
        return httppassword;
    }

    public void setHttppassword(String httppassword) {
        this.httppassword = httppassword;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public String getPostFormats() {
        return postFormats;
    }

    public void setPostFormats(String postFormats) {
        this.postFormats = postFormats;
    }

    public boolean bsetPostFormats(String postFormats) {
        if (org.apache.commons.lang.StringUtils.equals(this.postFormats, postFormats)) {
            return false;
        }
        setPostFormats(postFormats);
        return true;
    }

    // FIXME - Move to DB
    public int getUnmoderatedCommentCount() {
        return WordPress.wpDB.getUnmoderatedCommentCount(this.localTableBlogId);
    }

    public boolean isScaledImage() {
        return scaledImage;
    }

    public void setScaledImage(boolean scaledImage) {
        this.scaledImage = scaledImage;
    }

    public int getScaledImageWidth() {
        return scaledImageWidth;
    }

    public void setScaledImageWidth(int scaledImageWidth) {
        this.scaledImageWidth = scaledImageWidth;
    }

    public String getBlogOptions() {
        return blogOptions;
    }

    public void setBlogOptions(String blogOptions) {
        this.blogOptions = blogOptions;
    }

    // TODO: it's ugly to compare json strings, we have to normalize both strings before
    // comparison or compare JSON objects after parsing
    public boolean bsetBlogOptions(String blogOptions) {
        if (org.apache.commons.lang.StringUtils.equals(this.blogOptions, blogOptions)) {
            return false;
        }
        setBlogOptions(blogOptions);
        return true;
    }

    public boolean isActive() {
        return org.apache.commons.lang.StringUtils.length(password) > 0;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean bsetAdmin(boolean isAdmin) {
        if (this.isAdmin == isAdmin) {
            return false;
        }
        setAdmin(isAdmin);
        return true;
    }

    public String getAdminUrl() {
        String adminUrl = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {
        }.getType();
        Map<?, ?> blogOptions = gson.fromJson(this.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("admin_url");
            if (homeURLMap != null)
                adminUrl = homeURLMap.get("value").toString();
        }
        // Try to guess the URL of the dashboard if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (adminUrl == null) {
            if (this.getUrl().lastIndexOf("/") != -1) {
                adminUrl = this.getUrl().substring(0, this.getUrl().lastIndexOf("/"))
                        + "/wp-admin";
            } else {
                adminUrl = this.getUrl().replace("xmlrpc.php", "wp-admin");
            }
        }
        return adminUrl;
    }

    public boolean isPrivate() {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> blogOptions = gson.fromJson(getBlogOptions(), type);
            StringMap<?> blogPublicOption = (StringMap<?>) blogOptions.get("blog_public");
            String blogPublicOptionValue = blogPublicOption.get("value").toString();
            if (blogPublicOptionValue.equals("-1")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isJetpackPowered() {
        String optionsString = getBlogOptions();
        if (optionsString == null)
            return false;

        try {
            JSONObject options = new JSONObject(optionsString);
            if (options.has("jetpack_client_id"))
                return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isPhotonCapable() {
        return ((isDotcomFlag() && !isPrivate()) || isJetpackPowered());
    }

    public boolean hasValidJetpackCredentials() {
        return !TextUtils.isEmpty(getDotcom_username()) && !TextUtils.isEmpty(getDotcom_password());
    }

    /**
     * Get the WordPress.com blog ID
     * Stored in blogId for WP.com, api_blogId for Jetpack
     *
     * @return WP.com blogId string, potentially null for Jetpack sites
     */
    public String getDotComBlogId() {
        if (isDotcomFlag())
            return String.valueOf(getRemoteBlogId());
        else
            return getApi_blogid();
    }
}
