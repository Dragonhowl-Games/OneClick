package games.dragonhowl.oneclick.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import games.dragonhowl.oneclick.OneClick;
import games.dragonhowl.oneclick.utils.APIUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Fabric {

    private static final APIUtils.APIHandler handler = new APIUtils.APIHandler("https://meta.fabricmc.net/v2/versions");
    private static String fabricLoaderVersion; //Store so we don't need to ask the api every time

    public static class Version {
        @SerializedName("version")
        public String version;
        @SerializedName("stable")
        public boolean stable;
    }

    //Will only ask api first time called, return fabricLoadVersion var every next time - save on unneeded api calls
    public static String getLatestLoaderVersion() throws IOException {
        if (fabricLoaderVersion != null) return fabricLoaderVersion;

        Version[] versions = handler.get("/loader", Version[].class);
        if (versions != null) {
            for (Version version : versions) {
                if (version.stable) {
                    fabricLoaderVersion = version.version;
                    return version.version;
                }
            }
        }

        fabricLoaderVersion = "0.14.22"; //Known latest as backup
        return fabricLoaderVersion;
    }

    //Won't do anything if version is already installed
    public static JsonObject downloadJson(String gameDir, String gameVersion, String loaderVersion) throws IOException {
        String profileName = String.format("%s-%s-%s", "fabric-loader", loaderVersion, gameVersion);
        File versionFile = new File(gameDir + profileName + "/" + profileName + ".json");

        String json = APIUtils.getRaw(String.format(handler.getBaseUrl() + "/loader/%s/%s/profile/json", gameVersion, loaderVersion));
        if (json == null) return null;

        versionFile.getParentFile().mkdirs();
        if (!versionFile.createNewFile()) return OneClick.GSON.fromJson(json, JsonObject.class);

        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(versionFile));
        fos.write(json.getBytes(), 0, json.length());
        fos.close();

        return OneClick.GSON.fromJson(json, JsonObject.class);
    }

    public static void installProfile(String gameDir, String name, String icon, JsonObject versionJson) throws IOException {
        JsonObject profile = new JsonObject();
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());

        profile.addProperty("name", name);
        profile.addProperty("type", "custom");
        profile.addProperty("created", time);
        profile.addProperty("lastUsed", time);
        profile.addProperty("icon", icon);
        profile.addProperty("javaArgs", "-Xmx4G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
        profile.addProperty("lastVersionId", versionJson.get("id").getAsString());

        File profileFile = new File(gameDir + "/launcher_profiles.json");

        JsonReader reader = new JsonReader(new FileReader(profileFile));
        JsonObject profileJson = OneClick.GSON.fromJson(reader, JsonObject.class);
        reader.close();

        profileJson.getAsJsonObject("profiles").add(name, profile);
        JsonWriter writer = new JsonWriter(new FileWriter(profileFile));
        OneClick.GSON.toJson(profileJson, writer);
        writer.flush();
        writer.close();

        System.out.println("Installed Mod Profile");
    }
}