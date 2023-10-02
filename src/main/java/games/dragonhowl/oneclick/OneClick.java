package games.dragonhowl.oneclick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import games.dragonhowl.oneclick.minecraft.Fabric;
import games.dragonhowl.oneclick.minecraft.Modrinth;
import games.dragonhowl.oneclick.utils.ImageEncoder;
import io.github.gaming32.mrpacklib.Mrpack;
import io.github.gaming32.mrpacklib.packindex.PackFile;
import io.github.gaming32.mrpacklib.packindex.PackIndex;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OneClick extends Application {

    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Stage stage;

    @FXML
    Button fileChooserButton;
    @FXML
    Button installButton;
    @FXML
    Label progressText;
    @FXML
    ProgressBar progressBar;

    private String installPath = null;

    @Override
    public void start(Stage stage) throws Exception {
        switch (System.getProperty("os.name")) {
            /*case "Linux" -> {
                String home = System.getProperty("user.home");
                if (home == null) return;

                File gameDir = new File(home + "/.var/app/com.mojang.Minecraft/.minecraft");
                System.out.println(gameDir);
                installPath = gameDir.getAbsolutePath();
                System.out.println(installPath);
            }*/

            case "Windows" -> {
                String appdata = System.getenv("APPDATA");
                if (appdata == null) return;

                File gameDir = new File(appdata + "/.minecraft");
                if (!gameDir.exists()) return;

                installPath = gameDir.getAbsolutePath();
            }
        }

        FXMLLoader fxmlLoader = new FXMLLoader(OneClick.class.getResource("oneclick.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 650);
        scene.getStylesheets().addAll(this.getClass().getResource("style.css").toExternalForm());

        stage.setTitle("OneClick");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
    }

    @FXML
    protected void onFileChooserButtonClick() {
        DirectoryChooser pathPicker = new DirectoryChooser();
        pathPicker.setTitle("Select Install Location");
        if (installPath != null) pathPicker.setInitialDirectory(new File(installPath));

        File pickedPath = pathPicker.showDialog(stage);
        if (pickedPath == null) return;

        installPath = pickedPath.getAbsolutePath();
        progressText.setText("Install Location: " + installPath);
    }

    @FXML
    protected void onInstallButtonClick() {
        installButton.setVisible(false);
        progressBar.setVisible(true);

        installModpack(installPath);
    }

    public static void main(String[] args) {
        launch();
    }

    public void installModpack(String gameDir) {
        Task task = new Task() {

            public void log(String message) {
                updateMessage(message);
                System.out.println(message);
            }

            @Override
            protected Object call() throws Exception {
                updateProgress(0, 1);

                log("Getting Latest Modpack Version");
                Modrinth.VersionData latestVersion = Modrinth.getLatestVersion();

                if (latestVersion == null) {
                    // Error with modrinth, show message

                    System.exit(1);
                }

                log("Installing Fabric: " + latestVersion.gameVersions.get(0));
                JsonObject versionJson = Fabric.downloadJson(gameDir, latestVersion.gameVersions.get(0), Fabric.getLatestLoaderVersion());
                if (versionJson == null) {
                    // Fabric meta must be down

                    System.exit(1);
                }
                log("Installed Fabric");

                log("Downloading Modpack");
                ZipFile zip = Modrinth.downloadModPack(gameDir, latestVersion.fileData.filename, latestVersion.fileData.url);
                log("Downloaded Modpack");

                Mrpack mrpack = new Mrpack(zip);
                PackIndex packIndex = mrpack.getPackIndex();

                try {
                    packIndex.validate();
                } catch (IllegalArgumentException e) {
                    //Invalid Modpack

                    System.exit(1);
                }


                int max = packIndex.getFiles().size();
                for (int i = 0; i < max; i++) {
                    PackFile mod = packIndex.getFiles().get(i);

                    try {
                        mod.validate();
                    } catch (IllegalArgumentException e) {
                        //Invalid Mod in Modpack

                        System.exit(1);
                    }

                    if (mod.getEnv().getClient().equals(Mrpack.EnvCompatibility.UNSUPPORTED)) continue;

                    for (URL url : mod.getDownloads()) {
                        File modFile = new File(gameDir + "/" + mod.getPath());
                        modFile.mkdirs();

                        log("Downloading: " + modFile.getName());
                        Files.copy(url.openStream(), modFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    updateProgress(i, max);
                }

                for (ZipEntry zipEntry : mrpack.getGlobalOverrides()) {
                    if (zipEntry.isDirectory()) continue;

                    String filename = zipEntry.getName().replace("overrides", "");
                    log("Unpacking: " + filename);

                    File file = new File(gameDir + filename);
                    file.getParentFile().mkdirs();

                    Files.copy(zip.getInputStream(zipEntry), Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                }

                String icon = "Crafting_Table";
                for (ZipEntry file : mrpack.getGlobalOverrides()) {
                    if (file.getName().contains("icon.png")) {
                        icon = ImageEncoder.encodeImage(zip.getInputStream(file));
                        break;
                    }
                }

                Fabric.installProfile(gameDir, packIndex.getName(), icon, versionJson);

                zip.close();
                mrpack.close();

                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressText.textProperty().bind(task.messageProperty());
        new Thread(task).start();
    }
}