package games.dragonhowl.oneclick.minecraft;

import com.google.gson.annotations.SerializedName;
import games.dragonhowl.oneclick.Config;
import games.dragonhowl.oneclick.utils.APIUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipFile;

public class Modrinth {

    private static final APIUtils.APIHandler handler = new APIUtils.APIHandler("https://api.modrinth.com/v2");

    public static class Project {
        @SerializedName("title")
        public String title;
        @SerializedName("slug")
        public String slug;
        @SerializedName("icon_url")
        public String iconUrl;
        @SerializedName("body")
        public String body;
    }

    public static class Version {
        @SerializedName("id")
        public String id;
        @SerializedName("loaders")
        public List<String> loaders;
        @SerializedName("game_versions")
        public List<String> gameVersions;
        @SerializedName("files")
        public List<File> files;

        public static class File {
            @SerializedName("url")
            public String url;
            @SerializedName("filename")
            public String filename;
        }
    }

    public static class VersionData {
        @SerializedName("title")
        public String title;
        @SerializedName("slug")
        public String slug;
        @SerializedName("icon_url")
        public String iconUrl;

        public FileData fileData;
        public List<String> gameVersions;

        public VersionData() {
            fileData = new FileData();
        }

        public static class FileData {
            public String id;
            public String url;
            public String filename;
        }
    }

    public static VersionData getLatestVersion() throws IOException {
        Project project = handler.get("project/" + Config.MODPACK_SLUG, Project.class);
        Version[] versions = handler.get("project/" + Config.MODPACK_SLUG + "/version", Version[].class);

        if (project == null || versions == null) return null;

        Version version = versions[0];
        Version.File file = version.files.get(0);
        VersionData versionData = new VersionData();
        versionData.title = project.title;
        versionData.slug = project.slug;
        versionData.iconUrl = project.iconUrl;
        versionData.gameVersions = version.gameVersions;
        versionData.fileData.id = version.id;
        versionData.fileData.url = file.url;
        versionData.fileData.filename = file.filename;
        return versionData;
    }

    public static ZipFile downloadModPack(String gameDir, String filename, String url) throws IOException {
        Files.copy(new URL(url).openStream(), Paths.get(gameDir + "/" + filename), StandardCopyOption.REPLACE_EXISTING);
        return new ZipFile(gameDir + "/" + filename);
    }
}